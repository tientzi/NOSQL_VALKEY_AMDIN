# 🛒 DỰ ÁN WEBSITE QUẢN LÝ SIÊU THỊ MINI & CHUỖI BÁN LẺ THỰC PHẨM TƯƠI SẠCH (MINIMART)

Chào mừng bạn đến với dự án **Hệ Thống Quản Lý & Bán Hàng Siêu Thị Mini Trực Tuyến (MiniMart Fresh & Convenience)**. Đây là giải pháp phần mềm thương mại điện tử và quản trị chuỗi bán lẻ toàn diện, được xây dựng theo kiến trúc hiện đại **Fullstack Decoupled Architecture** kết hợp giữa nền tảng **Java Spring Boot 3.2 (RESTful API)**, cơ sở dữ liệu **Microsoft SQL Server**, giao diện người dùng **React 18 SPA (Vite)**, thuật toán khai phá dữ liệu **Apriori (Market Basket Analysis)** và trợ lý ảo thông minh **Google Gemini AI**.

Hệ thống được thiết kế chuyên biệt cho các chuỗi siêu thị mini, cửa hàng tiện lợi, tạp hóa thông minh và bán lẻ thực phẩm tươi sống; khép kín toàn bộ chu trình nghiệp vụ bán lẻ thực tế: từ tìm kiếm gợi ý thông minh, đặt hàng đa lô hạn sử dụng, tích điểm thăng hạng hội viên VIP, điều phối shipper tự động theo quận huyện, quy trình **Kiểm định chất lượng đầu vào (Inward Quality Control - QC)**, quản lý tiêu hủy hàng quá hạn & báo cáo vốn thiệt hại, cho đến xuất hóa đơn bán lẻ chuẩn thu ngân.

> [!NOTE]
> Hệ thống sở hữu tài liệu quy trình nghiệp vụ chuyên sâu chuẩn BPM với đầy đủ sơ đồ Mermaid chi tiết tại: [QUY_TRINH_NGHIEP_VU_CHI_TIET.md](file:///d:/NAM%20CUOI/CNJAVA/DO_AN_CONGNGHEJAVA/QUY_TRINH_NGHIEP_VU_CHI_TIET.md).  
> Kế hoạch kỹ thuật và kiến trúc nâng cấp kho đa lô 2.0 tham khảo tại: [plan2.md](file:///d:/NAM%20CUOI/CNJAVA/DO_AN_CONGNGHEJAVA/plan2.md) và [PLAN.md](file:///d:/NAM%20CUOI/CNJAVA/DO_AN_CONGNGHEJAVA/PLAN.md).

---

## 📑 MỤC LỤC

1. [🛠️ Công Nghệ Sử Dụng](#️-công-nghệ-sử-dụng)
2. [🏗️ Kiến Trúc Ứng Dụng & Mô Hình Spring Boot MVC](#️-kiến-trúc-ứng-dụng--mô-hình-spring-boot-mvc)
3. [✨ Tính Năng Nổi Bật & Các Điểm Mới Nâng Cấp](#-tính-năng-nổi-bật--các-điểm-mới-nâng-cấp)
4. [👥 Chi Tiết Các Phân Hệ Chức Năng](#-chi-tiết-các-phân-hệ-chức-năng)
   - [1. Phân Hệ Khách Hàng (User / Client Portal)](#1-phân-hệ-khách-hàng-user--client-portal)
   - [2. Phân Hệ Quản Trị Viên (Admin Console)](#2-phân-hệ-quản-trị-viên-admin-console)
   - [3. Phân Hệ Nhân Viên Giao Hàng (Shipper Console)](#3-phân-hệ-nhân-viên-giao-hàng-shipper-console)
5. [🗄️ Cấu Trúc Cơ Sở Dữ Liệu (Database Schema)](#️-cấu-trúc-cơ-sở-dữ-liệu-database-schema)
6. [📂 Cấu Trúc Thư Mục Dự Án](#-cấu-trúc-thư-mục-dự-án)
7. [🔑 Tài Khoản Trải Nghiệm Mặc Định](#-tài-khoản-trải-nghiệm-mặc-định)
8. [🚀 Hướng Dẫn Cài Đặt & Khởi Chạy](#-hướng-dẫn-cài-đặt--khởi-chạy)
9. [📡 Danh Sách RESTful API Chính](#-danh-sách-restful-api-chính)
10. [⚙️ Cấu Hình Môi Trường & Lưu Ý Quan Trọng](#️-cấu-hình-môi-trường--lưu-ý-quan-trọng)

---

## 🛠️ Công Nghệ Sử Dụng

### 1. Backend (RESTful Web Services)
* **Ngôn ngữ**: Java 21 (LTS).
* **Framework**: Spring Boot 3.2.0 (Spring MVC, Spring Data JPA, Spring Security 6).
* **Bảo mật**: Stateless JWT Authentication (`jjwt 0.11.5`), Remember-me Token, BCrypt Password Encoder.
* **Đăng nhập mạng xã hội**: Spring Boot Starter OAuth2 Client (Đăng nhập 1 chạm với Google & Facebook).
* **Tương tác dữ liệu**: Spring Data JPA, Hibernate ORM (Tự động ánh xạ chuỗi Unicode `NVARCHAR` qua thuộc tính `use_nationalized_character_data`).
* **Tiện ích & Mapper**: Project Lombok (`1.18.34`), Jakarta Validation, RestTemplate.
* **Build Tool**: Apache Maven (Script [mvnw.cmd](file:///d:/NAM%20CUOI/CNJAVA/DO_AN_CONGNGHEJAVA/mvnw.cmd) đi kèm).

### 2. Thuật Toán Khai Phá Dữ Liệu & Trí Tuệ Nhân Tạo (Data Mining & AI)
* **Thuật toán Apriori (Market Basket Analysis)**: Được lập trình trực tiếp trong service [RecommendationService.java](file:///d:/NAM%20CUOI/CNJAVA/DO_AN_CONGNGHEJAVA/src/main/java/com/groceryshop/service/RecommendationService.java), khai phá các tập mục mua cùng từ lịch sử 50 đơn hàng thực tế; tính toán chính xác chỉ số Độ hỗ trợ (Support), Độ tin cậy (Confidence) và Độ nâng (Lift) để gợi ý mặt hàng mua kèm tối ưu.
* **Cơ chế Fallback Cold-Start**: Tự động gợi ý sản phẩm cùng danh mục khi mặt hàng mới chưa có đủ lịch sử đồng xuất hiện.
* **Google Gemini AI API**: Tích hợp mô hình Gemini để hỗ trợ khách hàng hỏi đáp trực tuyến, tư vấn công thức nấu ăn và sản phẩm dinh dưỡng.
* **Offline Fallback Engine**: Tự động nhận diện từ khóa và gợi ý sản phẩm ngay cả khi chưa cấu hình Gemini API Key.

### 3. Frontend (Single Page Application - SPA)
* **Framework**: React 18.2.
* **Build Tool**: Vite 5.0 (tốc độ HMR tức thì, đóng gói bundle tự động vào `src/main/resources/static`).
* **Điều hướng**: React Router DOM v6 (Nested Routes, Route Guards, ProtectedRoute phân quyền `ROLE_ADMIN`, `ROLE_SHIPPER`, `ROLE_USER`).
* **HTTP Client**: Axios (Cấu hình Interceptor tự động đính kèm Bearer Token).
* **Biểu đồ & Thống kê**: Recharts (Vẽ biểu đồ AreaChart, BarChart, PieChart doanh thu, đơn hàng và tỷ lệ hủy kho).
* **Icon Library**: Lucide React.
* **Giao diện & Trải nghiệm**: CSS3 hiện đại (Glassmorphism, Card bóng bẩy, Responsive Mobile/Desktop).

### 4. Database, NoSQL & Lưu Trữ
* **Hệ quản trị CSDL Quan hệ**: Microsoft SQL Server (2016, 2019, 2022).
* **Script khởi tạo**: File đơn nhất duy nhất [valkey.sql](valkey.sql) tạo Database `valkey`, chứa trọn vẹn 24 bảng chuẩn JPA, ràng buộc khóa ngoại, chỉ mục, dữ liệu phân lô có giá vốn, kiểm định QC và 50 đơn hàng lịch sử TP.HCM.
* **NoSQL Message Broker & Hàng Đợi Bất Đồng Bộ**: **Valkey 7.2 (Open-Source Redis alternative)** tích hợp Stream, Hash cache, Consumer Group xử lý đơn hàng chống tranh chấp kho (race condition / overselling).
* **Valkey Admin Web GUI**: Quản trị trực quan cấu trúc Key/Stream/Memory Valkey tại cổng `8085` (`http://localhost:8085`).
* **Lưu trữ tệp**: Local File System Storage (Thư mục [uploads/](uploads/) lưu trữ ảnh sản phẩm, ảnh danh mục và mã QR ngân hàng).

---

## 🏗️ Kiến Trúc Ứng Dụng & Mô Hình Spring Boot MVC

Hệ thống được thiết kế theo mô hình **Spring Boot MVC Decoupled** chuẩn mực doanh nghiệp, chia tách rõ ràng 3 tầng:

```
+---------------------------------------------------------------------------------------------------+
|                                        VIEW (Giao Diện)                                           |
|   React 18 SPA (Client-Side Rendering) - Quản lý UI, State & Router tức thời (Zero Reload)       |
+-------------------------------------------------+-------------------------------------------------+
                                                  |  (Gọi bất đồng bộ HTTP Request kèm Bearer JWT)
                                                  v
+---------------------------------------------------------------------------------------------------+
|                                    CONTROLLER (Bộ Điều Phối)                                      |
|   Spring Boot MVC Core: DispatcherServlet -> Security Filter -> 24 REST Controllers (@RestController)   |
|   - Định tuyến endpoint, xác thực phân quyền, Validate @Valid và phản hồi dữ liệu chuẩn JSON      |
+-------------------------------------------------+-------------------------------------------------+
                                                  |  (Gọi xử lý nghiệp vụ)
                                                  v
+---------------------------------------------------------------------------------------------------+
|                                      MODEL (Dữ Liệu & Nghiệp Vụ)                                   |
|   - Tầng DTO: Đóng gói và chuẩn hóa dữ liệu trao đổi (Data Transfer Objects)                     |
|   - Tầng Service: Logic nghiệp vụ (OrderService, ProductBatchService, RecommendationService,...)  |
|   - Tầng Repository & Entity: Spring Data JPA / Hibernate tương tác với Microsoft SQL Server     |
+---------------------------------------------------------------------------------------------------+
```

### 📦 Chi Tiết Các Tầng:
1. **Model (M)**:
   * **Entities**: 22 lớp thực thể ánh xạ trực tiếp các bảng CSDL ([Product.java](file:///d:/NAM%20CUOI/CNJAVA/DO_AN_CONGNGHEJAVA/src/main/java/com/groceryshop/entity/Product.java), [ProductBatch.java](file:///d:/NAM%20CUOI/CNJAVA/DO_AN_CONGNGHEJAVA/src/main/java/com/groceryshop/entity/ProductBatch.java), [GoodsReceiptItem.java](file:///d:/NAM%20CUOI/CNJAVA/DO_AN_CONGNGHEJAVA/src/main/java/com/groceryshop/entity/GoodsReceiptItem.java), [Coupon.java](file:///d:/NAM%20CUOI/CNJAVA/DO_AN_CONGNGHEJAVA/src/main/java/com/groceryshop/entity/Coupon.java), [Order.java](file:///d:/NAM%20CUOI/CNJAVA/DO_AN_CONGNGHEJAVA/src/main/java/com/groceryshop/entity/Order.java),...).
   * **Repositories**: Kế thừa `JpaRepository`, thực thi các câu truy vấn mở rộng.
   * **Services**: Đóng gói toàn bộ logic cốt lõi:
     - [RecommendationService.java](file:///d:/NAM%20CUOI/CNJAVA/DO_AN_CONGNGHEJAVA/src/main/java/com/groceryshop/service/RecommendationService.java): Thuật toán Apriori tính Lift, Confidence và Cold-Start fallback.
     - [OrderService.java](file:///d:/NAM%20CUOI/CNJAVA/DO_AN_CONGNGHEJAVA/src/main/java/com/groceryshop/service/OrderService.java): Bán hàng đa lô, tự động tách dòng đơn hàng và trừ kho FIFO (bỏ qua lô quá hạn).
     - [ProductBatchService.java](file:///d:/NAM%20CUOI/CNJAVA/DO_AN_CONGNGHEJAVA/src/main/java/com/groceryshop/service/ProductBatchService.java): Quản lý lô, chặn sale hàng hết hạn, tiêu hủy lô ghi sổ cái kho.
     - [GoodsReceiptService.java](file:///d:/NAM%20CUOI/CNJAVA/DO_AN_CONGNGHEJAVA/src/main/java/com/groceryshop/service/GoodsReceiptService.java): Quy trình kiểm định chất lượng đầu vào (Inward QC) phân tách số lượng đạt/không đạt.
     - [CouponService.java](file:///d:/NAM%20CUOI/CNJAVA/DO_AN_CONGNGHEJAVA/src/main/java/com/groceryshop/service/CouponService.java): Tính chiết khấu voucher theo đúng ngành hàng áp dụng.
2. **Controller (C)**:
   * 24 REST Controllers tiếp nhận request, kiểm tra tính hợp lệ dữ liệu `@Valid`, kiểm soát phân quyền `@PreAuthorize` và tuần tự hóa kết quả trả về chuẩn JSON.
   * [SpaController.java](file:///d:/NAM%20CUOI/CNJAVA/DO_AN_CONGNGHEJAVA/src/main/java/com/groceryshop/controller/SpaController.java): Điều hướng các URL giao diện về file `index.html` của React khi người dùng F5 hoặc truy cập trực tiếp.
3. **View (V)**:
   * Ứng dụng Single Page Application (React 18) tương tác với Controller qua các API call bất đồng bộ (Axios), mang lại trải nghiệm mượt mà, không giật lag.

---

## ✨ Tính Năng Nổi Bật & Các Điểm Mới Nâng Cấp

### 1. 🧠 Thuật Toán Gợi Ý Sản Phẩm Mua Kèm Apriori (Market Basket Analysis)
* Phân tích hành vi mua sắm từ lịch sử các hóa đơn trong hệ thống:
  $$\text{Confidence}(A \to B) = \frac{\text{freq}(A \cap B)}{\text{freq}(A)}, \quad \text{Lift}(A \to B) = \frac{\text{Confidence}(A \to B)}{\text{Support}(B)}$$
* Lọc ra các sản phẩm thường được khách hàng mua cùng với chỉ số $\text{Lift} \ge 1.0$, hiển thị tại trang [ProductDetail.jsx](file:///d:/NAM%20CUOI/CNJAVA/DO_AN_CONGNGHEJAVA/frontend/src/pages/ProductDetail.jsx) với nhãn **"Thường mua cùng"**.
* Cơ chế tự động Cold-Start fallback sang các mặt hàng **"Cùng danh mục"** khi sản phẩm chưa có đủ dữ liệu hóa đơn.

### 2. 📦 Nghiệp Vụ Bán Hàng Đa Lô (Multi-Batch Sale) & Hóa Đơn Tách Dòng Minh Bạch
* Khi một sản phẩm vừa có **lô cận date giảm giá xả kho** ($Q_{sale}$, giá $P_{sale}$), vừa có **lô tiêu chuẩn mới nhập** (giá gốc $P_{gốc}$):
  * Khi khách hàng mua số lượng $N > Q_{sale}$: Hệ thống tự động phân bổ $Q_{sale}$ sản phẩm giá rẻ và $(N - Q_{sale})$ sản phẩm theo giá gốc.
  * Hiển thị thông báo màu vàng thông minh tại [Cart.jsx](file:///d:/NAM%20CUOI/CNJAVA/DO_AN_CONGNGHEJAVA/frontend/src/pages/Cart.jsx) và [Checkout.jsx](file:///d:/NAM%20CUOI/CNJAVA/DO_AN_CONGNGHEJAVA/frontend/src/pages/Checkout.jsx).
  * Trong đơn hàng và hóa đơn in tại [InvoiceManager.jsx](file:///d:/NAM%20CUOI/CNJAVA/DO_AN_CONGNGHEJAVA/frontend/src/pages/admin/InvoiceManager.jsx): Tự động tách thành **2 dòng sản phẩm riêng biệt** rõ ràng, minh bạch 100% về giá bán và hạn sử dụng.
  * Trừ kho FIFO: **Tuyệt đối không trừ vào các lô đã quá hạn sử dụng khi bán lẻ**.

### 3. 🛡️ Quản Lý Lô Quá Hạn, Tiêu Hủy Kho & Báo Cáo Tổn Thất Vốn
* **Bảo vệ an toàn**: Chặn triệt để nút "Thiết lập Sale" với các lô hàng đã quá hạn sử dụng (`expiryDate < LocalDate.now()`).
* **Tab Lô Hàng Đã Hết Hạn**: Tab riêng biệt gắn badge đỏ tại [ProductBatchManager.jsx](file:///d:/NAM%20CUOI/CNJAVA/DO_AN_CONGNGHEJAVA/frontend/src/pages/admin/ProductBatchManager.jsx) hiển thị số lượng tồn, giá vốn nhập và tổng vốn thiệt hại.
* **Quy trình Xuất Hủy Kho (`POST /api/admin/batches/{id}/dispose`)**: Giảm số lượng lô về 0, chuyển trạng thái sang `DISPOSED`, trừ tổng tồn kho và ghi sổ cái kho loại `EXPIRED_DISPOSAL`.
* **Báo Cáo Tổn Thất Vốn (`GET /api/admin/reports/expired-batches`)**: Card KPI thống kê chi tiết số lô hủy, tổng số lượng và số tiền vốn bị thất thoát tại [ReportManager.jsx](file:///d:/NAM%20CUOI/CNJAVA/DO_AN_CONGNGHEJAVA/frontend/src/pages/admin/ReportManager.jsx).

### 4. 💰 Cơ Chế 3 Lớp Fallback Bảo Vệ Giá Vốn Lô Hàng (`importPrice`)
* **Lớp 1 (Quan hệ chứng từ)**: Lấy giá nhập trực tiếp từ bản ghi `goods_receipt_items` tương ứng với mã lô.
* **Lớp 2 (Ước lượng dự phòng SQL)**: Tự động gán bằng 70% giá niêm yết cho các lô tạo độc lập không qua phiếu nhập.
* **Lớp 3 (Bảo vệ Entity trong Java)**: Phương thức `getEffectiveImportPrice()` trong [ProductBatch.java](file:///d:/NAM%20CUOI/CNJAVA/DO_AN_CONGNGHEJAVA/src/main/java/com/groceryshop/entity/ProductBatch.java) đảm bảo luôn trả về `BigDecimal > 0`, triệt tiêu hoàn toàn nguy cơ lỗi `NullPointerException`.

### 5. 🔍 Quy Trình Kiểm Định Chất Lượng Đầu Vào (Inward QC) & Khóa Tồn Kho Thủ Công
* **Khóa nhập tay tồn kho**: Khi tạo hoặc sửa thông tin sản phẩm tại [ProductManager.jsx](file:///d:/NAM%20CUOI/CNJAVA/DO_AN_CONGNGHEJAVA/frontend/src/pages/admin/ProductManager.jsx), ô tồn kho bị vô hiệu hóa (`disabled`) và mặc định bằng 0. Tồn kho chỉ được gia tăng qua quy trình nhập kho và kiểm định thực tế.
* **Quy trình kiểm định Inward QC 2 bước**:
  - Phiếu nhập kho mới từ NCC có trạng thái `CHỜ KIỂM ĐỊNH QC`.
  - Bộ phận kiểm hàng đánh giá: Ghi nhận số lượng đạt chuẩn ($Q_{passed}$) và số lượng lỗi/hỏng ($Q_{rejected}$) kèm lý do cụ thể.
  - Tự động cộng tồn kho và tạo lô mới đúng bằng $Q_{passed}$. Phần $Q_{rejected}$ được lập biên bản trả về nhà cung cấp và ghi vết vào Sổ cái kho.
  - Hỗ trợ in trực tiếp **Biên bản kiểm định chất lượng đầu vào**.

### 6. 🏷️ Nâng Cấp Mã Giảm Giá (Coupon) Theo Ngành Hàng & Hạn Mức Tối Đa
* Mã giảm giá có thể cấu hình áp dụng cho toàn bộ giỏ hàng hoặc chỉ áp dụng riêng cho một **Danh mục cụ thể** (`applicable_category_id`).
* Bổ sung trường hạn mức giảm tối đa (`max_discount_amount`) đối với coupon theo tỷ lệ phần trăm (%).
* [CouponService.java](file:///d:/NAM%20CUOI/CNJAVA/DO_AN_CONGNGHEJAVA/src/main/java/com/groceryshop/service/CouponService.java) tự động tính toán chiết khấu chỉ trên tổng tiền của các mặt hàng thỏa mãn điều kiện ngành hàng.

### 7. 🖼️ Giao Diện Bộ Lọc Danh Mục Với Thumbnail Hình Ảnh Trực Quan
* Bộ lọc danh mục tại [ProductList.jsx](file:///d:/NAM%20CUOI/CNJAVA/DO_AN_CONGNGHEJAVA/frontend/src/pages/ProductList.jsx) và [Home.jsx](file:///d:/NAM%20CUOI/CNJAVA/DO_AN_CONGNGHEJAVA/frontend/src/pages/Home.jsx) được nâng cấp hiển thị thumbnail/icon đại diện bắt mắt, hỗ trợ người dùng lọc nhanh mặt hàng yêu thích trên cả điện thoại và máy tính.

### 8. 🚚 Điều Phối Vận Chuyển Tự Động Theo Địa Bàn & Giao Hàng Shipper
* Tự động nhận diện Quận/Huyện từ chuỗi địa chỉ giao hàng của khách:
  * Khách tại **Quận Tân Phú** ➔ Điều phối tự động cho Shipper 1 (`shipper1`).
  * Khách tại **Quận Tân Bình** ➔ Điều phối tự động cho Shipper 2 (`shipper2`).
  * Khách tại **Quận 12** ➔ Điều phối tự động cho Shipper 3 (`shipper3`).
* Tính năng tự động phân bổ hàng loạt với 1 click, phân công thủ công và xử lý đơn giao thất bại để điều phối lại (`reassign`).

### 9. 💳 Thống Nhất Duyệt Thanh Toán & Cấu Hình Cổng Thanh Toán Đa Kênh
* Quản trị viên duyệt trạng thái thanh toán và xác nhận đơn hàng tập trung trực tiếp ngay tại [OrderManager.jsx](file:///d:/NAM%20CUOI/CNJAVA/DO_AN_CONGNGHEJAVA/frontend/src/pages/admin/OrderManager.jsx).
* Trang [PaymentManager.jsx](file:///d:/NAM%20CUOI/CNJAVA/DO_AN_CONGNGHEJAVA/frontend/src/pages/admin/PaymentManager.jsx) tập trung quản lý bật/tắt cổng (COD, Chuyển khoản VietQR, Ví MoMo, VNPAY) và tải lên hình ảnh mã QR ngân hàng động.

### 10. 👑 Hệ Thống Khách Hàng Thân Thiết & Tích Điểm VIP Loyalty
* 4 Hạng thẻ thành viên: **Đồng (BRONZE) -> Bạc (SILVER) -> Vàng (GOLD) -> Kim Cương (DIAMOND)**.
* Tự động tích lũy điểm khi hoàn tất đơn hàng và tự động thăng hạng.
* Chiết khấu trực tiếp trên đơn hàng (lên tới 8%) kèm đặc quyền miễn phí vận chuyển.

---

## 👥 Chi Tiết Các Phân Hệ Chức Năng

### 1. Phân Hệ Khách Hàng (User / Client Portal)

* **Trang Chủ ([Home.jsx](file:///d:/NAM%20CUOI/CNJAVA/DO_AN_CONGNGHEJAVA/frontend/src/pages/Home.jsx))**:
  - Banner quảng bá ưu đãi, khối danh mục sản phẩm kèm hình ảnh thumbnail sinh động.
  - Danh mục hàng bán chạy, hàng xả kho cận date giá sốc và thực phẩm tươi sạch.
* **Danh Mục & Tìm Kiếm ([ProductList.jsx](file:///d:/NAM%20CUOI/CNJAVA/DO_AN_CONGNGHEJAVA/frontend/src/pages/ProductList.jsx))**:
  - Tìm kiếm tương đối theo tên, bộ lọc đa tiêu chí (danh mục, khoảng giá, thương hiệu).
  - Bộ lọc danh mục dạng thẻ thumbnail trực quan dễ thao tác.
* **Chi Tiết Sản Phẩm ([ProductDetail.jsx](file:///d:/NAM%20CUOI/CNJAVA/DO_AN_CONGNGHEJAVA/frontend/src/pages/ProductDetail.jsx))**:
  - Hình ảnh sản phẩm chất lượng cao, giá niêm yết, giá khuyến mãi xả kho, tồn kho khả dụng.
  - **Khối Gợi Ý Mua Kèm Apriori**: Danh sách sản phẩm thường được mua cùng kèm chỉ số Lift/Confidence hoặc sản phẩm cùng danh mục.
* **Giỏ Hàng Thông Minh ([Cart.jsx](file:///d:/NAM%20CUOI/CNJAVA/DO_AN_CONGNGHEJAVA/frontend/src/pages/Cart.jsx))**:
  - Kiểm tra tồn kho khả dụng thời gian thực.
  - Cảnh báo thông minh về số lượng lô xả kho cận date và lô tiêu chuẩn.
* **Thanh Toán & Đặt Hàng ([Checkout.jsx](file:///d:/NAM%20CUOI/CNJAVA/DO_AN_CONGNGHEJAVA/frontend/src/pages/Checkout.jsx))**:
  - Sổ địa chỉ nhận hàng, chọn phương thức thanh toán linh hoạt (COD / VietQR / MoMo).
  - Áp dụng mã giảm giá (kiểm tra điều kiện ngành hàng và giới hạn tối đa).
  - Tự động cộng dồn chiết khấu hội viên VIP Loyalty.
  - Bảng kê đơn hàng hiển thị tách 2 dòng lô cận date & lô mới nếu có.
* **Lịch Sử Đơn Hàng ([OrderHistory.jsx](file:///d:/NAM%20CUOI/CNJAVA/DO_AN_CONGNGHEJAVA/frontend/src/pages/OrderHistory.jsx))**:
  - Theo dõi hành trình đơn: `CHỜ XÁC NHẬN` ➔ `ĐÃ XÁC NHẬN` ➔ `ĐANG GIAO` ➔ `HOÀN THÀNH` (hoặc `ĐÃ HỦY`).
* **Cổng Thành Viên VIP ([Loyalty.jsx](file:///d:/NAM%20CUOI/CNJAVA/DO_AN_CONGNGHEJAVA/frontend/src/pages/Loyalty.jsx))**:
  - Thẻ VIP điện tử sang trọng (Bronze, Silver, Gold, Diamond), thanh tiến trình thăng hạng và lịch sử tích điểm.
* **Trợ Lý Trí Tuệ Nhân Tạo & Chăm Sóc Khách Hàng**:
  - ChatBot AI Google Gemini tư vấn 24/7 và khung gửi tin nhắn phản hồi hai chiều tới ban quản trị.

---

### 2. Phân Hệ Quản Trị Viên (Admin Console)

Truy cập tại: `/admin` (Yêu cầu quyền `ROLE_ADMIN`).

#### A. Quản Lý Sản Phẩm & Danh Mục ([ProductManager.jsx](file:///d:/NAM%20CUOI/CNJAVA/DO_AN_CONGNGHEJAVA/frontend/src/pages/admin/ProductManager.jsx))
* Giao diện hợp nhất 2 Sub-tab: **Sản phẩm & Tồn kho** và **Danh mục**.
* 3 Thẻ KPI đầu trang: Tổng lượng tồn kho, Mặt hàng sắp hết (`<= minStock`), Mặt hàng hết tồn (`= 0`).
* Nút `+ Tạo phiếu nhập` màu xanh lá nổi bật, dẫn trực tiếp sang tạo phiếu nhập kho.
* Khóa trường tồn kho thủ công, ngăn chặn việc sửa tay sai lệch thực tế.

#### B. Lịch Sử & Nhập Kho Nâng Cao ([WarehouseHistoryManager.jsx](file:///d:/NAM%20CUOI/CNJAVA/DO_AN_CONGNGHEJAVA/frontend/src/pages/admin/WarehouseHistoryManager.jsx))
* **Tab 1 - Phiếu Nhập Kho**: Lập phiếu nhập từ NCC đa thương hiệu, chọn sản phẩm, số lượng, đơn giá và số lô dự kiến.
* **Tab 2 - Quản Lý Chất Lượng Đầu Vào (Inward QC)**: Đánh giá phiếu nhập, ghi nhận số lượng đạt chuẩn ($Q_{passed}$) và số lượng từ chối ($Q_{rejected}$) kèm lý do, in Biên bản kiểm định chất lượng.
* **Tab 3 - Sổ Cái Biến Động Kho**: Tra cứu toàn bộ lịch sử xuất/nhập/bán/hủy hàng hóa.

#### C. Quản Lý Lô Hàng & Hạn Dùng ([ProductBatchManager.jsx](file:///d:/NAM%20CUOI/CNJAVA/DO_AN_CONGNGHEJAVA/frontend/src/pages/admin/ProductBatchManager.jsx))
* Quản lý số lô, ngày sản xuất, hạn sử dụng, giá vốn nhập (`importPrice`) và giá xả kho (`salePrice`).
* Cảnh báo cận date 30 - 60 ngày; thiết lập giá xả hàng khuyến mãi cho lô.
* **Tab Lô Hàng Đã Hết Hạn**: Hiển thị riêng các lô quá date kèm tổng vốn thiệt hại; nút **Xuất Hủy Kho** tự động ghi sổ cái `EXPIRED_DISPOSAL`.

#### D. Quản Lý Đơn Hàng & Vận Chuyển ([OrderManager.jsx](file:///d:/NAM%20CUOI/CNJAVA/DO_AN_CONGNGHEJAVA/frontend/src/pages/admin/OrderManager.jsx) & [DeliveryManager.jsx](file:///d:/NAM%20CUOI/CNJAVA/DO_AN_CONGNGHEJAVA/frontend/src/pages/admin/DeliveryManager.jsx))
* Quản lý toàn bộ danh sách đơn hàng, xác nhận duyệt đơn và duyệt thanh toán đồng bộ.
* Tự động nhận diện quận huyện từ địa chỉ và phân bổ shipper theo địa bàn phụ trách.
* Phân công shipper thủ công hoặc hàng loạt; quản lý đơn giao thất bại để tái điều phối.

#### E. Quản Lý Hóa Đơn & Thanh Toán ([InvoiceManager.jsx](file:///d:/NAM%20CUOI/CNJAVA/DO_AN_CONGNGHEJAVA/frontend/src/pages/admin/InvoiceManager.jsx) & [PaymentManager.jsx](file:///d:/NAM%20CUOI/CNJAVA/DO_AN_CONGNGHEJAVA/frontend/src/pages/admin/PaymentManager.jsx))
* In hóa đơn bán lẻ chuẩn thu ngân (tách rõ 2 dòng lô cận date & lô mới nếu có).
* Xuất danh sách hóa đơn ra file CSV/Excel phục vụ đối soát.
* Cấu hình bật/tắt các cổng thanh toán và tải lên ảnh mã QR chuyển khoản.

#### F. Báo Cáo & Phân Tích Hoạt Động ([ReportManager.jsx](file:///d:/NAM%20CUOI/CNJAVA/DO_AN_CONGNGHEJAVA/frontend/src/pages/admin/ReportManager.jsx))
* Thống kê doanh thu theo thời gian, tỷ lệ đơn hàng thành công, phương thức thanh toán.
* **Card KPI Thất thoát hàng hết hạn**: Báo cáo tổng vốn thiệt hại từ các lô đã tiêu hủy kèm danh sách chi tiết.

#### G. Quản Lý Khách Hàng, Mã Giảm Giá & Tiếp Thị
* [CouponManager.jsx](file:///d:/NAM%20CUOI/CNJAVA/DO_AN_CONGNGHEJAVA/frontend/src/pages/admin/CouponManager.jsx): Tạo voucher theo tỷ lệ % hoặc tiền mặt, cấu hình theo danh mục cụ thể và giới hạn mức giảm tối đa.
* [UserManager.jsx](file:///d:/NAM%20CUOI/CNJAVA/DO_AN_CONGNGHEJAVA/frontend/src/pages/admin/UserManager.jsx): Quản lý người dùng, phân quyền, xem hạng VIP và điểm thưởng.
* [SupplierManager.jsx](file:///d:/NAM%20CUOI/CNJAVA/DO_AN_CONGNGHEJAVA/frontend/src/pages/admin/SupplierManager.jsx): Quản lý danh sách nhà cung cấp đối tác.
* [ReviewManager.jsx](file:///d:/NAM%20CUOI/CNJAVA/DO_AN_CONGNGHEJAVA/frontend/src/pages/admin/ReviewManager.jsx): Duyệt đánh giá và nhận xét sản phẩm.
* Quản lý hội thoại và phản hồi tin nhắn của khách hàng.

---

### 3. Phân Hệ Nhân Viên Giao Hàng (Shipper Console)

Truy cập tại: `/shipper` (Yêu cầu quyền `ROLE_SHIPPER` hoặc `ROLE_ADMIN`).

* **Bảng Thống Kê Giao Hàng**: Đơn cần giao, đơn hoàn thành trong ngày, số tiền COD cần nộp về quỹ.
* **Danh Sách Đơn Hàng Theo Quận**: Đơn hàng do Admin hoặc hệ thống phân bổ theo đúng quận phụ trách của shipper.
* **Thao Tác Giao Nhận**:
  - Bấm `Bắt đầu giao` ➔ Chuyển đơn sang trạng thái `ĐANG GIAO HÀNG`.
  - Gọi điện nhanh cho khách hàng từ giao diện.
  - Xác nhận `Giao hàng thành công` (thu tiền COD) hoặc báo cáo `Giao hàng thất bại` kèm lý do cụ thể.

---

## 🗄️ Cấu Trúc Cơ Sở Dữ Liệu (Database Schema)

Cơ sở dữ liệu **Microsoft SQL Server** được khởi tạo hoàn chỉnh từ file duy nhất [valkey.sql](valkey.sql) (tạo CSDL `valkey`) gồm 24 bảng thực thể chuẩn hóa:

| STT | Tên Bảng | Mục Đích & Cột Nổi Bật Mới Nâng Cấp |
| :--- | :--- | :--- |
| 1 | `roles` | Danh sách vai trò hệ thống (`ROLE_ADMIN`, `ROLE_USER`, `ROLE_SHIPPER`). |
| 2 | `users` | Tài khoản, mật khẩu mã hóa BCrypt, `loyalty_points`, `membership_tier`, OAuth2 provider. |
| 3 | `categories` | Danh mục ngành hàng, mô tả và hình ảnh đại diện (`image`). |
| 4 | `brands` | Thương hiệu sản phẩm (Vinamilk, CP, Barona, Masan,...). |
| 5 | `suppliers` | Danh sách nhà cung cấp (MST, điện thoại, email, địa chỉ). |
| 6 | `products` | Thông tin mặt hàng, giá niêm yết, giá khuyến mãi, đơn vị tính, mô tả, ảnh chính. |
| 7 | `product_images` | Bộ sưu tập hình ảnh chi tiết của sản phẩm. |
| 8 | `product_batches` | **Quản lý phân lô**: `batch_name`, `quantity`, `expiry_date`, **`import_price` (giá vốn nhập)**, **`sale_price` (giá xả kho)**, **`status` (`ACTIVE`/`DISPOSED`)**. |
| 9 | `inventory` | Quản lý tồn kho thực tế (`current_stock`, `min_stock`, `max_stock`). |
| 10 | `goods_receipt` | Thông tin phiếu nhập kho hàng từ nhà cung cấp (`receipt_number`, `status`). |
| 11 | `goods_receipt_items` | **Chi tiết nhập & Kiểm định QC**: `quantity`, `import_price`, **`passed_quantity`**, **`rejected_quantity`**, **`reject_reason`**, **`qc_status`**, **`qc_note`**, **`inspected_at`**, **`inspected_by`**. |
| 12 | `inventory_ledger` | Sổ cái kho ghi nhận toàn bộ biến động: `RECEIPT`, `SELL`, `RETURN`, `EXPIRED_DISPOSAL`, `QC_REJECT`. |
| 13 | `coupons` | **Mã giảm giá**: `code`, `discount_type`, `discount_value`, **`applicable_category_id` (áp dụng theo ngành hàng)**, **`max_discount_amount` (giảm tối đa)**. |
| 14 | `addresses` | Sổ địa chỉ nhận hàng của khách (tỉnh/thành, quận/huyện, phường/xã, chi tiết). |
| 15 | `cart` | Giỏ hàng của từng khách hàng. |
| 16 | `cart_items` | Chi tiết mặt hàng và số lượng nằm trong giỏ. |
| 17 | `orders` | Đơn đặt hàng, tổng tiền, chiết khấu VIP, trạng thái (`CHO_TIEP_NHAN`, `CHO_XAC_NHAN`, `DANG_GIAO`, `HOAN_THANH`, `TU_CHOI`, `HUY`), `idempotency_key`, shipper phân công. |
| 18 | `order_items` | Chi tiết từng sản phẩm trong đơn (lưu tên riêng biệt cho lô cận date và lô tiêu chuẩn). |
| 19 | `payment_method_configs` | Cấu hình bật/tắt các phương thức thanh toán và lưu URL ảnh mã QR động. |
| 20 | `payments` | Lịch sử và trạng thái giao dịch thanh toán (`PENDING`, `APPROVED`, `FAILED`). |
| 21 | `reviews` | Đánh giá số sao (1-5 sao) và bình luận trải nghiệm của khách hàng. |
| 22 | `customer_messages` | Hộp thư tin nhắn trao đổi hai chiều giữa khách hàng và quản trị viên. |
| 23 | `order_queue_jobs` | **Hàng đợi Valkey Job Queue**: `job_id`, `order_id`, `job_type`, `status` (`PENDING`/`PROCESSING`/`SUCCEEDED`/`FAILED`/`CANCELLED`), `retry_count`, `max_retries`. |
| 24 | `inventory_reservations` | **Tạm giữ tồn kho bất đồng bộ**: `order_id`, `product_id`, `batch_id`, `quantity`, `status` (`RESERVED`/`COMMITTED`/`RELEASED`). |

---

## 📂 Cấu Trúc Thư Mục Dự Án

```
DOAN_NOSQL/
├── pom.xml                                  # Cấu hình Maven dependencies (Java 21, Spring Boot 3.2, Data Redis/Valkey)
├── mvnw / mvnw.cmd                          # Maven Wrapper cho Windows và Linux
├── run.bat                                  # Script khởi chạy nhanh 1-Click Spring Boot trên Windows
├── run-valkey.bat                           # Script khởi chạy nhanh 1-Click Valkey & Valkey Admin Docker
├── docker-compose.yml                       # Khởi chạy cụm Valkey (6379) & Valkey Admin Web GUI (8085)
├── valkey.sql                               # Script CSDL duy nhất: 24 bảng, nạp hạt giống & 50 đơn hàng HCM
├── docker/valkey/valkey.conf                # Cấu hình bộ nhớ và lưu bền AOF/RDB cho Valkey
├── QUY_TRINH_NGHIEP_VU_CHI_TIET.md          # Tài liệu quy trình nghiệp vụ BPM chi tiết kèm sơ đồ Mermaid
├── PLAN_VALKEY_ADMIN_MINI_MARKET.md         # Kế hoạch & kiến trúc tích hợp Valkey NoSQL Queue
├── PLAN.md / plan2.md                       # Tài liệu kiến trúc và nâng cấp hệ thống kho đa lô
├── uploads/                                 # Thư mục lưu trữ hình ảnh sản phẩm và mã QR upload
│
├── src/main/java/com/groceryshop/           # Mã nguồn Backend (Java Spring Boot 3.2)
│   ├── GroceryShopApplication.java          # Main Application Class
│   ├── config/                              # Cấu hình Spring Boot (DataInitializer, WebConfig)
│   ├── security/                            # Spring Security 6 & Stateless JWT
│   │   ├── SecurityConfig.java              # Cấu hình phân quyền endpoints & bộ lọc
│   │   ├── JwtTokenProvider.java            # Tạo, giải mã và xác thực JWT token
│   │   └── JwtAuthenticationFilter.java     # Filter chặn bắt Authorization Bearer header
│   ├── controller/                          # 24 RESTful Controllers xử lý API
│   │   ├── AuthController.java              # Đăng ký, đăng nhập JWT, đổi mật khẩu
│   │   ├── ProductController.java           # Quản lý sản phẩm & API gợi ý Apriori
│   │   ├── ProductBatchController.java      # Quản lý Lô, HSD, xả hàng & xuất hủy lô hết hạn
│   │   ├── GoodsReceiptController.java      # Phiếu nhập kho & kiểm định chất lượng Inward QC
│   │   ├── OrderController.java             # Tạo đơn hàng, duyệt đơn & duyệt thanh toán
│   │   ├── DeliveryController.java          # Tự động điều phối đơn hàng theo quận huyện
│   │   ├── ShipperController.java           # Bảng điều khiển riêng cho Shipper
│   │   ├── ReportController.java            # Báo cáo doanh thu & Báo cáo tổn thất hàng hết hạn
│   │   ├── CouponController.java            # Quản lý mã giảm giá theo danh mục
│   │   ├── ChatController.java              # ChatBot AI Gemini & Offline Fallback
│   │   └── SpaController.java               # Forwarding route cho Single Page Application
│   ├── entity/                              # 22 Thực thể JPA tương ứng với các bảng DB
│   ├── repository/                          # Giao diện Spring Data JPA Repositories
│   ├── service/                             # Tầng xử lý logic nghiệp vụ
│   │   ├── RecommendationService.java       # Thuật toán Apriori (Confidence, Lift, Cold-Start)
│   │   ├── OrderService.java                # Bán hàng đa lô, tách dòng hóa đơn & trừ kho FIFO
│   │   ├── ProductBatchService.java         # Quản lý lô, chặn sale quá date, hủy kho ghi sổ cái
│   │   ├── GoodsReceiptService.java         # Nhập kho & kiểm định chất lượng Inward QC
│   │   ├── CouponService.java               # Tính chiết khấu coupon theo ngành hàng
│   │   ├── ProductService.java              # Khóa chỉnh sửa tồn kho trực tiếp
│   │   ├── ReportService.java               # Tổng hợp báo cáo kinh doanh & tổn thất lô hủy
│   │   └── ...
│   ├── dto/                                 # Data Transfer Objects
│   │   ├── ProductRecommendationDTO.java    # DTO gợi ý sản phẩm kèm Confidence, Lift, Reason
│   │   ├── QCInspectionRequestDTO.java      # DTO tiếp nhận dữ liệu kiểm định QC
│   │   ├── CouponItemInfo.java              # DTO hỗ trợ kiểm tra coupon theo ngành hàng
│   │   └── ...
│   └── exception/                           # Global Exception Handler tập trung
│
├── src/main/resources/
│   ├── application.properties               # Cấu hình SQL Server, cổng 8080, JWT Secret, Gemini Key
│   └── static/                              # Chứa static bundle đã build từ React (HTML/CSS/JS)
│
└── frontend/                                # Mã nguồn Frontend (React 18 SPA + Vite)
    ├── package.json                         # Dependencies Frontend (React, Axios, Recharts, Lucide)
    ├── vite.config.js                       # Cấu hình Vite & Proxy chuyển tiếp cổng 8080
    ├── index.html                           # File HTML gốc của SPA
    └── src/
        ├── App.jsx                          # Cấu hình BrowserRouter, Routes và Context Providers
        ├── contexts/                        # State Management (AuthContext, CartContext)
        ├── services/api.js                  # Axios Instance kèm Interceptor gắn JWT Bearer token
        ├── layouts/                         # UserLayout và AdminLayout
        ├── pages/                           # Các màn hình người dùng
        │   ├── Home.jsx, Cart.jsx, Checkout.jsx, ProductDetail.jsx, ProductList.jsx, Loyalty.jsx...
        │   ├── admin/                       # Các màn hình quản trị chuyên sâu
        │   │   ├── ProductManager.jsx       # Quản lý Sản phẩm & Danh mục (khóa tồn kho thủ công)
        │   │   ├── WarehouseHistoryManager.jsx # Quản lý Lịch sử & Nhập kho (Phiếu nhập, Inward QC, Sổ cái)
        │   │   ├── ProductBatchManager.jsx  # Quản lý Lô HSD (Tab Lô hết hạn & Nút Xuất hủy)
        │   │   ├── OrderManager.jsx         # Quản lý đơn hàng & duyệt thanh toán tập trung
        │   │   ├── DeliveryManager.jsx      # Điều phối vận chuyển thông minh theo quận
        │   │   ├── InvoiceManager.jsx       # Quản lý hóa đơn, in ấn chuẩn và xuất CSV
        │   │   ├── PaymentManager.jsx       # Cấu hình phương thức thanh toán & Upload mã QR
        │   │   ├── ReportManager.jsx        # Báo cáo doanh thu & Tổn thất hàng hết hạn
        │   │   ├── CouponManager.jsx        # Quản lý voucher theo danh mục
        │   │   ├── UserManager.jsx          # Quản lý người dùng & VIP Loyalty
        │   │   └── SupplierManager.jsx      # Quản lý nhà cung cấp đối tác
        │   └── shipper/
        │       └── ShipperDashboard.jsx     # Bảng điều khiển riêng cho Shipper
        ├── components/                      # ChatBot AI Gemini, Feedback Widget, ProtectedRoute...
        └── utils/                           # Hàm tiện ích (In ấn hóa đơn, Xuất CSV, Format tiền tệ)
```

---

## 🔑 Tài Khoản Trải Nghiệm Mặc Định

Hệ thống đã nạp sẵn dữ liệu demo tương ứng với 3 vai trò người dùng (Mật khẩu mặc định là: `pass1234`):

| Vai Trò | Tên Đăng Nhập | Mật Khẩu | Họ Tên & Khu Vực / Đặc Quyền | Quyền Hạn (Role) |
| :--- | :--- | :--- | :--- | :--- |
| **Quản Trị Viên** | `admin` | `pass1234` | Quản Trị Viên Hệ Thống | `ROLE_ADMIN` |
| **Shipper 1** | `shipper1` | `pass1234` | Nguyễn Văn Giao (Phụ trách Quận Tân Phú) | `ROLE_SHIPPER` |
| **Shipper 2** | `shipper2` | `pass1234` | Trần Văn Tốc (Phụ trách Quận Tân Bình) | `ROLE_SHIPPER` |
| **Shipper 3** | `shipper3` | `pass1234` | Lê Hoàng Vũ (Phụ trách Quận 12) | `ROLE_SHIPPER` |
| **Khách Hàng VIP 1** | `user1` | `pass1234` | Nguyễn Văn An (**Hạng Kim Cương - DIAMOND**) | `ROLE_USER` |
| **Khách Hàng VIP 2** | `user2` | `pass1234` | Trần Thị Bình (**Hạng Vàng - GOLD**) | `ROLE_USER` |
| **Khách Hàng VIP 3** | `user3` | `pass1234` | Lê Hoàng Cường (**Hạng Bạc - SILVER**) | `ROLE_USER` |
| **Khách Hàng VIP 4** | `user4` | `pass1234` | Phạm Thu Dung (**Hạng Đồng - BRONZE**) | `ROLE_USER` |

---

## 🚀 Hướng Dẫn Cài Đặt & Khởi Chạy

### 1. Yêu Cầu Môi Trường
* **Java Development Kit (JDK)**: Phiên bản **Java 17** hoặc **Java 21 LTS** (Khuyến nghị Eclipse Temurin, Amazon Corretto hoặc Oracle JDK).
* **Cơ sở dữ liệu**: **Microsoft SQL Server** (2012, 2016, 2019, 2022).
* **Node.js**: Phiên bản 18+ hoặc 20+ (Dùng khi cần build lại giao diện frontend).
* **Trình quản lý gói**: npm hoặc yarn.

---

### 2. Thiết Lập Cơ Sở Dữ Liệu SQL Server & Valkey NoSQL

#### A. Thiết Lập SQL Server
1. Mở công cụ **SQL Server Management Studio (SSMS)** hoặc Azure Data Studio.
2. Kết nối tới SQL Server của bạn (cổng mặc định `1433`).
3. Mở file [valkey.sql](valkey.sql) và nhấn **Execute (F5)**.
   *(Script duy nhất sẽ tự động tạo cơ sở dữ liệu `valkey`, toàn bộ 24 bảng chuẩn JPA, khóa ngoại, chỉ mục, dữ liệu phân lô HSD có giá vốn, kiểm định QC và 50 đơn hàng lịch sử thực tế TP.HCM phục vụ thuật toán Apriori)*.
4. Kiểm tra tài khoản và mật khẩu kết nối CSDL trong file [src/main/resources/application.properties](src/main/resources/application.properties):
   ```properties
   spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=valkey;encrypt=true;trustServerCertificate=true
   spring.datasource.username=sa
   spring.datasource.password=123
   ```

#### B. Khởi Chạy Hàng Đợi NoSQL Valkey & Valkey Admin (Port 8085)
1. Cài đặt và mở Docker Desktop trên máy tính.
2. Nhấp đúp chuột chạy file [run-valkey.bat](run-valkey.bat) hoặc mở terminal gõ:
   ```bash
   docker compose up -d
   ```
3. Truy cập công cụ quản trị trực quan **Valkey Admin Web GUI** tại:
   - **URL**: `http://localhost:8085`
   - **Username**: `admin`
   - **Password**: `admin`

---

### 3. Khởi Chạy Nhanh Dự Án

#### ⚡ Cách 1: Khởi Chạy Nhanh 1-Click trên Windows
Double-click trực tiếp vào file [run.bat](run.bat) tại thư mục gốc:
* Script sẽ tự động nhận diện `JAVA_HOME`, kích hoạt bảng mã UTF-8 và khởi chạy máy chủ Spring Boot.
* Mở trình duyệt web và truy cập:
  * Website Khách hàng: `http://localhost:8080`
  * Trang Đăng nhập: `http://localhost:8080/login`
  * Trang Quản trị: `http://localhost:8080/admin`
  * Bảng Shipper: `http://localhost:8080/shipper`
  * Quản trị Hàng Đợi Valkey: `http://localhost:8080/admin/queue` (kết nối trực tiếp Web GUI cổng 8085)

#### 🛠️ Cách 2: Khởi Chạy Phục Vụ Phát Triển (Development Live Reload)
Nếu bạn muốn vừa chỉnh sửa code React vừa xem thay đổi tức thời:
1. **Terminal 1 (Backend)**:
   ```bash
   .\mvnw.cmd spring-boot:run
   ```
   *(Backend chạy tại `http://localhost:8080`)*.
2. **Terminal 2 (Frontend)**:
   ```bash
   cd frontend
   npm run dev
   ```
   *(Frontend chạy tại `http://localhost:5173`, tự động chuyển tiếp API sang cổng 8080)*.

#### 📦 Cách 3: Đóng Gói Lại Bản Build Khi Sửa Giao Diện
Khi thay đổi mã nguồn trong thư mục `frontend/`, chạy lệnh đóng gói để cập nhật vào Backend:
```bash
cd frontend
npm install
npm run build
cd ..
.\mvnw.cmd spring-boot:run
```
*(Các file sản phẩm sẽ được tự động xuất vào thư mục `src/main/resources/static`)*.

---

## 📡 Danh Sách RESTful API Chính

| Nhóm Chức Năng | Phương Thức | Endpoint URL | Phân Quyền | Mô Tả Nghiệp Vụ |
| :--- | :--- | :--- | :--- | :--- |
| **Xác Thực (Auth)** | `POST` | `/api/auth/login` | Public | Đăng nhập hệ thống, trả về Bearer JWT token |
| | `POST` | `/api/auth/register` | Public | Đăng ký tài khoản khách hàng mới |
| | `GET` | `/api/auth/me` | Authenticated | Lấy thông tin tài khoản đang đăng nhập |
| **Sản Phẩm & Danh Mục** | `GET` | `/api/public/products` | Public | Lấy danh sách sản phẩm, lọc theo danh mục, giá, từ khóa |
| | `GET` | `/api/public/products/{id}` | Public | Lấy chi tiết thông tin 1 sản phẩm |
| | `GET` | `/api/public/products/{id}/recommendations` | Public | **Gợi ý sản phẩm mua kèm bằng thuật toán Apriori** |
| | `POST` | `/api/admin/products` | ADMIN | Thêm mới sản phẩm (Tồn kho tự động gán = 0) |
| | `PUT` | `/api/admin/products/{id}` | ADMIN | Cập nhật sản phẩm (Khóa chỉnh sửa tồn kho trực tiếp) |
| | `GET` | `/api/public/categories` | Public | Lấy danh sách danh mục sản phẩm |
| | `POST` | `/api/admin/categories` | ADMIN | Thêm mới danh mục sản phẩm |
| **Giỏ Hàng & Đơn Hàng** | `GET` | `/api/cart` | Authenticated | Lấy danh sách sản phẩm trong giỏ hàng |
| | `POST` | `/api/cart/add` | Authenticated | Thêm sản phẩm vào giỏ hàng |
| | `POST` | `/api/orders` | Authenticated | **Tạo đơn hàng (Trừ kho FIFO, tách 2 dòng lô cận date & lô mới)** |
| | `GET` | `/api/orders/my-orders` | Authenticated | Lịch sử đơn hàng của khách hàng |
| | `GET` | `/api/admin/orders` | ADMIN | Danh sách toàn bộ đơn hàng hệ thống |
| | `PATCH` | `/api/admin/orders/{id}/status` | ADMIN | Cập nhật trạng thái đơn & tự động duyệt thanh toán |
| **Vận Chuyển & Shipper** | `GET` | `/api/admin/deliveries/shippers` | ADMIN | Lấy danh sách shipper và khu vực phụ trách |
| | `POST` | `/api/admin/deliveries/auto-assign` | ADMIN | **Tự động điều phối đơn hàng theo quận huyện** |
| | `POST` | `/api/admin/deliveries/assign` | ADMIN | Phân công shipper thủ công cho đơn hàng |
| | `GET` | `/api/shipper/orders/my-deliveries` | SHIPPER | Danh sách đơn hàng được gán cho shipper đang đăng nhập |
| | `PATCH` | `/api/shipper/orders/{id}/status` | SHIPPER | Cập nhật trạng thái giao (Thành công / Thất bại) |
| **Lô Hàng & Hạn Sử Dụng** | `GET` | `/api/admin/batches` | ADMIN | Danh sách tất cả các lô hàng kèm giá vốn |
| | `GET` | `/api/admin/batches/expiring` | ADMIN | Danh sách lô hàng sắp hết hạn (cận date) |
| | `GET` | `/api/admin/batches/expired` | ADMIN | **Danh sách lô hàng đã quá hạn sử dụng** |
| | `POST` | `/api/admin/batches/{id}/dispose` | ADMIN | **Xuất hủy lô quá hạn, trừ kho và ghi sổ cái kho** |
| | `POST` | `/api/admin/batches/batch/{id}/clearance-sale` | ADMIN | Thiết lập giá bán xả kho cho lô cụ thể |
| **Nhập Kho & Kiểm Định QC**| `GET` | `/api/admin/goods-receipts` | ADMIN | Danh sách phiếu nhập kho từ nhà cung cấp |
| | `POST` | `/api/admin/goods-receipts` | ADMIN | Lập phiếu nhập kho mới từ nhà cung cấp |
| | `POST` | `/api/admin/goods-receipts/{id}/qc-inspection` | ADMIN | **Kiểm định chất lượng đầu vào (Nhập kho hàng đạt chuẩn, từ chối hàng lỗi)** |
| | `PUT` | `/api/admin/goods-receipts/{id}/complete` | ADMIN | Hoàn tất phiếu nhập và đồng bộ giá vốn lô |
| | `GET` | `/api/admin/inventory-ledger` | ADMIN | Tra cứu sổ cái / thẻ kho xuất nhập tồn |
| **Mã Giảm Giá (Coupons)** | `GET` | `/api/public/coupons` | Public | Lấy danh sách voucher khuyến mãi khả dụng |
| | `POST` | `/api/admin/coupons` | ADMIN | **Tạo mã giảm giá (hỗ trợ áp dụng theo danh mục & mức giảm tối đa)** |
| **Hóa Đơn & Thanh Toán**| `GET` | `/api/public/payment-methods` | Public | Lấy danh sách các phương thức thanh toán đang mở |
| | `POST` | `/api/admin/payment-methods/upload-qr` | ADMIN | Tải lên ảnh mã QR thanh toán ngân hàng/MoMo |
| **Báo Cáo (Reports)** | `GET` | `/api/admin/reports/overview` | ADMIN | Báo cáo doanh thu và đơn hàng theo khoảng thời gian |
| | `GET` | `/api/admin/reports/expired-batches` | ADMIN | **Báo cáo tổn thất vốn do hàng quá hạn bị tiêu hủy** |
| **AI & Tin Nhắn Phản Hồi**| `POST` | `/api/public/chat` | Public | Gửi tin nhắn trò chuyện với Trợ lý ảo Google Gemini |
| | `POST` | `/api/messages/send` | Authenticated | Khách hàng gửi tin nhắn phản hồi tới ban quản trị |
| | `GET` | `/api/admin/messages/conversations` | ADMIN | Quản trị viên xem danh sách các cuộc hội thoại |
| | `POST` | `/api/admin/messages/conversations/{id}/reply` | ADMIN | Quản trị viên trả lời tin nhắn của khách hàng |

---

## ⚙️ Cấu Hình Môi Trường & Lưu Ý Quan Trọng

### 1. Tài Liệu Quy Trình Nghiệp Vụ Chuẩn Hóa
Toàn bộ chu trình vận hành của siêu thị từ bước mua sắm, duyệt đơn, điều phối shipper, nhập kho QC, xử lý lô cận date, đến tính toán tích điểm hội viên được mô tả chuẩn hóa dạng BPMN / Mermaid tại tài liệu:
👉 **[QUY_TRINH_NGHIEP_VU_CHI_TIET.md](file:///d:/NAM%20CUOI/CNJAVA/DO_AN_CONGNGHEJAVA/QUY_TRINH_NGHIEP_VU_CHI_TIET.md)** (Gồm 770+ dòng tài liệu chi tiết kèm sơ đồ tuần tự và lưu đồ giải thuật).

### 2. Cấu Hình Google Gemini AI
Hệ thống đã tích hợp sẵn khóa API Gemini mã hóa Base64 trong file [application.properties](file:///d:/NAM%20CUOI/CNJAVA/DO_AN_CONGNGHEJAVA/src/main/resources/application.properties):
```properties
gemini.api.key.base64=QVEuQWI4Uk42TEZMSWd1dnpYSTFGU1Radl83V3NBM2xXLTEwVTAxZW9qQXp0UGdKZVhLLXc=
```
Nếu bạn muốn sử dụng API Key riêng của bạn:
1. Đăng ký lấy API Key miễn phí tại [Google AI Studio](https://aistudio.google.com/).
2. Đặt biến môi trường hệ thống: `GEMINI_API_KEY=your_key_here` hoặc mã hóa Base64 và điền vào thuộc tính `gemini.api.key.base64`.

### 3. Cấu Hình Đăng Nhập Mạng Xã Hội Google OAuth2
Để kích hoạt tính năng "Đăng nhập bằng Google":
1. Truy cập [Google Cloud Console](https://console.cloud.google.com) ➔ Tạo OAuth 2.0 Client ID (Web Application).
2. Thêm Authorized redirect URI: `http://localhost:8080/login/oauth2/code/google`.
3. Điền Client ID và Client Secret vào [application.properties](file:///d:/NAM%20CUOI/CNJAVA/DO_AN_CONGNGHEJAVA/src/main/resources/application.properties):
   ```properties
   spring.security.oauth2.client.registration.google.client-id=YOUR_GOOGLE_CLIENT_ID
   spring.security.oauth2.client.registration.google.client-secret=YOUR_GOOGLE_CLIENT_SECRET
   ```

### 4. Tương Thích SQL Server & Chuẩn Hóa Tiếng Việt
* Ứng dụng đã cấu hình `spring.jpa.properties.hibernate.use_nationalized_character_data=true`, đảm bảo toàn bộ chuỗi ký tự được ánh xạ chuẩn sang kiểu dữ liệu `NVARCHAR` trong SQL Server, loại bỏ 100% lỗi font chữ dấu tiếng Việt.
* Trình điều khiển JDBC được cấu hình tương thích cả với các phiên bản SQL Server yêu cầu giao thức bảo mật mã hóa TLS 1.0/1.1/1.2.

---

## 🎯 Tổng Kết

Dự án **Website Quản Lý Siêu Thị Mini & Cửa Hàng Tiện Lợi (MiniMart)** là giải pháp công nghệ toàn diện kết hợp giữa khả năng xử lý nghiệp vụ mạnh mẽ, chặt chẽ của **Java Spring Boot 3**, giao diện hiện đại mượt mà của **React 18 SPA**, và sức mạnh phân tích thông minh từ thuật toán **Apriori** cùng **Google Gemini AI**. Dự án không chỉ đáp ứng hoàn hảo yêu cầu đồ án môn học chuyên ngành Công Nghệ Java (CNJAVA) mà còn sở hữu tính ứng dụng thực tiễn cao trong quản lý chuỗi bán lẻ siêu thị hiện đại.
