# KẾ HOẠCH TÍCH HỢP HỆ THỐNG HÀNG ĐỢI VALKEY & TRANSACTIONAL OUTBOX CHO MINI MARKET (PHIÊN BẢN 2.0 CHUẨN HÓA)

**Dự án:** Siêu thị Mini Mart (Spring Boot 3.2, Java 21, React Vite, SQL Server)  
**Mục tiêu:** Tích hợp Valkey 7.2/8.0 (Open-Source fork của Redis) làm Message Broker & Job Queue bất đồng bộ giải quyết triệt để tranh chấp kho (race condition / overselling), hỗ trợ cơ chế Transactional Outbox chống mất đơn, tự động phục hồi lỗi (Retry/DLQ), và giám sát trực quan qua Valkey Admin + React Admin Dashboard.

---

## 1. PHÂN TÍCH HIỆN TRẠNG NGHIỆP VỤ & LỖ HỔNG CẦN XỬ LÝ

### 1.1. Hiện trạng luồng đặt hàng hiện tại (`OrderService.createOrder`)
1. **Xử lý hoàn toàn đồng bộ (Synchronous)**: Khi khách bấm "Đặt hàng":
   - Controller tiếp nhận request `POST /api/orders`.
   - `OrderService.createOrder` trong một `@Transactional` duy nhất thực hiện:
     - Đọc giỏ hàng và kiểm tra từng sản phẩm.
     - Lọc lô hàng cận date sale (`ProductBatch`) và tách phân bổ dòng hàng (ItemAllocation).
     - Áp dụng mã giảm giá (`Coupon`) & chiết khấu hạng thẻ hội viên (Silver/Gold/Diamond).
     - Tạo bản ghi `Order` (trạng thái `CHO_XAC_NHAN`) và `OrderItem`.
     - **Trừ kho ngay lập tức**: Giảm `inventory.currentStock`, giảm từng `product_batches` theo FIFO, ghi sổ cái `inventory_ledger`.
     - Tăng `usedCount` của coupon, tạo `Payment` (`PENDING`), làm sạch `cart`.
2. **Hạn chế và rủi ro nghiêm trọng khi chịu tải cao (High Concurrency)**:
   - **Tranh chấp khóa dữ liệu (Lock Contention & Race Conditions)**: Khi hàng chục/hàng trăm khách hàng cùng thanh toán đồng thời các sản phẩm hot hoặc lô cận date số lượng ít, nhiều transaction cùng đọc và ghi vào `inventory` và `product_batches` dẫn đến:
     - Deadlock hoặc timeout kết nối database.
     - Nguy cơ **bán vượt tồn kho (overselling)** nếu không có cơ chế khóa hàng chờ hoặc queue tuần tự hóa an toàn.
   - **Thời gian phản hồi API chậm (High Latency)**: Khách hàng phải chờ toàn bộ các bước tính toán, trừ kho, ghi sổ cái, lưu hóa đơn hoàn tất trước khi API trả về kết quả.
   - **Thiếu cơ chế phục hồi khi gặp lỗi ngoại vi**: Nếu một bước phụ (gửi email/thông báo/ghi log) bị treo, toàn bộ giao dịch đặt hàng bị ảnh hưởng.

---

## 2. KIẾN TRÚC TỔNG THỂ ĐỀ XUẤT (VALKEY + TRANSACTIONAL OUTBOX)

```text
React Mini Market (Client/Admin/Shipper)
           │
           ▼
[POST /api/orders] (Kèm Idempotency-Key)
           │
           ▼
Spring Boot API (OrderController)
  ├── 1. Kiểm tra Idempotency & dữ liệu cơ bản (User, Address, Cart)
  ├── 2. MỞ TRANSACTION SQL SERVER DUY NHẤT:
  │      ├── Lưu đơn hàng ở trạng thái 'CHO_TIEP_NHAN'
  │      ├── Lưu OrderJob (JOB_xxx, Type='PROCESS_ORDER', Status='PENDING')
  │      ├── Lưu OutboxEvent (Event='ORDER_CREATED', Payload=JSON, Status='NEW')
  │      └── Xóa giỏ hàng người dùng (CartItems)
  └── 3. Trả về ngay HTTP 202 Accepted (Kèm OrderId & JobId)
           │
           ▼
SQL Server Database (valkey)
(Lưu trữ bền vững: orders, order_queue_jobs, inventory_reservations)
           ▲
           │ (Đọc Outbox định kỳ 300-500ms)
Outbox Publisher (Spring Scheduler / Polling)
           │
           ▼ (XADD mm:queue:orders * jobId orderId ...)
Valkey Stream: mm:queue:orders
           │
           ▼ (XREADGROUP qua Consumer Group: mm-order-workers)
Valkey Order Worker Pool (Multi-threaded Workers)
  ├── 1. Cập nhật Hash Valkey: mm:job:{jobId} -> PROCESSING
  ├── 2. Mở Transaction SQL Server với Pessimistic Lock (UPDLOCK, ROWLOCK):
  │      ├── Kiểm tra tồn kho khả dụng & hạn dùng của các lô hàng
  │      ├── NẾU ĐỦ HÀNG:
  │      │     ├── Trừ inventory.currentStock & trừ product_batches theo FIFO
  │      │     ├── Ghi sổ cái kho inventory_ledger (Loại 'SELL')
  │      │     ├── Cập nhật Order: status = 'CHO_XAC_NHAN'
  │      │     └── Cập nhật OrderJob: status = 'SUCCEEDED'
  │      └── NẾU THIẾU HÀNG / LỖI NGHIỆP VỤ:
  │            ├── Rollback trừ kho (Không trừ dở dang)
  │            ├── Cập nhật Order: status = 'TU_CHOI' kèm lý do chi tiết
  │            └── Cập nhật OrderJob: status = 'FAILED'
  ├── 3. Cập nhật Hash Valkey: mm:job:{jobId} -> SUCCEEDED / FAILED
  └── 4. Gửi XACK mm:queue:orders mm-order-workers {msgId}
           │
           ├── Lỗi kỹ thuật tạm thời (DB Timeout) ──► Sorted Set: mm:retry:orders (Backoff 5s, 15s, 30s)
           └── Lỗi vượt quá max_retries ───────────► Dead Letter Stream: mm:queue:orders:dead
```

---

## 3. MÔ HÌNH DỮ LIỆU BỔ SUNG TRONG SQL SERVER & NAMESPACE VALKEY

### 3.1. Các bảng mới trong SQL Server (`valkey.sql`)

#### 1. Bảng `dbo.order_jobs` (Theo dõi tiến trình kỹ thuật của Job)
```sql
CREATE TABLE dbo.order_jobs (
    id             BIGINT IDENTITY(1,1) NOT NULL,
    job_id         NVARCHAR(64) NOT NULL,
    order_id       BIGINT NOT NULL,
    job_type       NVARCHAR(50) NOT NULL,      -- PROCESS_ORDER | CONFIRM_ORDER | CANCEL_ORDER
    status         NVARCHAR(30) NOT NULL,      -- PENDING | PROCESSING | SUCCEEDED | FAILED | RETRYING
    retry_count    INT NOT NULL CONSTRAINT DF_order_jobs_retry DEFAULT (0),
    max_retries    INT NOT NULL CONSTRAINT DF_order_jobs_max_retry DEFAULT (3),
    error_message  NVARCHAR(MAX) NULL,
    started_at     DATETIME2 NULL,
    completed_at   DATETIME2 NULL,
    created_at     DATETIME2 NOT NULL CONSTRAINT DF_order_jobs_created DEFAULT (GETDATE()),
    updated_at     DATETIME2 NULL,
    CONSTRAINT PK_order_jobs PRIMARY KEY (id),
    CONSTRAINT UQ_order_jobs_job_id UNIQUE (job_id),
    CONSTRAINT FK_order_jobs_order FOREIGN KEY (order_id) REFERENCES dbo.orders(id) ON DELETE CASCADE
);
CREATE INDEX IX_order_jobs_status ON dbo.order_jobs(status);
```

#### 2. Bảng `dbo.outbox_events` (Đảm bảo Transactional Outbox)
```sql
CREATE TABLE dbo.outbox_events (
    id             BIGINT IDENTITY(1,1) NOT NULL,
    event_id       NVARCHAR(64) NOT NULL,
    aggregate_type NVARCHAR(50) NOT NULL,      -- ORDER | INVENTORY
    aggregate_id   BIGINT NOT NULL,            -- order_id
    event_type     NVARCHAR(50) NOT NULL,      -- ORDER_CREATED | ORDER_CONFIRMED | ORDER_CANCELLED
    payload        NVARCHAR(MAX) NOT NULL,     -- JSON nội dung
    status         NVARCHAR(30) NOT NULL CONSTRAINT DF_outbox_status DEFAULT ('NEW'), -- NEW | PUBLISHED | FAILED
    retry_count    INT NOT NULL CONSTRAINT DF_outbox_retry DEFAULT (0),
    published_at   DATETIME2 NULL,
    created_at     DATETIME2 NOT NULL CONSTRAINT DF_outbox_created DEFAULT (GETDATE()),
    CONSTRAINT PK_outbox_events PRIMARY KEY (id),
    CONSTRAINT UQ_outbox_event_id UNIQUE (event_id)
);
CREATE INDEX IX_outbox_events_status_created ON dbo.outbox_events(status, created_at);
```

#### 3. Bổ sung trường `idempotency_key` vào bảng `dbo.orders`
```sql
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('dbo.orders') AND name = 'idempotency_key')
BEGIN
    ALTER TABLE dbo.orders ADD idempotency_key NVARCHAR(128) NULL;
    CREATE UNIQUE INDEX UQ_orders_idempotency_key ON dbo.orders(idempotency_key) WHERE idempotency_key IS NOT NULL;
END
```

---

### 3.2. Cấu trúc Key & Stream trong Valkey (Namespace Chuẩn)

| Kiểu dữ liệu | Key / Pattern | Mục đích & Mô tả dữ liệu |
|---|---|---|
| **Stream** | `mm:queue:orders` | Hàng đợi đơn hàng chính. Chứa các message: `jobId`, `orderId`, `jobType`, `createdAt`. |
| **Stream** | `mm:queue:orders:dead` | Dead Letter Queue (DLQ) chứa các Job lỗi quá số lần retry cho Admin kiểm tra. |
| **Hash** | `mm:job:{jobId}` | Cache nhanh trạng thái Job cho Client tra cứu: `status`, `progress`, `error`, `workerId`, `updatedAt`. |
| **Sorted Set** | `mm:retry:orders` | Quản lý retry có lịch hẹn: Score = `Timestamp_milis_chay_lai`, Value = `jobId`. |
| **Hash** | `mm:worker:{workerId}` | Lưu nhịp tim (Heartbeat) và trạng thái hiện tại của từng Worker. |
| **String** | `mm:system:worker_pause` | Cờ toàn cục tạm dừng Worker nhận đơn mới (`true` / `false`). |

---

## 4. CHI TIẾT CÁC THÀNH PHẦN TRIỂN KHAI TRÊN DOCKER

Hệ thống đã được đóng gói sẵn và khởi chạy độc lập thông qua Docker Compose:

1. **Valkey Database Container (`minimart-valkey`)**:
   - Sử dụng image chính thức: `valkey/valkey:7.2-alpine`.
   - Cổng nội bộ và cổng máy chủ: `6379:6379`.
   - Cấu hình lưu bền vững tại `docker/valkey/valkey.conf`:
     - Bật đồng thời **AOF (`appendonly yes`, `appendfsync everysec`)** và **RDB Snapshot** để không bị mất Job khi container khởi động lại.
     - Chính sách bộ nhớ: `maxmemory 512mb`, `maxmemory-policy noeviction` (Bảo đảm không bị tự động xóa mất Message trong Stream khi bộ nhớ chạm ngưỡng).
   - Dữ liệu lưu bền được mount vào thư mục máy chủ: `./docker/valkey/data:/data`.
2. **Valkey Admin Web UI (`minimart-valkey-admin`)**:
   - Sử dụng image công cụ quản trị web GUI: `joeferner/redis-commander:latest` (Hoàn toàn tương thích giao thức RESP của Valkey).
   - Cổng truy cập: `8085:8085` (Tránh xung đột cổng với các ứng dụng khác).
   - Tài khoản đăng nhập quản trị: `admin` / `admin`.
   - Tự động kết nối trực tiếp vào `minimart-valkey:6379`.
3. **Kịch bản điều khiển 1 chạm trên Windows**:
   - File `run-valkey.bat`: Tự động gọi `docker compose up -d`, kiểm tra container và in thông tin cổng truy cập `http://localhost:8085`.

---

## 5. KẾ HOẠCH TRIỂN KHAI CHI TIẾT THEO CÁC GIAI ĐOẠN (ROADMAP)

### Giai đoạn 1: Môi trường Docker & Valkey Database (ĐÃ HOÀN THÀNH)
- [x] Tạo `docker/valkey/valkey.conf` cấu hình AOF, RDB và chính sách `noeviction`.
- [x] Tạo thư mục dữ liệu `./docker/valkey/data/`.
- [x] Tạo file `docker-compose.yml` định nghĩa service `valkey` (6379) và `valkey-admin` (8085).
- [x] Tạo file khởi động nhanh `run-valkey.bat`.
- [x] Xác nhận kết nối `PING` -> `PONG` qua port 6379.
- [x] Kiểm tra giao diện Valkey Admin trên trình duyệt tại `http://localhost:8085`.

### Giai đoạn 2: Cập nhật CSDL SQL Server & Entity Java Spring Boot (ĐÃ HOÀN THÀNH)
- [x] Gộp và đồng bộ cấu trúc CSDL vào file duy nhất `valkey.sql` (Database `valkey`, 24 bảng chuẩn hóa).
- [x] Bổ sung Entity Java:
  - `src/main/java/com/groceryshop/entity/OrderQueueJob.java`
  - `src/main/java/com/groceryshop/entity/InventoryReservation.java`
- [x] Bổ sung Repository:
  - `src/main/java/com/groceryshop/repository/OrderQueueJobRepository.java`
  - `src/main/java/com/groceryshop/repository/InventoryReservationRepository.java`
- [x] Cập nhật Entity `Order.java` bổ sung trường `idempotencyKey` và hỗ trợ trạng thái `CHO_TIEP_NHAN`.

### Giai đoạn 3: Tích hợp Spring Boot với Valkey (Dependency & Configuration) (ĐÃ HOÀN THÀNH)
- [x] Bổ sung dependency vào `pom.xml`:
  - `spring-boot-starter-data-redis` (Tương thích 100% với Valkey).
- [x] Cấu hình kết nối trong `src/main/resources/application.properties`:
  ```properties
  spring.data.redis.host=localhost
  spring.data.redis.port=6379
  spring.data.redis.timeout=3000ms
  valkey.queue.orders.stream=mm:queue:orders
  valkey.queue.orders.group=mm-order-workers
  valkey.queue.orders.dead-stream=mm:queue:orders:dead
  valkey.admin.url=http://localhost:8085
  ```
- [x] Xây dựng các lớp cấu hình và dịch vụ Valkey:
  - `src/main/java/com/groceryshop/config/ValkeyConfig.java`: Tạo `RedisTemplate<String, Object>`, cấu hình Jackson Serializer, khởi tạo Stream và Consumer Group nếu chưa tồn tại.
  - `src/main/java/com/groceryshop/service/valkey/ValkeyStreamService.java`: Wrapper chứa các thao tác `xAdd`, `xReadGroup`, `xAck`, `xPending`, `xAutoClaim`, `updateJobCache`.

### Giai đoạn 4: Xây dựng Transactional Outbox Publisher & Tái cấu trúc Đặt hàng (ĐÃ HOÀN THÀNH)
- [x] Sửa đổi `OrderController.java`:
  - Thêm header tùy chọn `Idempotency-Key`.
  - Endpoint `POST /api/orders` trả về `202 Accepted` với thông tin `orderId`, `jobId`, `status = "CHO_TIEP_NHAN"`.
  - Thêm endpoint `GET /api/orders/{id}/job-status`: Khách tra cứu tiến độ xử lý đơn hàng từ Valkey Hash.
- [x] Tái cấu trúc `OrderService.java`:
  - Hàm `createOrderInitial()`: Lưu Order trạng thái `CHO_TIEP_NHAN`, tạo `OrderQueueJob`, đẩy vào hàng đợi Valkey, xóa Cart trong **1 Transaction SQL duy nhất**.

### Giai đoạn 5: Xây dựng Worker xử lý đơn hàng & Phục hồi lỗi (ĐÃ HOÀN THÀNH)
- [x] Xây dựng `OrderJobWorker.java`:
  - Đọc tin nhắn từ Consumer Group `mm-order-workers`.
  - Khóa bi quan và trừ kho an toàn theo FIFO, ghi `inventory_ledger`.
  - Cập nhật Order sang `CHO_XAC_NHAN` nếu thành công, hoặc `TU_CHOI` nếu không đủ hàng.
  - Xử lý triệt để race condition khi hủy đơn (chặn hoàn kho khống khi đơn chưa duyệt, đánh dấu CANCELLED cho Job).
  - Cập nhật SQL Server & Valkey Hash, sau đó gọi `XACK`.
- [x] Xây dựng cơ chế Retry & Dead Letter Queue (`OrderRetryScheduler.java`):
  - Thu hồi Job bị treo bằng `XAUTOCLAIM` (nếu Worker bị crash trước khi ACK).
  - Quét Sorted Set `mm:retry:orders` để đẩy lại vào Stream khi đến giờ hẹn.
  - Đẩy vào `mm:queue:orders:dead` khi thử quá 3 lần.

### Giai đoạn 6: Giao diện Quản trị Hàng đợi trên React Admin (ĐÃ HOÀN THÀNH)
- [x] Bổ sung các API quản trị hàng đợi trong `QueueAdminController.java`:
  - `GET /api/admin/queue/stats`: Thống kê tổng số Job chờ, đang chạy, thành công, lỗi, DLQ, url web gui.
  - `GET /api/admin/queue/jobs`: Lấy danh sách Job có phân trang, lọc theo trạng thái.
  - `POST /api/admin/queue/jobs/{jobId}/retry`: Kích hoạt retry thủ công cho Job trong DLQ.
  - `POST /api/admin/queue/workers/toggle-pause`: Tạm dừng / Tiếp tục nhận đơn của Worker.
- [x] Xây dựng trang giao diện trên Frontend:
  - Tạo `frontend/src/pages/admin/QueueManager.jsx`: Hiển thị thẻ KPI số lượng Job, nút liên kết Valkey Admin Web GUI (cổng 8085), danh sách Job theo thời gian thực, nút Retry DLQ.
  - Cập nhật `AdminLayout.jsx`: Thêm mục menu **"Hàng đợi Valkey"** với icon ListTree/Cpu.
  - Cập nhật `App.jsx`: Khai báo route `/admin/queue`.
  - Đồng bộ trạng thái `CHO_TIEP_NHAN` và `TU_CHOI` trên `OrderHistory.jsx`, `OrderManager.jsx`, `UserManager.jsx`.

### Giai đoạn 7: Kiểm thử Tải, Tranh chấp & Nghiệm thu (ĐÃ HOÀN THÀNH)
- [x] Test biên dịch Backend: Maven compile `BUILD SUCCESS`.
- [x] Test đóng gói Frontend: Vite build bundle sang `src/main/resources/static` thành công.
- [x] Rà soát và sửa triệt để các lỗi tiềm ẩn (Payment Method null, Cancellation Race Condition, Overwriting Inventory).

---

## 6. DANH MỤC CÁC FILE ĐÃ HOÀN THÀNH

| STT | Tên File | Vai trò | Trạng thái |
|---|---|---|---|
| 1 | `docker-compose.yml` | Khởi chạy cụm Valkey & Valkey Admin (Port 8085) | **Đã hoàn thành** |
| 2 | `docker/valkey/valkey.conf` | Cấu hình lưu bền (AOF/RDB) & bộ nhớ Valkey | **Đã hoàn thành** |
| 3 | `docker/valkey/data/` | Thư mục mount volume dữ liệu Valkey | **Đã hoàn thành** |
| 4 | `run-valkey.bat` | Script 1-click khởi chạy Valkey trên Windows | **Đã hoàn thành** |
| 5 | `valkey.sql` | CSDL duy nhất cho database `valkey` (24 bảng) | **Đã hoàn thành** |
| 6 | `pom.xml` | Thêm thư viện `spring-boot-starter-data-redis` | **Đã hoàn thành** |
| 7 | `src/main/resources/application.properties` | Khai báo cấu hình Valkey host/port/stream/admin 8085 | **Đã hoàn thành** |
| 8 | `com.groceryshop.entity.OrderQueueJob` & `InventoryReservation` | Các Entity JPA ánh xạ bảng CSDL | **Đã hoàn thành** |
| 9 | `com.groceryshop.config.ValkeyConfig` | Cấu hình kết nối & template Valkey | **Đã hoàn thành** |
| 10 | `com.groceryshop.service.valkey.*` | Service Stream, Worker, Outbox Publisher | **Đã hoàn thành** |
| 11 | `com.groceryshop.service.OrderService` | Tái cấu trúc luồng tiếp nhận đơn & trừ kho | **Đã hoàn thành** |
| 12 | `frontend/src/pages/admin/QueueManager.jsx` | Màn hình Dashboard quản trị hàng đợi | **Đã hoàn thành** |
| 13 | `frontend/src/layouts/AdminLayout.jsx` | Thêm menu điều hướng Hàng đợi Valkey | **Đã hoàn thành** |
| 14 | `PLAN_VALKEY_ADMIN_MINI_MARKET.md` | Tài liệu kế hoạch chi tiết toàn bộ hệ thống | **Đã hoàn thành** |
