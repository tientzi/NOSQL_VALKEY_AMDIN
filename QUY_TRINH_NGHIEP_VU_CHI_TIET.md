# TỔNG HỢP TOÀN BỘ QUY TRÌNH NGHIỆP VỤ HỆ THỐNG SIÊU THỊ TIỆN LỢI MINIMART

**Dự án**: Siêu thị tiện lợi MiniMart (Hệ thống Bán hàng Đa kênh & Điều phối Giao hàng)  
**Công nghệ**: Backend Java Spring Boot 3 + Frontend React Vite  
**Cơ sở dữ liệu**: SQL Server / JPA Hibernate  
**Tài liệu chuẩn hóa**: Quy trình nghiệp vụ thuần túy (Business Process Management - BPM)  

---

## MỤC LỤC TỔNG QUAN

1. [PHẦN I: CÁC VAI TRÒ VÀ TÁC NHÂN TRONG HỆ THỐNG (SYSTEM ACTORS)](#phần-i-các-vai-trò-và-tác-nhân-trong-hệ-thống)
2. [PHẦN II: CÁC QUY TRÌNH NGHIỆP VỤ LỚN TOÀN TRÌNH (CORE END-TO-END WORKFLOWS)](#phần-ii-các-quy-trình-nghiệp-vụ-lớn-toàn-trình)
   - [2.1. Quy trình Mua hàng & Đặt hàng Toàn trình (Customer Shopping & Checkout)](#21-quy-trình-mua-hàng--đặt-hàng-toàn-trình)
   - [2.2. Quy trình Xử lý, Duyệt đơn & Điều phối Giao hàng Đa khu vực (Order Fulfillment & Dispatch)](#22-quy-trình-xử-lý-duyệt-đơn--điều-phối-giao-hàng-đa-khu-vực)
   - [2.3. Quy trình Nhập kho & Kiểm định Chất lượng Lô hàng Hạn dùng (Goods Receipt & QC Lifecycle)](#23-quy-trình-nhập-kho--kiểm-định-chất-lượng-lô-hàng-hạn-dùng)
   - [2.4. Quy trình Bán hàng Đa lô, Xả kho Cận Date & Xuất hủy Hàng hết hạn (Batch Clearance & Disposal)](#24-quy-trình-bán-hàng-đa-lô-xả-kho-cận-date--xuất-hủy-hàng-hết-hạn)
   - [2.5. Quy trình Khách hàng Thân thiết, Tích lũy Điểm & Thăng hạng Hội viên (Loyalty Tiering)](#25-quy-trình-khách-hàng-thân-thiết-tích-lũy-điểm--thăng-hạng-hội-viên)
3. [PHẦN III: CÁC QUY TRÌNH NGHIỆP VỤ THEO TỪNG PHÂN HỆ CHỨC NĂNG (SUB-PROCESSES BY MODULE)](#phần-iii-các-quy-trình-nghiệp-vụ-theo-từng-phân-hệ-chức-năng)
   - [3.1. Phân hệ Quản lý Thương hiệu (Brand Management)](#31-phân-hệ-quản-lý-thương-hiệu-brand-management)
   - [3.2. Phân hệ Quản lý Danh mục Ngành hàng (Category Management)](#32-phân-hệ-quản-lý-danh-mục-ngành-hàng-category-management)
   - [3.3. Phân hệ Quản lý Nhà cung cấp Đối tác (Supplier Management)](#33-phân-hệ-quản-lý-nhà-cung-cấp-đối-tác-supplier-management)
   - [3.4. Phân hệ Quản lý Mặt hàng & Thư viện Hình ảnh (Product Management)](#34-phân-hệ-quản-lý-mặt-hàng--thư-viện-hình-ảnh-product-management)
   - [3.5. Phân hệ Quản lý Tồn kho & Sổ cái Biến động Kho (Inventory & Stock Ledger)](#35-phân-hệ-quản-lý-tồn-kho--sổ-cái-biến-động-kho-inventory--ledger)
   - [3.6. Phân hệ Quản lý Mã Giảm Giá & Chiến dịch Khuyến mãi (Coupon Management)](#36-phân-hệ-quản-lý-mã-giảm-giá--chiến-dịch-khuyến-mãi-coupon-management)
   - [3.7. Phân hệ Quản lý Cấu hình & Đối soát Thanh toán (Payment Management)](#37-phân-hệ-quản-lý-cấu-hình--đối-soát-thanh-toán-payment-management)
   - [3.8. Phân hệ Quản lý Đánh giá & Phản hồi Khách hàng (Review Management)](#38-phân-hệ-quản-lý-đánh-giá--phản-hồi-khách-hàng-review-management)
   - [3.9. Phân hệ Hỗ trợ Trực tuyến & Trợ lý Ảo Thông minh (Support & AI Bot)](#39-phân-hệ-hỗ-trợ-trực-tuyến--trợ-lý-ảo-thông-minh-support--ai-bot)
   - [3.10. Phân hệ Quản lý Tài khoản & Phân quyền Người dùng (User & Security)](#310-phân-hệ-quản-lý-tài-khoản--phân-quyền-người-dùng-user--security)
   - [3.11. Phân hệ Báo cáo Thống kê & Phân tích Hoạt động Kinh doanh (Analytics & Reports)](#311-phân-hệ-báo-cáo-thống-kê--phân-tích-hoạt-động-kinh-doanh-analytics--reports)

---

## PHẦN I: CÁC VAI TRÒ VÀ TÁC NHÂN TRONG HỆ THỐNG

| Vai trò / Tác nhân | Ký hiệu phân quyền | Trách nhiệm và Quyền hạn chính |
| :--- | :---: | :--- |
| **Khách hàng** | `ROLE_USER` | Tìm kiếm, xem hàng hóa, chọn mua, áp dụng voucher khuyến mãi, thanh toán (tiền mặt COD hoặc chuyển khoản QR/MoMo), tra cứu hành trình đơn hàng, xem điểm tích lũy và hưởng đặc quyền hạng thành viên, đánh giá chất lượng sản phẩm, nhắn tin hỗ trợ. |
| **Nhân viên Giao hàng (Shipper)** | `ROLE_SHIPPER` | Nhận danh sách đơn hàng được điều phối tự động theo quận phụ trách (Tân Phú, Tân Bình, Quận 12...), cập nhật trạng thái đang giao, liên hệ khách hàng, giao hàng tận tay, thu tiền COD và cập nhật kết quả giao hàng. |
| **Quản trị viên / Quản lý Kho** | `ROLE_ADMIN` | Quản lý toàn bộ danh mục ngành hàng, nhãn hàng, nhà cung cấp, thông tin sản phẩm; lập phiếu nhập kho và kiểm định chất lượng hàng hóa; quản lý các lô hàng cận date và tiêu hủy hàng hết hạn; duyệt đơn hàng và điều phối giao hàng; cấu hình tài khoản thanh toán; duyệt đánh giá; xem báo cáo doanh thu và tổn thất kho. |

---

## PHẦN II: CÁC QUY TRÌNH NGHIỆP VỤ LỚN TOÀN TRÌNH

---

### 2.1. Quy trình Mua hàng & Đặt hàng Toàn trình (Customer Shopping & Checkout)

#### 1. Mô tả chi tiết quy trình:
1. **Tìm kiếm và Chọn hàng**: Khách hàng xem danh mục, tìm kiếm theo từ khóa hoặc xem các mặt hàng xả kho cận date giá ưu đãi.
2. **Thêm vào Giỏ hàng**: Khách chọn số lượng. Hệ thống kiểm tra số lượng tồn kho khả dụng; nếu còn đủ hàng, hệ thống cập nhật mặt hàng vào giỏ hàng của khách kèm thông tin lô sale (nếu có).
3. **Tiến hành Đặt hàng (Checkout)**: Khách mở trang thanh toán. Hệ thống tải sổ địa chỉ nhận hàng và danh sách các phương thức thanh toán đang hoạt động.
4. **Áp dụng Mã giảm giá (Coupon)**: Khách nhập mã voucher. Hệ thống kiểm tra 6 điều kiện (thời hạn, trạng thái kích hoạt, số lượt còn lại, giá trị đơn tối thiểu, ngành hàng áp dụng, giới hạn giảm tối đa) và tính ra số tiền được giảm trừ.
5. **Khấu trừ Đặc quyền Hạng Hội viên**: Hệ thống tự động kiểm tra hạng thành viên của khách để cộng thêm mức chiết khấu tri ân:
   * Hạng Bạc: Giảm thêm 2% tổng tiền hàng.
   * Hạng Vàng: Giảm thêm 5% tổng tiền hàng.
   * Hạng Kim Cương: Giảm thêm 8% tổng tiền hàng.
6. **Xác nhận Đặt hàng & Trừ kho Thông minh**:
   * Khách chọn hình thức thanh toán (Tiền mặt khi nhận COD, Chuyển khoản VietQR, hoặc Ví điện tử MoMo) và bấm "Đặt hàng".
   * Hệ thống tự động phân bổ trừ tồn kho theo nguyên tắc **Nhập trước - Xuất trước (FIFO)**: Ưu tiên trừ hết số lượng trong lô cận date giá rẻ trước; phần còn lại trừ tiếp vào lô tiêu chuẩn (nếu mua vượt số lượng sale thì tách thành 2 dòng rõ ràng trên đơn hàng).
   * Hệ thống trừ tổng tồn kho của sản phẩm, ghi sổ cái nhật ký kho loại "Xuất bán hàng", cập nhật lượt dùng mã giảm giá, khởi tạo đơn hàng ở trạng thái **"Chờ xác nhận"** và làm sạch giỏ hàng.
7. **Phản hồi Khách hàng**: Trả về màn hình xác nhận đặt hàng thành công với đầy đủ mã đơn hàng, bảng kê hàng hóa, số tiền thực trả và mã QR quét thanh toán (nếu chọn chuyển khoản).

#### 2. Sơ đồ Mermaid:
```mermaid
sequenceDiagram
    autonumber
    actor C as Khách hàng (User)
    participant UI as Giao diện Mua sắm (Website)
    participant CartMod as Phân hệ Giỏ hàng
    participant PromoMod as Phân hệ Khuyến mãi & Hội viên
    participant OrderMod as Phân hệ Quản lý Đơn hàng
    participant StockMod as Phân hệ Quản lý Kho & Lô HSD
    participant DB as Cơ sở dữ liệu Hệ thống

    C->>UI: Xem danh sách sản phẩm & Chọn mặt hàng muốn mua
    C->>UI: Nhập số lượng & Bấm "Thêm vào giỏ hàng"
    UI->>CartMod: Yêu cầu thêm sản phẩm vào giỏ
    CartMod->>StockMod: Kiểm tra số lượng tồn kho khả dụng thực tế
    StockMod-->>CartMod: Xác nhận còn đủ hàng trong kho
    CartMod->>DB: Lưu thông tin mặt hàng vào giỏ của khách
    CartMod-->>UI: Cập nhật giỏ hàng thành công (Số lượng, Tạm tính)

    C->>UI: Chuyển sang màn hình Đặt hàng (Checkout)
    UI->>UI: Tải Sổ địa chỉ người nhận & Cấu hình thanh toán
    
    opt Khách hàng nhập Mã giảm giá
        C->>UI: Nhập mã Voucher ưu đãi
        UI->>PromoMod: Kiểm tra điều kiện áp dụng mã khuyến mãi
        PromoMod->>DB: Kiểm tra hạn dùng, lượt dùng, đơn tối thiểu & danh mục
        PromoMod-->>UI: Trả về số tiền được giảm trừ từ Voucher
    end

    C->>UI: Chọn hình thức thanh toán (COD / Chuyển khoản QR / MoMo) & Bấm "Đặt hàng"
    UI->>OrderMod: Gửi thông tin đơn hàng đầy đủ (Người nhận, Địa chỉ, Phương thức, Ghi chú)

    critical Xử lý giao dịch Đặt hàng & Trừ kho tự động
        OrderMod->>PromoMod: Kiểm tra hạng hội viên của khách (Đồng / Bạc / Vàng / Kim Cương)
        PromoMod-->>OrderMod: Áp dụng thêm chiết khấu hạng thẻ (2% / 5% / 8%)
        OrderMod->>StockMod: Phân bổ trừ kho theo từng Lô hạn sử dụng (FIFO lô còn hạn)
        StockMod->>DB: Giảm số lượng tồn kho khả dụng của sản phẩm
        StockMod->>DB: Giảm số lượng tồn thực tế của Lô hàng tương ứng
        StockMod->>DB: Ghi sổ cái nhật ký biến động kho (Loại: Xuất bán hàng)
        opt Có áp dụng Voucher
            OrderMod->>DB: Tăng số lượt đã sử dụng của mã khuyến mãi
        end
        OrderMod->>DB: Khởi tạo Đơn hàng mới (Trạng thái: Chờ xác nhận)
        OrderMod->>DB: Khởi tạo Giao dịch thanh toán (Trạng thái: Chờ xử lý)
        OrderMod->>DB: Xóa sạch các mặt hàng trong giỏ của khách
    end

    OrderMod-->>UI: Trả về Phiếu xác nhận đơn hàng (Mã đơn, Danh sách món, Tổng thanh toán)
    alt Khách chọn Chuyển khoản VietQR hoặc MoMo
        UI->>UI: Hiển thị Mã QR động kèm số tiền chính xác và cú pháp chuyển khoản
    else Khách chọn Tiền mặt COD
        UI-->>C: Hiển thị thông báo đặt hàng thành công & Chuyển sang Lịch sử đơn hàng
    end
```

---

### 2.2. Quy trình Xử lý, Duyệt đơn & Điều phối Giao hàng Đa khu vực (Order Fulfillment & Dispatch)

#### 1. Mô tả chi tiết quy trình:
1. **Tiếp nhận & Kiểm tra Đơn**: Đơn hàng mới vào hệ thống ở trạng thái **"Chờ xác nhận"**. Quản trị viên mở bảng quản lý đơn hàng để kiểm tra tính chính xác của địa chỉ và hàng hóa.
2. **Xử lý Duyệt đơn hoặc Hủy đơn**:
   * *Nếu hủy đơn*: Đơn chuyển sang **"Đã hủy"**, hệ thống tự động hoàn trả số lượng hàng về đúng lô ban đầu, hoàn trả tồn kho, ghi sổ cái kho loại "Hoàn trả hàng hủy", hoàn trả lượt dùng voucher và hủy giao dịch thanh toán.
   * *Nếu duyệt đơn*: Đơn chuyển sang **"Đã xác nhận"**. Hệ thống tự động xác nhận thanh toán: nếu là đơn COD thì đổi thành "Đã duyệt COD", nếu là chuyển khoản đã nhận được tiền thì đổi thành "Đã thanh toán".
3. **Điều phối Giao hàng theo Địa bàn**:
   * *Cơ chế tự động theo khu vực*: Hệ thống phân tích chuỗi địa chỉ giao hàng của khách để nhận diện quận huyện:
     * Địa chỉ thuộc **Quận Tân Phú** $\rightarrow$ Tự động phân bổ cho Shipper số 1.
     * Địa chỉ thuộc **Quận Tân Bình** $\rightarrow$ Tự động phân bổ cho Shipper số 2.
     * Địa chỉ thuộc **Quận 12** $\rightarrow$ Tự động phân bổ cho Shipper số 3.
   * *Cơ chế phân bổ thủ công*: Quản trị viên có thể chọn từng đơn hoặc chọn hàng loạt đơn chưa gán để giao cho một shipper tùy ý.
   * Đơn hàng sau khi phân bổ chuyển sang trạng thái **"Đã nhận đơn"**.
4. **Quy trình Giao nhận của Shipper**:
   * Shipper mở ứng dụng, xem danh sách đơn hàng được giao, thông tin người nhận, lộ trình và số tiền cần thu.
   * Shipper bấm **"Bắt đầu giao"** $\rightarrow$ Đơn hàng đổi sang trạng thái **"Đang giao hàng"**.
   * Đến địa chỉ, shipper liên hệ người nhận và giao hàng thực tế.
5. **Cập nhật Kết quả Giao hàng**:
   * *Trường hợp giao thất bại* (khách không nghe máy, từ chối nhận): Shipper nhập lý do thất bại và xác nhận $\rightarrow$ Đơn chuyển sang trạng thái **"Giao thất bại"** để quản trị viên liên hệ lại hoặc điều phối lại cho shipper khác (`reassign`).
   * *Trường hợp giao thành công*: Shipper nhận tiền mặt (nếu đơn COD) và bấm "Giao thành công" $\rightarrow$ Đơn chuyển sang trạng thái **"Đã giao hàng"**, giao dịch thanh toán COD chuyển thành "Đã thanh toán".
6. **Hoàn tất Đơn hàng & Tích điểm Thăng hạng**: Quản trị viên kiểm tra và bấm "Hoàn thành đơn" $\rightarrow$ Đơn chuyển sang **"Hoàn thành"**, hệ thống tự động tính điểm tích lũy cho khách hàng (1 điểm / 100.000đ) và tự động nâng hạng thẻ nếu đủ điều kiện.

#### 2. Sơ đồ Mermaid:
```mermaid
flowchart TD
    Start(["Đơn hàng mới tạo<br>Trạng thái: Chờ xác nhận"]) --> AdminCheck{"Quản trị viên kiểm tra<br>thông tin đơn hàng"}
    
    AdminCheck -- "Thông tin sai / Hết hàng" --> AdminCancel["Hủy đơn hàng (Đã hủy)"]
    AdminCancel --> RestoreStock["Tự động hoàn trả tồn kho sản phẩm & Lô hàng<br>Phục hồi lượt sử dụng Mã giảm giá<br>Hủy bỏ giao dịch thanh toán"]
    RestoreStock --> EndCancel(["Kết thúc: Đơn hàng bị hủy"])

    AdminCheck -- "Thông tin hợp lệ" --> AdminApprove["Duyệt đơn hàng<br>Trạng thái: Đã xác nhận"]
    AdminApprove --> PayStatusCheck{"Hình thức thanh toán?"}
    PayStatusCheck -- "Tiền mặt COD" --> SetCodApproved["Trạng thái thanh toán: Đã duyệt COD<br>(Sẵn sàng thu tiền khi giao)"]
    PayStatusCheck -- "Chuyển khoản QR / MoMo" --> SetQrPaid["Trạng thái thanh toán: Đã thanh toán<br>(Xác nhận đã nhận tiền vào tài khoản)"]

    SetCodApproved --> DispatchStep["Phân bổ Shipper giao hàng<br>(Trang Quản lý Vận chuyển)"]
    SetQrPaid --> DispatchStep

    DispatchStep --> AutoScan{"Lựa chọn phương thức phân bổ"}
    AutoScan -- "Tự động theo địa bàn quận huyện" --> AutoArea["Hệ thống nhận diện địa chỉ giao hàng:<br>• Quận Tân Phú -> Gán cho Shipper 1<br>• Quận Tân Bình -> Gán cho Shipper 2<br>• Quận 12 -> Gán cho Shipper 3"]
    AutoScan -- "Phân công thủ công" --> ManualPick["Quản trị viên chọn Shipper khả dụng từ danh sách"]

    AutoArea --> Assigned["Gán Shipper thành công<br>Trạng thái: Đã nhận đơn"]
    ManualPick --> Assigned

    Assigned --> ShipperApp["Shipper mở Bảng điều khiển giao hàng<br>Xem danh sách đơn, SĐT, Địa chỉ, Số tiền COD"]
    ShipperApp --> StartDelivery["Shipper nhận hàng & bấm 'Bắt đầu giao'<br>Trạng thái: Đang giao hàng"]
    StartDelivery --> DeliveryOutcome{"Kết quả giao hàng thực tế tại nhà khách"}

    DeliveryOutcome -- "Khách không nghe máy / Từ chối nhận" --> DeliveryFail["Shipper bấm 'Báo giao thất bại'<br>Trạng thái: Giao thất bại<br>Lưu lý do thất bại vào hệ thống"]
    DeliveryFail --> AdminReassign["Quản trị viên liên hệ lại khách hàng<br>-> Điều phối lại Shipper khác hoặc Hủy đơn"]

    DeliveryOutcome -- "Khách nhận hàng đầy đủ" --> DeliverySuccess["Shipper xác nhận 'Giao thành công'<br>Trạng thái: Đã giao hàng<br>Thanh toán COD đổi sang: Đã thanh toán"]
    DeliverySuccess --> AdminComplete["Quản trị viên xác nhận 'Hoàn thành đơn'<br>Trạng thái: Hoàn thành"]
    AdminComplete --> RewardPoints["Hệ thống tự động cộng Điểm tích lũy cho Khách<br>(1 điểm cho mỗi 100.000 VNĐ)<br>Tự động cập nhật Hạng Hội Viên mới"]
    RewardPoints --> EndSuccess(["Kết thúc: Đơn hàng hoàn tất trọn vẹn"])
```

---

### 2.3. Quy trình Nhập kho & Kiểm định Chất lượng Lô hàng Hạn dùng (Goods Receipt & QC Lifecycle)

#### 1. Mô tả chi tiết quy trình:
1. **Lập Phiếu Nhập Hàng**: Quản lý kho chọn Nhà cung cấp đối tác, Thương hiệu và danh sách mặt hàng cần nhập. Với mỗi mặt hàng, nhập số lượng dự kiến nhập, đơn giá nhập vốn, mã số lô sản xuất và hạn sử dụng (HSD). Phiếu được lưu ở trạng thái **"Bản nháp (DRAFT)"**.
2. **Kiểm tra Thực tế tại Cửa kho**: Khi xe tải của nhà cung cấp giao hàng đến, nhân sự kho kiểm đếm thực tế. Hệ thống cung cấp 2 phương thức xử lý:
   * **Phương thức 1 - Hoàn thành nhanh (100% đạt chuẩn)**: Áp dụng khi hàng hóa nguyên đai nguyên kiện, không có hư hao. Toàn bộ số lượng nhập được ghi nhận đạt chuẩn.
   * **Phương thức 2 - Kiểm định Chất lượng (QC) Chuyên sâu**: Áp dụng khi cần kiểm tra tỉ mỉ hàng tươi sống, bánh kẹo, đồ hộp. Nhân viên nhập rõ số lượng đạt chuẩn (`passedQuantity`) và số lượng lỗi/hư hỏng/móp méo/cận date bị từ chối trả về nhà cung cấp (`rejectedQuantity`) kèm lý do cụ thể. Hệ thống kiểm tra điều kiện bắt buộc: `Số lượng đạt + Số lượng từ chối = Tổng số lượng nhập ban đầu`.
3. **Cập nhật Kho & Sinh Lô Hàng Tự Động**:
   * Phiếu nhập chuyển sang trạng thái **"Hoàn thành (COMPLETED)"**.
   * **Nguyên tắc an toàn dữ liệu**: Hệ thống **chỉ tăng số lượng tồn kho khả dụng đối với phần số lượng ĐẠT CHUẨN**.
   * Tự động khởi tạo bản ghi **Lô hàng mới** với đúng số lượng đạt chuẩn, gắn mã số lô, giá vốn nhập và hạn sử dụng để phục vụ bán lẻ.
   * Ghi nhận vào **Sổ cái nhật ký kho** loại "Nhập kho mua hàng" (đối với phần đạt chuẩn) và ghi nhận lý do trả hàng (đối với phần từ chối) để đối soát công nợ với nhà cung cấp.

#### 2. Sơ đồ Mermaid:
```mermaid
sequenceDiagram
    autonumber
    actor W as Quản lý kho / Thủ kho
    participant UI as Giao diện Quản lý Kho
    participant ReceiptMod as Phân hệ Nhập kho
    participant QCMod as Phân hệ Kiểm định Chất lượng
    participant BatchMod as Phân hệ Quản lý Lô HSD
    participant StockMod as Phân hệ Quản lý Tồn kho
    participant DB as Cơ sở dữ liệu Hệ thống

    W->>UI: Chọn Nhà cung cấp, Thương hiệu & Sản phẩm nhập
    W->>UI: Nhập Số lượng, Đơn giá vốn, Mã số lô sản xuất & Hạn sử dụng
    UI->>ReceiptMod: Gửi thông tin lập Phiếu nhập hàng mới
    ReceiptMod->>DB: Khởi tạo Phiếu nhập hàng (Trạng thái: Bản nháp)
    ReceiptMod-->>UI: Xác nhận tạo phiếu thành công (Mã phiếu nhập, Tổng tiền vốn tạm tính)

    W->>UI: Tiến hành kiểm tra hàng thực tế tại cửa kho
    alt Lựa chọn 1: Hoàn tất nhanh (Toàn bộ hàng hóa đạt chuẩn 100%)
        W->>UI: Bấm nút "Duyệt nhập kho nhanh"
        UI->>ReceiptMod: Yêu cầu hoàn thành toàn bộ phiếu nhập
        ReceiptMod->>ReceiptMod: Mặc định: Số lượng đạt = Tổng số lượng nhập, Số lượng lỗi = 0
    else Lựa chọn 2: Kiểm định Chất lượng (QC) chi tiết từng mặt hàng
        W->>UI: Bấm nút "Kiểm định chất lượng (QC)"
        W->>UI: Nhập Số lượng đạt chuẩn, Số lượng lỗi/hỏng, Lý do từ chối & Ghi chú kiểm định
        UI->>QCMod: Gửi biên bản kiểm định chất lượng (QC)
        QCMod->>QCMod: Kiểm tra hợp lệ: (SL Đạt + SL Lỗi == Tổng SL nhập ban đầu)
    end

    critical Cập nhật Tồn kho khả dụng, Khởi tạo Lô hàng & Ghi sổ cái
        ReceiptMod->>DB: Chuyển trạng thái Phiếu nhập sang "Hoàn thành"
        ReceiptMod->>StockMod: Tăng tồn kho của sản phẩm bằng đúng SỐ LƯỢNG ĐẠT CHUẨN
        ReceiptMod->>BatchMod: Tự động khởi tạo Lô hàng mới (Số lô, HSD, SL đạt chuẩn, Giá vốn nhập)
        ReceiptMod->>StockMod: Ghi sổ cái nhật ký kho (Loại: Nhập kho, Ghi rõ SL đạt chuẩn & SL trả về NCC)
    end

    ReceiptMod-->>UI: Thông báo hoàn tất quy trình nhập kho thành công
    UI-->>W: Hiển thị tồn kho cập nhật mới & Lô hàng đã sẵn sàng mở bán trên hệ thống
```

---

### 2.4. Quy trình Bán hàng Đa lô, Xả kho Cận Date & Xuất hủy Hàng hết hạn (Batch Clearance & Disposal)

#### 1. Mô tả chi tiết quy trình:
1. **Hệ thống Quét Hạn Sử Dụng Hàng Ngày**:
   * Hệ thống tự động so sánh ngày hết hạn của từng lô hàng với ngày hiện tại để phân loại thành 3 nhóm trạng thái:
     * **Lô hàng tiêu chuẩn**: Hạn sử dụng còn xa (> 30 ngày) $\rightarrow$ Bán với giá niêm yết chuẩn.
     * **Lô hàng cận date**: Hạn sử dụng còn trong vòng 30 ngày trở lại $\rightarrow$ Cảnh báo quản lý kho.
     * **Lô hàng đã hết hạn**: Hạn sử dụng nhỏ hơn ngày hôm nay $\rightarrow$ Tự động chuyển sang trạng thái hết hạn (`EXPIRED`).
2. **Nghiệp vụ Xả kho Cận Date (Clearance Sale)**:
   * Với các lô cận hạn, quản lý kho có thể kích hoạt chương trình "Xả kho cận date" bằng cách nhập phần trăm giảm giá (ví dụ: giảm 30%) hoặc nhập trực tiếp giá xả kho mong muốn (`salePrice < price`).
   * Hệ thống cập nhật giá sale riêng cho lô đó và hiển thị nổi bật trên website kèm hạn sử dụng cụ thể để khách hàng nắm rõ trước khi mua.
3. **Thuật toán Xuất bán Đa lô Thông minh (FIFO)**:
   * Khi khách hàng đặt mua mặt hàng có lô cận date:
     * Nếu khách mua **nhỏ hơn hoặc bằng** số lượng tồn của lô cận date: Toàn bộ mặt hàng được tính theo giá xả kho ưu đãi.
     * Nếu khách mua **vượt quá** số lượng tồn của lô cận date: Hệ thống tự động tách thành 2 dòng mặt hàng trên đơn hàng: dòng 1 là toàn bộ số lượng lô sale với giá ưu đãi, dòng 2 là phần số lượng còn thiếu lấy từ lô tiêu chuẩn với giá niêm yết chuẩn.
4. **Nghiệp vụ Khóa & Xuất hủy Hàng Hết Hạn**:
   * Đối với các lô đã quá hạn sử dụng: Hệ thống **khóa cứng**, cấm không cho thiết lập giảm giá và không bao giờ xuất bán cho khách hàng.
   * Các lô hết hạn được chuyển sang tab quản lý "Lô hàng hết hạn", hệ thống tự động tính toán tổng số tiền vốn bị thiệt hại (`Số lượng tồn x Giá vốn nhập`).
   * Quản lý kho thực hiện thao tác **"Xuất hủy kho"**: Số lượng tồn của lô đưa về 0, trạng thái lô chuyển sang "Đã xuất hủy (`DISPOSED`)", trừ tồn kho khả dụng của sản phẩm, ghi sổ cái kho loại "Xuất hủy hàng hết hạn" và tổng hợp số liệu vào Báo cáo tổn thất hàng hủy.

#### 2. Sơ đồ Mermaid:
```mermaid
flowchart TD
    ScanBatch["Hệ thống quét hạn sử dụng của từng Lô hàng"] --> CheckDate{"Kiểm tra Hạn sử dụng (HSD) so với Ngày hiện tại"}

    CheckDate -- "Còn hạn xa (> 30 ngày)" --> NormalBatch["Lô hàng tiêu chuẩn<br>Bán lẻ theo Giá niêm yết gốc"]
    
    CheckDate -- "Cận date (<= 30 ngày)" --> NearExpire["Lô hàng CẬN HẠN SỬ DỤNG"]
    NearExpire --> ActionSale{"Quyết định của Quản lý kho"}
    ActionSale -- "Giảm giá thanh lý nhanh" --> SetSale["Thiết lập chương trình 'Xả kho cận date'<br>Nhập phần trăm giảm hoặc giá bán sale ưu đãi riêng"]
    SetSale --> DisplayWeb["Hiển thị huy hiệu 'Xả kho cận date'<br>trên Website kèm HSD rõ ràng cho khách"]
    DisplayWeb --> CustomerBuy["Khách hàng chọn mua sản phẩm"]
    CustomerBuy --> FifoAllocation{"Số lượng khách mua so với tồn kho của Lô sale?"}
    FifoAllocation -- "Mua trong giới hạn tồn Lô sale" --> AllSale["Tính toàn bộ theo Giá ưu đãi của Lô cận date"]
    FifoAllocation -- "Mua vượt quá tồn Lô sale" --> SplitRows["Tự động tách thành 2 dòng mặt hàng trên đơn:<br>1. Toàn bộ Lô cận date tính Giá ưu đãi<br>2. Phần vượt quá lấy từ Lô chuẩn tính Giá niêm yết"]

    CheckDate -- "Đã quá hạn sử dụng" --> ExpiredBatch["Lô hàng ĐÃ HẾT HẠN DÙNG"]
    ExpiredBatch --> BlockSale["Hệ thống khóa chặt: Tuyệt đối không cho bán lẻ<br>& Vô hiệu hóa nút thiết lập Giảm giá"]
    BlockSale --> MoveExpiredTab["Chuyển sang danh sách 'Lô hàng hết hạn'<br>Tự động tính tổn thất vốn = Số lượng tồn x Giá vốn nhập"]
    MoveExpiredTab --> DisposeAction["Quản lý kho bấm nút 'Xuất hủy kho'"]
    DisposeAction --> UpdateDisposed["Đưa tồn của Lô về 0 & Chuyển trạng thái 'Đã xuất hủy'<br>Giảm tồn kho khả dụng của sản phẩm tương ứng<br>Ghi sổ cái nhật ký kho loại: Xuất hủy hàng hết hạn"]
    UpdateDisposed --> ReportLoss["Cập nhật số liệu vào Báo cáo Thất thoát Hàng hủy"]
```

---

### 2.5. Quy trình Khách hàng Thân thiết, Tích lũy Điểm & Thăng hạng Hội viên (Loyalty Tiering)

#### 1. Mô tả chi tiết quy trình:
1. **Khởi tạo Tài khoản**: Khách hàng đăng ký tài khoản thành công mặc định bắt đầu ở hạng **Đồng (Bronze)** với 0 điểm tích lũy.
2. **Cơ chế Tích lũy Điểm**:
   * Khi khách mua hàng và đơn hàng được hoàn tất thành công (shipper giao hàng và thanh toán đầy đủ), hệ thống tự động quy đổi: **Mỗi 100.000 VNĐ thanh toán thực tế = Tích lũy 1 điểm**.
   * Điểm thưởng được cộng dồn lũy kế vào hồ sơ khách hàng.
3. **Tự động Thăng hạng & Đặc quyền tương ứng**:
   * **Hạng Đồng (`BRONZE` - Dưới 100 điểm)**: Quyền lợi cơ bản, tích lũy điểm thưởng theo đơn hàng.
   * **Hạng Bạc (`SILVER` - Từ 100 đến 499 điểm)**: Tự động chiết khấu 2% trực tiếp trên mọi đơn hàng tiếp theo; nhận voucher sinh nhật.
   * **Hạng Vàng (`GOLD` - Từ 500 đến 999 điểm)**: Tự động chiết khấu 5% trực tiếp trên mọi đơn hàng + Giảm 30% phí vận chuyển.
   * **Hạng Kim Cương (`DIAMOND` - Từ 1.000 điểm trở lên)**: Tự động chiết khấu tối đa 8% trực tiếp trên mọi đơn hàng + Miễn phí vận chuyển 100% không giới hạn.
4. **Theo dõi Trực quan trên Trang Cá nhân**: Khách hàng mở trang "Khách Hàng Thân Thiết" (`/loyalty`) để xem thẻ thành viên điện tử sang trọng, điểm khả dụng hiện có, thanh tiến trình % thăng hạng và kho mã giảm giá công khai đang phát hành.

#### 2. Sơ đồ Mermaid:
```mermaid
stateDiagram-v2
    [*] --> BRONZE: Khách hàng đăng ký tài khoản mới (0 điểm)
    
    BRONZE --> SILVER: Tích lũy đủ >= 100 điểm
    note right of BRONZE
        Đặc quyền Hạng Đồng:
        • Tích lũy 1 điểm cho mỗi 100.000đ
    end note

    SILVER --> GOLD: Tích lũy đủ >= 500 điểm
    note right of SILVER
        Đặc quyền Hạng Bạc:
        • Tự động chiết khấu 2% giá trị đơn hàng
        • Tặng Voucher quà tặng sinh nhật
    end note

    GOLD --> DIAMOND: Tích lũy đủ >= 1.000 điểm
    note right of GOLD
        Đặc quyền Hạng Vàng:
        • Tự động chiết khấu 5% giá trị đơn hàng
        • Giảm 30% phí vận chuyển
    end note

    note right of DIAMOND
        Đặc quyền Hạng Kim Cương VIP:
        • Tự động chiết khấu cao nhất 8% giá trị đơn hàng
        • Miễn phí vận chuyển 100% không giới hạn
        • Ưu tiên chăm sóc khách hàng VIP
    end note

    DIAMOND --> DIAMOND: Duy trì đặc quyền cao nhất của Siêu thị
```

---

## PHẦN III: CÁC QUY TRÌNH NGHIỆP VỤ THEO TỪNG PHÂN HỆ CHỨC NĂNG

---

### 3.1. Phân hệ Quản lý Thương hiệu (Brand Management)

#### Mục đích:
Quản lý các nhãn hiệu, thương hiệu sản phẩm đối tác kinh doanh tại siêu thị (Vinamilk, TH True Milk, Coca-Cola, Pepsi, Acecook, Chinsu...).

#### Các quy trình con:
1. **Thêm mới thương hiệu**: Quản trị viên nhập Tên thương hiệu, Mô tả giới thiệu, Ảnh logo. Hệ thống kiểm tra tên không được trùng lặp. Mặc định kích hoạt trạng thái hoạt động.
2. **Chỉnh sửa thương hiệu**: Cập nhật thông tin mô tả, thay đổi ảnh logo hoặc đổi tên thương hiệu (kiểm tra không trùng với các thương hiệu khác).
3. **Bật/Tắt trạng thái hoạt động**: Tạm thời ẩn các thương hiệu tạm ngưng kinh doanh khỏi menu lọc của khách hàng.
4. **Xóa thương hiệu**: Kiểm tra ràng buộc toàn vẹn dữ liệu; nếu thương hiệu đã có sản phẩm thuộc về thì ngăn chặn xóa để bảo vệ lịch sử bán hàng.

#### Sơ đồ Mermaid:
```mermaid
flowchart TD
    StartBrand([Quản trị viên vào Quản lý Thương hiệu]) --> ActionChoice{"Lựa chọn thao tác"}
    
    ActionChoice -- "Thêm thương hiệu mới" --> InputBrand["Nhập Tên thương hiệu, Mô tả, Tải ảnh Logo"]
    InputBrand --> CheckExist{"Tên thương hiệu<br>đã có trong hệ thống?"}
    CheckExist -- "Đã tồn tại" --> ShowError["Thông báo lỗi: Tên thương hiệu đã được sử dụng"]
    CheckExist -- "Chưa có" --> SaveBrand["Lưu thương hiệu mới vào cơ sở dữ liệu<br>(Trạng thái: Đang hoạt động)"]
    SaveBrand --> RefreshBrand["Cập nhật danh sách hiển thị"]

    ActionChoice -- "Chỉnh sửa thông tin" --> EditBrand["Mở biểu mẫu chỉnh sửa thương hiệu"]
    EditBrand --> CheckUnique{"Tên mới có bị trùng<br>với thương hiệu khác?"}
    CheckUnique -- "Trùng lặp" --> ShowError
    CheckUnique -- "Hợp lệ" --> UpdateBrand["Lưu các thông tin thay đổi vào hệ thống"]
    UpdateBrand --> RefreshBrand

    ActionChoice -- "Bật / Tắt trạng thái" --> ToggleActive["Đổi trạng thái: Đang hoạt động <-> Tạm ẩn"]
    ToggleActive --> RefreshBrand

    ActionChoice -- "Xóa thương hiệu" --> CheckLinkedProd{"Có sản phẩm nào<br>đang gắn với thương hiệu này?"}
    CheckLinkedProd -- "Còn sản phẩm liên kết" --> BlockDelete["Cảnh báo: Không thể xóa thương hiệu đang có sản phẩm kinh doanh"]
    CheckLinkedProd -- "Không có sản phẩm" --> ConfirmDelete["Xóa hoàn toàn thương hiệu khỏi hệ thống"]
    ConfirmDelete --> RefreshBrand
```

---

### 3.2. Phân hệ Quản lý Danh mục Ngành hàng (Category Management)

#### Mục đích:
Tổ chức cây phân loại mặt hàng trong siêu thị (Rau củ quả, Thịt cá tươi sống, Bánh kẹo & Đồ ăn vặt, Nước giải khát, Hóa mỹ phẩm, Gia vị...).

#### Các quy trình con:
1. **Thêm danh mục mới**: Nhập tên danh mục, mô tả tóm tắt, ảnh biểu tượng đại diện (Icon). Kiểm tra không trùng tên.
2. **Cập nhật danh mục**: Cho phép thay đổi tên hiển thị, ảnh biểu tượng và trạng thái hoạt động.
3. **Phân loại sản phẩm**: Phục vụ việc khách hàng duyệt lọc sản phẩm theo ngành hàng trên website và làm điều kiện giới hạn phạm vi áp dụng mã giảm giá.

#### Sơ đồ Mermaid:
```mermaid
flowchart TD
    StartCat([Quản trị viên mở Quản lý Danh mục]) --> CatAction{"Chọn thao tác"}
    CatAction -- "Tạo danh mục mới" --> EnterCat["Nhập Tên danh mục, Mô tả, Tải ảnh Icon"]
    EnterCat --> CheckCatName{"Tên danh mục đã có?"}
    CheckCatName -- "Đã có" --> DuplicateAlert["Thông báo: Tên danh mục đã tồn tại"]
    CheckCatName -- "Chưa có" --> SaveCat["Lưu danh mục mới vào hệ thống"]
    SaveCat --> DoneCat(["Hoàn tất thao tác"])

    CatAction -- "Chỉnh sửa" --> EditCat["Sửa Tên danh mục / Icon / Trạng thái"]
    EditCat --> CheckConflict{"Tên trùng với danh mục khác?"}
    CheckConflict -- "Trùng" --> DuplicateAlert
    CheckConflict -- "Không trùng" --> UpdateCat["Lưu thông tin cập nhật vào hệ thống"]
    UpdateCat --> DoneCat

    CatAction -- "Khách hàng duyệt danh mục" --> ClientBrowse["Khách chọn danh mục trên thanh điều hướng"]
    ClientBrowse --> QueryProds["Hệ thống lọc toàn bộ sản phẩm đang bán thuộc danh mục"]
    QueryProds --> RenderGrid["Hiển thị danh sách hàng hóa cho khách mua sắm"]
```

---

### 3.3. Phân hệ Quản lý Nhà cung cấp Đối tác (Supplier Management)

#### Mục đích:
Quản lý danh sách các tổng kho, công ty phân phối hàng sỉ cho siêu thị, thông tin liên lạc và theo dõi lịch sử cung ứng mặt hàng.

#### Các quy trình con:
1. **Thêm nhà cung cấp mới**: Nhập tên doanh nghiệp, người đại diện liên hệ, số điện thoại hotline, email công vụ, địa chỉ kho nhận hàng.
2. **Cập nhật hồ sơ đối tác**: Điều chỉnh số điện thoại, đổi địa chỉ kho xuất hàng hoặc người đại diện.
3. **Theo dõi danh mục hàng cung ứng**: Xem danh sách toàn bộ các sản phẩm đã từng nhập từ nhà cung cấp này, tổng số lượng đã cung cấp và ngày nhập hàng gần nhất.
4. **Bảo vệ dữ liệu lịch sử khi xóa**: Nếu nhà cung cấp đã từng có phiếu nhập kho phát sinh trong quá khứ, hệ thống ngăn cấm xóa vĩnh viễn, hướng dẫn chuyển trạng thái sang "Ngừng hợp tác" để bảo toàn chứng từ kế toán.

#### Sơ đồ Mermaid:
```mermaid
flowchart TD
    StartSup([Quản trị viên mở Quản lý Nhà cung cấp]) --> SupChoice{"Chọn thao tác"}
    
    SupChoice -- "Thêm đối tác cung ứng mới" --> InputSup["Nhập Tên công ty, Người liên hệ, SĐT, Email, Địa chỉ"]
    InputSup --> SaveSup["Lưu thông tin Nhà cung cấp vào hệ thống"]
    SaveSup --> SupDone(["Thành công"])

    SupChoice -- "Cập nhật hồ sơ" --> EditSup["Sửa thông tin liên hệ, SĐT hotline, Địa chỉ kho"]
    EditSup --> UpdateSup["Lưu thay đổi vào hệ thống"]
    UpdateSup --> SupDone

    SupChoice -- "Xem danh mục hàng cung ứng" --> ViewProdHistory["Hệ thống tổng hợp toàn bộ sản phẩm đã nhập:<br>• Mã sản phẩm & Tên hàng hóa<br>• Tổng số lượng đã cung cấp lũy kế<br>• Lần nhập kho gần nhất"]
    ViewProdHistory --> SupDone

    SupChoice -- "Xóa nhà cung cấp" --> CheckHasReceipt{"Nhà cung cấp đã có<br>Phiếu nhập kho nào chưa?"}
    CheckHasReceipt -- "Đã có chứng từ nhập kho" --> PreventDelete["Ngăn chặn xóa: Bắt buộc giữ lại lịch sử kế toán!<br>Hướng dẫn chuyển sang trạng thái 'Ngừng hợp tác'"]
    CheckHasReceipt -- "Chưa có chứng từ" --> ConfirmDelete["Xóa hoàn toàn Nhà cung cấp khỏi hệ thống"]
    ConfirmDelete --> SupDone
```

---

### 3.4. Phân hệ Quản lý Mặt hàng & Thư viện Hình ảnh (Product Management)

#### Mục đích:
Quản lý toàn bộ thông tin thương phẩm trưng bày và mở bán: Tên sản phẩm, Mã vạch Barcode, Mã quản lý SKU, Đơn vị tính (Chai, Lon, Gói, Hộp, Kg), Trọng lượng, Giá bán niêm yết, Giá bán ưu đãi chung, Danh mục, Thương hiệu và Thư viện ảnh chi tiết.

#### Các quy trình con:
1. **Tạo mới sản phẩm**:
   * Quản trị viên nhập thông tin chi tiết, chọn Danh mục và Thương hiệu liên kết, tải ảnh đại diện chính và các ảnh chụp góc cạnh.
   * **Khóa nhập tay số lượng tồn kho**: Hệ thống tự động khởi tạo bản ghi tồn kho với số lượng mặc định bằng 0. Số lượng tồn kho chỉ được phép gia tăng thông qua Phiếu Nhập Kho có kiểm định QC.
2. **Chỉnh sửa sản phẩm**: Điều chỉnh giá bán, sửa đổi mô tả chi tiết, bổ sung hoặc xóa ảnh thư viện. Hệ thống cấm chỉnh sửa số lượng tồn kho trực tiếp từ biểu mẫu này.
3. **Kích hoạt / Tạm ngừng kinh doanh**: Ẩn hoặc hiển thị lại sản phẩm trên website bán hàng.

#### Sơ đồ Mermaid:
```mermaid
flowchart TD
    StartProd([Quản trị viên mở Quản lý Sản phẩm]) --> ProdMode{"Lựa chọn"}
    
    ProdMode -- "Thêm sản phẩm mới" --> FormProd["Nhập Tên hàng, Mã vạch Barcode, SKU, Đơn vị tính, Giá niêm yết"]
    FormProd --> SelectRef["Gắn Danh mục ngành hàng & Thương hiệu sản xuất"]
    SelectRef --> UploadImgs["Tải lên Ảnh đại diện chính & Bộ sưu tập ảnh chi tiết"]
    UploadImgs --> SaveProduct["Lưu thông tin sản phẩm vào hệ thống"]
    SaveProduct --> AutoInitInv["Tự động khởi tạo Tồn kho ban đầu = 0<br>(Quy tắc chuẩn: Không cho nhập tay số lượng tồn kho)"]
    AutoInitInv --> ProdSuccess(["Tạo sản phẩm mới thành công"])

    ProdMode -- "Chỉnh sửa thông tin" --> OpenEdit["Mở biểu mẫu cập nhật sản phẩm"]
    OpenEdit --> ChangeFields["Sửa Giá bán niêm yết, Mô tả, Thay ảnh, Vị trí kệ trưng bày"]
    ChangeFields --> SaveUpdate["Lưu thông tin cập nhật vào hệ thống"]
    SaveUpdate --> ProdSuccess

    ProdMode -- "Khóa / Mở bán" --> ToggleProd["Đổi trạng thái kinh doanh: Đang mở bán <-> Tạm ngừng kinh doanh"]
    ToggleProd --> ProdSuccess
```

---

### 3.5. Phân hệ Quản lý Tồn kho & Sổ cái Biến động Kho (Inventory & Ledger)

#### Mục đích:
Theo dõi chính xác số lượng tồn kho của từng sản phẩm tại từng vị trí kệ hàng, cảnh báo hàng sắp hết và ghi vết toàn bộ lịch sử biến động kho phục vụ kiểm toán nội bộ.

#### Các quy trình con:
1. **Kiểm soát Tồn kho & Cảnh báo Tồn thấp**:
   * Hệ thống hiển thị danh sách tồn kho thực tế của tất cả mặt hàng.
   * Khi số lượng tồn thực tế nhỏ hơn hoặc bằng Ngưỡng an toàn tối thiểu (`minimumStock`), hệ thống tự động đưa vào danh mục "Cảnh báo tồn kho thấp" để thủ kho kịp thời lên kế hoạch nhập hàng.
   * Cập nhật vị trí kệ hàng (`location`) và điều chỉnh ngưỡng an toàn tối thiểu.
2. **Chặn Thao tác Can thiệp Tồn kho Trực tiếp**:
   * Chức năng cộng trừ tồn kho bằng tay bị khóa cứng. Mọi biến động bắt buộc phải gắn liền với chứng từ hợp lệ.
3. **Ghi vết Sổ cái Kho Tự động (Inventory Ledger)**:
   * **Nhập kho (`IMPORT`)**: Tự động ghi nhận số lượng tăng kèm mã phiếu nhập hàng.
   * **Bán lẻ (`SELL`)**: Tự động ghi nhận số lượng giảm kèm mã đơn hàng của khách.
   * **Hoàn trả (`RETURN`)**: Tự động ghi nhận số lượng bù hoàn kèm mã đơn hàng bị hủy.
   * **Xuất hủy (`EXPIRED_DISPOSAL`)**: Tự động ghi nhận số lượng trừ kèm mã lô hàng quá hạn và số tiền vốn bị tổn thất.

#### Sơ đồ Mermaid:
```mermaid
flowchart TD
    TriggerEvent{"Sự kiện phát sinh trong hoạt động siêu thị"}
    
    TriggerEvent -- "1. Hoàn tất Phiếu Nhập Kho" --> EvImport["Kiểm định hàng về đạt chuẩn (+Số lượng)"]
    EvImport --> LedgerImport["Ghi Sổ cái kho: Loại = NHẬP KHO<br>Ghi rõ số lượng nhập & Mã phiếu nhập hàng"]
    LedgerImport --> UpdateInvPlus["Tăng số lượng tồn kho khả dụng của sản phẩm"]

    TriggerEvent -- "2. Khách hàng Đặt đơn thành công" --> EvSell["Khách xác nhận mua hàng (-Số lượng)"]
    EvSell --> LedgerSell["Ghi Sổ cái kho: Loại = BÁN HÀNG<br>Ghi rõ số lượng xuất & Mã đơn hàng tương ứng"]
    LedgerSell --> UpdateInvMinus["Giảm số lượng tồn kho khả dụng của sản phẩm"]

    TriggerEvent -- "3. Hủy bỏ đơn hàng" --> EvCancel["Hủy đơn hàng chưa giao (+Số lượng)"]
    EvCancel --> LedgerReturn["Ghi Sổ cái kho: Loại = HOÀN TRẢ ĐƠN HỦY<br>Ghi rõ số lượng phục hồi & Mã đơn hàng đã hủy"]
    LedgerReturn --> UpdateInvRestore["Phục hồi lại số lượng tồn kho sản phẩm & Lô hàng"]

    TriggerEvent -- "4. Tiêu hủy lô hàng hết hạn" --> EvDispose["Thủ kho xuất hủy hàng hết hạn (-Số lượng)"]
    EvDispose --> LedgerDispose["Ghi Sổ cái kho: Loại = XUẤT HỦY HÀNG HẾT DATE<br>Ghi rõ số lượng tiêu hủy & Giá trị vốn thiệt hại"]
    LedgerDispose --> UpdateInvWaste["Trừ dứt điểm tồn kho & Hạch toán tổn thất tài chính"]
```

---

### 3.6. Phân hệ Quản lý Mã Giảm Giá & Chiến dịch Khuyến mãi (Coupon Management)

#### Mục đích:
Tạo và kiểm soát các mã khuyến mãi kích cầu tiêu dùng cho khách hàng thân thiết và khách hàng mới.

#### Các quy trình con:
1. **Tạo mới Chiến dịch Mã giảm giá**: Quản trị viên nhập mã code (viết hoa), tiêu đề ưu đãi, hình thức giảm (theo phần trăm % hoặc theo số tiền cố định), giá trị giảm, hạn mức giảm tối đa, giá trị đơn hàng tối thiểu để áp dụng, thời gian bắt đầu/kết thúc, tổng lượt dùng tối đa và danh mục ngành hàng áp dụng (tùy chọn).
2. **Quy trình Xác thực 6 Bước khi Khách áp Voucher**:
   * Bước 1: Mã có tồn tại và đang ở trạng thái kích hoạt không?
   * Bước 2: Thời gian hiện tại có nằm trong thời hạn hiệu lực không?
   * Bước 3: Tổng lượt sử dụng đã đạt giới hạn tối đa chưa?
   * Bước 4: Tổng giá trị giỏ hàng có đạt điều kiện giá trị đơn tối thiểu không?
   * Bước 5: (Nếu có cấu hình danh mục) Giỏ hàng có chứa mặt hàng thuộc danh mục áp dụng không?
   * Bước 6: Tính số tiền chiết khấu thực tế (đảm bảo không vượt quá hạn mức giảm tối đa).
3. **Cập nhật Lượt dùng Tự động**:
   * Khi tạo đơn thành công: Tự động tăng số lượt đã sử dụng lên 1.
   * Khi đơn hàng bị hủy: Tự động hoàn trả 1 lượt sử dụng cho khách hàng.

#### Sơ đồ Mermaid:
```mermaid
flowchart TD
    ClientCode["Khách hàng nhập mã Voucher tại trang Thanh toán"] --> Validate1{"Bước 1: Mã tồn tại &<br>đang kích hoạt hoạt động?"}
    Validate1 -- "Không hợp lệ" --> Err1["Thông báo lỗi: Mã giảm giá không tồn tại hoặc đã bị khóa"]
    
    Validate1 -- "Hợp lệ" --> Validate2{"Bước 2: Thời gian hiện tại<br>nằm trong thời hạn áp dụng?"}
    Validate2 -- "Quá hạn / Chưa đến" --> Err2["Thông báo lỗi: Mã giảm giá đã hết hạn sử dụng"]

    Validate2 -- "Hợp lệ" --> Validate3{"Bước 3: Lượt dùng hiện tại<br>chưa đạt mức tối đa?"}
    Validate3 -- "Đã hết lượt" --> Err3["Thông báo lỗi: Mã ưu đãi đã hết lượt sử dụng"]

    Validate3 -- "Còn lượt" --> Validate4{"Bước 4: Tổng giá trị hàng<br>đạt mức tối thiểu yêu cầu?"}
    Validate4 -- "Chưa đủ" --> Err4["Thông báo lỗi: Đơn hàng chưa đạt giá trị tối thiểu để áp dụng"]

    Validate4 -- "Đủ điều kiện" --> Validate5{"Bước 5: Có yêu cầu ngành hàng<br>áp dụng cụ thể không?"}
    Validate5 -- "Không có hàng phù hợp" --> Err5["Thông báo lỗi: Đơn hàng không chứa sản phẩm thuộc ngành hàng được áp dụng"]
    
    Validate5 -- "Thỏa mãn điều kiện" --> CalcDiscount["Bước 6: Tính toán số tiền chiết khấu:<br>• Giảm theo %: (Tổng tiền x %) tối đa không vượt hạn mức trần<br>• Giảm tiền mặt: Khấu trừ trực tiếp số tiền cố định"]
    CalcDiscount --> SuccessApply["Áp dụng thành công:<br>Cập nhật số tiền giảm giá & Tính lại tổng tiền thực trả"]
```

---

### 3.7. Phân hệ Quản lý Cấu hình & Đối soát Thanh toán (Payment Management)

#### Mục đích:
Quản lý thông tin tài khoản ngân hàng chuyển khoản VietQR, ví điện tử MoMo của siêu thị và đối soát giao dịch thanh toán trực tuyến.

#### Các quy trình con:
1. **Cấu hình Thông tin Nhận tiền**: Cài đặt thông tin tài khoản ngân hàng MB Bank (Số tài khoản, Chủ tài khoản, Cú pháp chuyển khoản chuẩn `MINIMART {Mã_Đơn}`), thông tin ví MoMo và tải ảnh mã QR cố định.
2. **Bật / Tắt Phương thức Thanh toán**: Quản trị viên linh hoạt tắt phương thức chuyển khoản khi ngân hàng bảo trì để tránh gián đoạn trải nghiệm của khách hàng.
3. **Đối soát Giao dịch Thanh toán Trực tuyến**:
   * Quản trị viên mở bảng theo dõi giao dịch thanh toán để kiểm tra danh sách các đơn hàng chuyển khoản chờ xác nhận.
   * **Duyệt thanh toán**: Khi tiền đã về tài khoản ngân hàng, quản trị viên bấm "Duyệt thanh toán" $\rightarrow$ Giao dịch chuyển sang "Đã thanh toán", đơn hàng tự động chuyển sang "Đã xác nhận" và sẵn sàng điều phối giao hàng.
   * **Từ chối thanh toán**: Nếu sau thời gian chờ khách không chuyển tiền hoặc chuyển sai số tiền, quản trị viên bấm "Từ chối thanh toán" $\rightarrow$ Giao dịch chuyển sang "Thất bại", đơn hàng tự động chuyển sang "Đã hủy", hệ thống tự động hoàn trả tồn kho và mã giảm giá.

#### Sơ đồ Mermaid:
```mermaid
flowchart TD
    AdminPay([Quản trị viên mở Quản lý Thanh toán]) --> PayAction{"Chọn nghiệp vụ"}
    
    PayAction -- "Cấu hình tài khoản nhận tiền" --> EditConfig["Cài đặt Số tài khoản MB Bank, Tên chủ thẻ, Ví MoMo, Tải ảnh QR"]
    EditConfig --> SaveConfig["Lưu cấu hình phương thức thanh toán"]
    SaveConfig --> ClientView["Khách hàng mở trang Thanh toán:<br>Tự động tạo mã VietQR động gắn đúng số tiền và cú pháp"]

    PayAction -- "Đối soát giao dịch chuyển khoản" --> AuditTrans["Mở danh sách các giao dịch chờ kiểm tra"]
    AuditTrans --> CheckBankResult{"Kiểm tra tiền đã vào<br>tài khoản ngân hàng chưa?"}
    
    CheckBankResult -- "Đã nhận đúng số tiền" --> ApprovePay["Bấm 'Duyệt thanh toán'"]
    ApprovePay --> PaySuccess["Chuyển giao dịch sang: ĐÃ THANH TOÁN<br>Đơn hàng tự động chuyển sang: ĐÃ XÁC NHẬN<br>Sẵn sàng điều phối giao hàng"]

    CheckBankResult -- "Khách không chuyển / Sai thông tin" --> RejectPay["Bấm 'Từ chối thanh toán' kèm lý do"]
    RejectPay --> PayFail["Chuyển giao dịch sang: THẤT BẠI<br>Đơn hàng tự động chuyển sang: ĐÃ HỦY<br>Tự động hoàn trả tồn kho & Mã giảm giá"]
```

---

### 3.8. Phân hệ Quản lý Đánh giá & Phản hồi Khách hàng (Review Management)

#### Mục đích:
Thu thập ý kiến đánh giá chất lượng sản phẩm từ khách hàng đã mua và kiểm duyệt nội dung hiển thị công khai.

#### Các quy trình con:
1. **Khách hàng Gửi Đánh giá**: Khách hàng mở trang chi tiết sản phẩm đã mua, chọn số sao đánh giá (1 đến 5 sao) và viết nhận xét thực tế.
2. **Kiểm duyệt Đánh giá (Admin Moderation)**:
   * Đánh giá mới tạo mặc định ở trạng thái **Chờ duyệt** để phòng chống nội dung quảng cáo rác hoặc từ ngữ vi phạm tiêu chuẩn cộng đồng.
   * Quản trị viên kiểm tra tại trang quản lý đánh giá: bấm "Duyệt" để công khai hiển thị trên trang sản phẩm hoặc bấm "Xóa" nếu đánh giá sai lệch, phản cảm.
3. **Cập nhật Điểm Đánh giá Trung bình**: Hệ thống tự động tính toán lại điểm sao trung bình của sản phẩm dựa trên các đánh giá đã được duyệt công khai.

#### Sơ đồ Mermaid:
```mermaid
flowchart TD
    CustOrder["Khách hàng đã nhận và trải nghiệm sản phẩm"] --> OpenProduct["Mở trang chi tiết sản phẩm trên website"]
    OpenProduct --> WriteReview["Chọn số sao (1 - 5 Sao) & Viết nhận xét cảm nhận"]
    WriteReview --> SubmitReview["Gửi đánh giá lên hệ thống"]
    SubmitReview --> SaveReview["Hệ thống lưu đánh giá vào danh sách CHỜ KIỂM DUYỆT"]
    SaveReview --> AdminReviewUI["Hiển thị tại trang Quản lý Đánh giá của Quản trị viên"]
    AdminReviewUI --> ModReview{"Quản trị viên kiểm duyệt"}
    ModReview -- "Nhận xét văn minh, đúng thực tế" --> ApproveRev["Bấm 'Duyệt đánh giá'"]
    ApproveRev --> PublicRev["Hiển thị công khai đánh giá trên trang sản phẩm<br>Tự động cập nhật điểm sao trung bình của sản phẩm"]
    ModReview -- "Nội dung phản cảm, quảng cáo rác" --> DeleteReview["Bấm 'Xóa bỏ đánh giá vi phạm'"]
```

---

### 3.9. Phân hệ Hỗ trợ Trực tuyến & Trợ lý Ảo Thông minh (Support & AI Bot)

#### Mục đích:
Hỗ trợ giải đáp thắc mắc của khách hàng 24/7 về giá cả, vị trí sản phẩm, trạng thái giao hàng và tiếp nhận khiếu nại chất lượng dịch vụ.

#### Các quy trình con:
1. **Tư vấn tự động bằng Trợ lý ảo AI (Chatbot)**:
   * Khách bấm vào biểu tượng Chatbot tại góc màn hình.
   * Khách nhập câu hỏi (ví dụ: "Sản phẩm nào đang rẻ nhất?", "Có rau sạch không?", "Hôm nay có khuyến mãi gì?").
   * Trợ lý ảo sử dụng mô hình AI kết hợp dữ liệu sản phẩm thực tế trong siêu thị để phản hồi ngắn gọn, thân thiện và chính xác cho khách.
2. **Gửi Phản hồi / Hỗ trợ Trực tiếp (Customer Messaging)**:
   * Khách hàng gửi tin nhắn yêu cầu hỗ trợ hoặc góp ý đến siêu thị.
   * Hệ thống tạo cuộc hội thoại mới và đánh dấu thông báo chưa đọc cho Quản trị viên.
   * Quản trị viên mở bảng tin nhắn hỗ trợ, đọc nội dung và gửi phản hồi trả lời trực tiếp cho khách hàng.

#### Sơ đồ Mermaid:
```mermaid
sequenceDiagram
    autonumber
    actor C as Khách hàng
    participant BotUI as Cửa sổ Trợ lý ảo AI
    participant ChatEngine as Bộ xử lý Ngữ cảnh AI
    participant MsgSvc as Hệ thống Tin nhắn Hỗ trợ
    actor A as Nhân viên Chăm sóc Khách hàng

    alt Sử dụng Trợ lý ảo tư vấn tự động
        C->>BotUI: Nhập câu hỏi (VD: "Sản phẩm nào giá tốt nhất hôm nay?")
        BotUI->>ChatEngine: Gửi câu hỏi kèm thông tin sản phẩm trong kho
        ChatEngine->>ChatEngine: Phân tích và sinh câu trả lời phù hợp
        ChatEngine-->>BotUI: Phản hồi kết quả tư vấn thân thiện
        BotUI-->>C: Hiển thị câu trả lời ngay lập tức
    else Gửi tin nhắn yêu cầu hỗ trợ trực tiếp
        C->>BotUI: Nhập tin nhắn cần hỗ trợ / khiếu nại dịch vụ
        BotUI->>MsgSvc: Gửi tin nhắn đến Hộp thư hỗ trợ siêu thị
        MsgSvc->>A: Thông báo có tin nhắn mới từ khách hàng
        A->>MsgSvc: Soạn câu trả lời giải đáp thắc mắc
        MsgSvc-->>BotUI: Gửi câu trả lời tới tài khoản khách hàng
        BotUI-->>C: Khách hàng đọc được tin nhắn phản hồi
    end
```

---

### 3.10. Phân hệ Quản lý Tài khoản & Phân quyền Người dùng (User & Security)

#### Mục đích:
Quản lý đăng ký, đăng nhập an toàn, phân quyền chặt chẽ theo vai trò và bảo vệ dữ liệu thông tin cá nhân của người dùng.

#### Các quy trình con:
1. **Đăng ký tài khoản truyền thống**: Khách hàng nhập Tên đăng nhập, Email, Mật khẩu, Họ tên và SĐT. Mật khẩu được mã hóa an toàn bằng thuật toán BCrypt trước khi lưu trữ.
2. **Đăng nhập truyền thống**: Khách đăng nhập bằng Tên đăng nhập/Email và Mật khẩu. Khi thông tin khớp, hệ thống cấp phát chuỗi mã thông báo bảo mật (JWT Token) để xác thực các phiên thao tác tiếp theo.
3. **Đăng nhập nhanh bằng Mạng xã hội (Google / Facebook OAuth2)**:
   * Khách chọn "Đăng nhập với Google". Hệ thống chuyển hướng sang cổng bảo mật của Google.
   * Khách đồng ý cấp quyền $\rightarrow$ Google gửi lại thông tin hồ sơ (Email, Tên, Ảnh đại diện).
   * Hệ thống tự động tạo tài khoản mới (nếu đăng nhập lần đầu) hoặc liên kết với tài khoản có sẵn và cấp mã thông báo bảo mật đăng nhập ngay.
4. **Quản trị Tài khoản & Phân quyền (Admin)**:
   * Quản trị viên xem danh sách người dùng, lọc theo vai trò (Khách hàng, Shipper, Quản trị viên).
   * Khóa hoặc mở khóa tài khoản người dùng vi phạm chính sách (tài khoản Quản trị viên chính được bảo vệ, cấm khóa).
   * Cập nhật thông tin cá nhân và quản lý sổ địa chỉ giao hàng nhận hàng.

#### Sơ đồ Mermaid:
```mermaid
flowchart TD
    LoginEntry([Khách hàng mở màn hình Đăng nhập]) --> LoginType{"Chọn phương thức đăng nhập"}
    
    LoginType -- "Tài khoản & Mật khẩu thông thường" --> InputPass["Nhập Tên đăng nhập/Email & Mật khẩu"]
    InputPass --> CheckDB{"Hệ thống kiểm tra<br>mã hóa mật khẩu?"}
    CheckDB -- "Sai mật khẩu" --> FailAuth["Thông báo: Sai tài khoản hoặc mật khẩu"]
    CheckDB -- "Chính xác" --> GenJWT["Cấp phát Mã thông báo bảo mật (JWT Token)<br>Đăng nhập thành công"]

    LoginType -- "Đăng nhập nhanh với Google" --> RedirectGoogle["Chuyển hướng sang Cổng xác thực Google"]
    RedirectGoogle --> GoogleSuccess["Google xác thực và gửi lại thông tin cá nhân"]
    GoogleSuccess --> CheckUserExists{"Email Google đã có<br>trong hệ thống chưa?"}
    CheckUserExists -- "Chưa có" --> CreateOAuthUser["Tự động tạo tài khoản người dùng mới"]
    CheckUserExists -- "Đã có" --> LinkUser["Liên kết thông tin đăng nhập"]
    CreateOAuthUser --> GenJWT
    LinkUser --> GenJWT

    GenJWT --> RoleRoute{"Kiểm tra Phân quyền tài khoản"}
    RoleRoute -- "Khách hàng thông thường" --> GoStore["Chuyển tới Trang Mua sắm siêu thị"]
    RoleRoute -- "Nhân viên giao hàng" --> GoShipper["Chuyển tới Bảng điều khiển Shipper"]
    RoleRoute -- "Quản trị viên" --> GoAdmin["Chuyển tới Trang Quản trị Siêu thị (Admin)"]
```

---

### 3.11. Phân hệ Báo cáo Thống kê & Phân tích Hoạt động Kinh doanh (Analytics & Reports)

#### Mục đích:
Cung cấp số liệu tài chính trực quan, đo lường tốc độ bán hàng, đánh giá năng suất giao hàng và thống kê tổn thất vốn do hàng hóa hết hạn để ban quản lý đưa ra quyết định kịp thời.

#### Các quy trình con:
1. **Tổng quan Chỉ số Hoạt động (Dashboard KPI)**:
   * Tổng doanh thu thực tế thu về từ các đơn hàng đã hoàn tất.
   * Tổng số đơn hàng đã phục vụ thành công.
   * Tổng số lượng khách hàng đăng ký thành viên.
   * Tổng số danh mục sản phẩm đang kinh doanh.
2. **Phân tích Doanh thu theo Chu kỳ Thời gian**:
   * Biểu đồ đường thể hiện doanh thu lũy kế theo từng tháng/ngày giúp so sánh tốc độ tăng trưởng kinh doanh.
3. **Phân bổ Cơ cấu Trạng thái Đơn hàng**:
   * Biểu đồ tròn thể hiện tỷ lệ phần trăm giữa đơn thành công, đơn đang giao, đơn hủy và đơn giao thất bại để đánh giá chất lượng phục vụ.
4. **Báo cáo Hiệu suất Giao hàng của Shipper**:
   * Bảng thống kê chi tiết cho từng nhân viên giao hàng: Số đơn được phân bổ, Số đơn giao thành công, Số đơn giao thất bại, Tỷ lệ giao đạt chuẩn (%) và Tổng số tiền mặt COD đang thu hộ.
5. **Báo cáo Tổn thất Lô hàng Hết hạn dùng**:
   * Thống kê toàn bộ các lô hàng quá hạn đã bị xuất hủy: Tên mặt hàng, Mã số lô, Số lượng hư hỏng và Tổng số tiền vốn bị thiệt hại để điều chỉnh kế hoạch nhập hàng lần sau.

#### Sơ đồ Mermaid:
```mermaid
flowchart TD
    AdminReport([Quản trị viên mở Trung tâm Báo cáo - Thống kê]) --> TabChoice{"Chọn loại báo cáo cần xem"}
    
    TabChoice -- "1. Tổng quan Doanh thu & Tăng trưởng" --> KPIQuery["Hệ thống tổng hợp các đơn hàng hoàn tất:<br>• Tổng doanh thu thực nhận<br>• Doanh thu chi tiết theo từng tháng trong năm"]
    KPIQuery --> RenderCharts["Vẽ Biểu đồ xu hướng Doanh thu & Thẻ chỉ số tổng quan"]

    TabChoice -- "2. Cơ cấu Trạng thái Đơn hàng" --> DistQuery["Phân nhóm toàn bộ đơn hàng trong hệ thống:<br>• Đơn hoàn tất thành công (Xanh lá)<br>• Đơn đang vận chuyển (Vàng)<br>• Đơn bị hủy (Đỏ)<br>• Đơn giao không thành công (Tím)"]
    DistQuery --> RenderPie["Vẽ Biểu đồ tròn Phân bổ Tỷ lệ Đơn hàng"]

    TabChoice -- "3. Năng suất Giao hàng của Shipper" --> ShipperQuery["Tổng hợp số liệu giao nhận của từng Shipper:<br>• Số đơn giao thành công / Tổng đơn nhận<br>• Tỷ lệ % giao hàng thành công<br>• Tiền mặt COD đã thu hộ cần nộp về quỹ"]
    ShipperQuery --> RenderShipperTable["Hiển thị Bảng xếp hạng Hiệu suất Shipper"]

    TabChoice -- "4. Tổn thất Hàng hết hạn dùng" --> WasteQuery["Tổng hợp các Lô hàng đã quá hạn bị xuất hủy:<br>• Danh sách lô hàng & Số lượng sản phẩm tiêu hủy<br>• Tổng số tiền vốn nhập bị thiệt hại"]
    WasteQuery --> RenderLossKPI["Hiển thị Báo cáo Thất thoát Vốn Hàng hết hạn"]
```

---

## TỔNG KẾT DANH MỤC CÁC PHÂN HỆ VÀ TRÁCH NHIỆM

| STT | Phân hệ nghiệp vụ | Trách nhiệm chính trong hệ thống | Đối tượng tương tác chính |
| :---: | :--- | :--- | :--- |
| **1** | **Mua sắm & Giỏ hàng** | Khám phá sản phẩm, kiểm tra tồn kho khả dụng tức thời, quản lý mặt hàng trong giỏ hàng. | Khách hàng |
| **2** | **Đặt hàng & Xử lý đơn** | Tạo đơn hàng, khấu trừ chiết khấu hội viên và voucher, trừ kho FIFO theo lô hạn dùng, duyệt đơn. | Khách hàng, Quản trị viên |
| **3** | **Điều phối & Giao hàng** | Tự động phân bổ đơn theo địa bàn quận huyện (Tân Phú, Tân Bình, Quận 12), giao hàng, thu tiền COD. | Quản trị viên, Shipper |
| **4** | **Quản lý Lô & Hạn dùng** | Theo dõi hạn sử dụng, xả kho cận date giá ưu đãi, khóa cấm bán và xuất hủy hàng hết hạn. | Quản lý kho |
| **5** | **Nhập kho & Kiểm định QC** | Lập phiếu nhập từ nhà cung cấp, kiểm định chất lượng đạt/lỗi, tự động tạo lô hàng mới và tăng tồn kho. | Quản lý kho, Thủ kho |
| **6** | **Thương hiệu & Ngành hàng**| Phân loại hàng hóa theo cây danh mục, quản lý nhãn hiệu đối tác kinh doanh. | Quản trị viên, Khách hàng |
| **7** | **Khuyến mãi & Mã giảm giá**| Tạo chiến dịch ưu đãi, kiểm tra 6 tiêu chí hợp lệ khi áp dụng voucher, quản lý số lượt sử dụng. | Quản trị viên, Khách hàng |
| **8** | **Khách hàng thân thiết** | Tích lũy 1 điểm cho mỗi 100.000đ thanh toán, tự động nâng hạng thẻ (Đồng, Bạc, Vàng, Kim Cương). | Khách hàng |
| **9** | **Đánh giá & Phản hồi** | Khách hàng chấm điểm và viết nhận xét sản phẩm, quản trị viên kiểm duyệt nội dung công khai. | Khách hàng, Quản trị viên |
| **10**| **Hỗ trợ & Trợ lý ảo AI** | Trợ lý ảo AI tư vấn sản phẩm 24/7, tiếp nhận và phản hồi tin nhắn hỗ trợ trực tiếp giữa khách và admin. | Khách hàng, Quản trị viên |
| **11**| **Tài khoản & Bảo mật** | Đăng ký, đăng nhập bảo mật (BCrypt + JWT), đăng nhập Google OAuth2, phân quyền truy cập. | Toàn bộ người dùng |
| **12**| **Báo cáo & Thống kê** | Phân tích doanh thu, tỷ lệ đơn hàng, hiệu suất nhân viên giao hàng, báo cáo tổn thất vốn hàng hủy. | Quản trị viên, Ban quản lý |
