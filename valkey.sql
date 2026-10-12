-- =============================================================
-- valkey.sql (TẬP LỆNH DATABASE TOÀN DIỆN & DUY NHẤT)
-- Tích hợp đầy đủ:
--  1. Cấu trúc bảng (24 entities) chuẩn JPA/Hibernate (Spring Boot 3.2)
--  2. Hàng đợi Valkey Stream & Transactional Outbox (order_jobs, outbox_events, idempotency_key)
--  3. Khóa ngoại, ràng buộc CHECK, chỉ mục tối ưu truy vấn
--  4. Quản lý phân lô (Product Batches: import_price, sale_price, status)
--  5. Xuất hủy hàng quá hạn & Sổ cái biến động kho (Inventory Ledger)
--  6. 50 đơn hàng lịch sử TP.HCM (Tân Phú, Tân Bình, Quận 12) cho thuật toán Apriori
--  7. Đầy đủ Shipper & Phân hạng khách hàng VIP (Diamond, Gold, Silver, Bronze)
-- =============================================================

IF DB_ID(N'valkey') IS NULL
BEGIN
    CREATE DATABASE valkey;
END
GO

USE valkey;
GO

SET NOCOUNT ON;
SET XACT_ABORT ON;
GO

-- =============================================================
-- 0. DROP OLD TABLES IN FK-SAFE ORDER
-- =============================================================
IF OBJECT_ID(N'dbo.order_jobs', N'U') IS NOT NULL DROP TABLE dbo.order_jobs;
IF OBJECT_ID(N'dbo.outbox_events', N'U') IS NOT NULL DROP TABLE dbo.outbox_events;
IF OBJECT_ID(N'dbo.customer_messages', N'U') IS NOT NULL DROP TABLE dbo.customer_messages;
IF OBJECT_ID(N'dbo.payments', N'U') IS NOT NULL DROP TABLE dbo.payments;
IF OBJECT_ID(N'dbo.payment_method_configs', N'U') IS NOT NULL DROP TABLE dbo.payment_method_configs;
IF OBJECT_ID(N'dbo.order_items', N'U') IS NOT NULL DROP TABLE dbo.order_items;
IF OBJECT_ID(N'dbo.orders', N'U') IS NOT NULL DROP TABLE dbo.orders;
IF OBJECT_ID(N'dbo.reviews', N'U') IS NOT NULL DROP TABLE dbo.reviews;
IF OBJECT_ID(N'dbo.cart_items', N'U') IS NOT NULL DROP TABLE dbo.cart_items;
IF OBJECT_ID(N'dbo.cart', N'U') IS NOT NULL DROP TABLE dbo.cart;
IF OBJECT_ID(N'dbo.inventory_ledger', N'U') IS NOT NULL DROP TABLE dbo.inventory_ledger;
IF OBJECT_ID(N'dbo.product_batches', N'U') IS NOT NULL DROP TABLE dbo.product_batches;
IF OBJECT_ID(N'dbo.goods_receipt_items', N'U') IS NOT NULL DROP TABLE dbo.goods_receipt_items;
IF OBJECT_ID(N'dbo.goods_receipt', N'U') IS NOT NULL DROP TABLE dbo.goods_receipt;
IF OBJECT_ID(N'dbo.inventory', N'U') IS NOT NULL DROP TABLE dbo.inventory;
IF OBJECT_ID(N'dbo.product_images', N'U') IS NOT NULL DROP TABLE dbo.product_images;
IF OBJECT_ID(N'dbo.addresses', N'U') IS NOT NULL DROP TABLE dbo.addresses;
IF OBJECT_ID(N'dbo.coupons', N'U') IS NOT NULL DROP TABLE dbo.coupons;
IF OBJECT_ID(N'dbo.products', N'U') IS NOT NULL DROP TABLE dbo.products;
IF OBJECT_ID(N'dbo.suppliers', N'U') IS NOT NULL DROP TABLE dbo.suppliers;
IF OBJECT_ID(N'dbo.users', N'U') IS NOT NULL DROP TABLE dbo.users;
IF OBJECT_ID(N'dbo.brands', N'U') IS NOT NULL DROP TABLE dbo.brands;
IF OBJECT_ID(N'dbo.categories', N'U') IS NOT NULL DROP TABLE dbo.categories;
IF OBJECT_ID(N'dbo.roles', N'U') IS NOT NULL DROP TABLE dbo.roles;
GO

-- =============================================================
-- 1. ROLES
-- Java: Role.java -> roles(id, name)
-- =============================================================
CREATE TABLE dbo.roles (
    id   BIGINT IDENTITY(1,1) NOT NULL,
    name NVARCHAR(50) NOT NULL,
    CONSTRAINT PK_roles PRIMARY KEY (id),
    CONSTRAINT UQ_roles_name UNIQUE (name)
);
GO

-- =============================================================
-- 2. USERS
-- Java: User.java
-- password is nullable because OAuth2 users can have no local password.
-- =============================================================
CREATE TABLE dbo.users (
    id              BIGINT IDENTITY(1,1) NOT NULL,
    username        NVARCHAR(50) NOT NULL,
    email           NVARCHAR(100) NOT NULL,
    password        NVARCHAR(255) NULL,
    full_name       NVARCHAR(100) NULL,
    phone           NVARCHAR(20) NULL,
    role_id         BIGINT NULL,
    is_active       BIT NOT NULL CONSTRAINT DF_users_is_active DEFAULT (1),
    loyalty_points  INT NOT NULL CONSTRAINT DF_users_loyalty_points DEFAULT (0),
    membership_tier NVARCHAR(50) NOT NULL CONSTRAINT DF_users_membership_tier DEFAULT (N'BRONZE'),
    created_at      DATETIME2 NOT NULL CONSTRAINT DF_users_created_at DEFAULT (GETDATE()),
    provider        NVARCHAR(20) NULL CONSTRAINT DF_users_provider DEFAULT (N'LOCAL'),
    provider_id     NVARCHAR(100) NULL,
    avatar_url      NVARCHAR(512) NULL,
    CONSTRAINT PK_users PRIMARY KEY (id),
    CONSTRAINT UQ_users_username UNIQUE (username),
    CONSTRAINT UQ_users_email UNIQUE (email),
    CONSTRAINT FK_users_role FOREIGN KEY (role_id) REFERENCES dbo.roles(id) ON DELETE SET NULL
);
GO

-- =============================================================
-- 3. ADDRESSES
-- Java: Address.java
-- =============================================================
CREATE TABLE dbo.addresses (
    id             BIGINT IDENTITY(1,1) NOT NULL,
    user_id        BIGINT NULL,
    receiver_name  NVARCHAR(100) NOT NULL,
    receiver_phone NVARCHAR(20) NOT NULL,
    province       NVARCHAR(100) NOT NULL,
    district       NVARCHAR(100) NOT NULL,
    ward           NVARCHAR(100) NOT NULL,
    detail_address NVARCHAR(255) NOT NULL,
    is_default     BIT NOT NULL CONSTRAINT DF_addresses_is_default DEFAULT (0),
    CONSTRAINT PK_addresses PRIMARY KEY (id),
    CONSTRAINT FK_addresses_user FOREIGN KEY (user_id) REFERENCES dbo.users(id) ON DELETE SET NULL
);
GO

-- =============================================================
-- 4. CATEGORIES
-- Java: Category.java
-- =============================================================
CREATE TABLE dbo.categories (
    id         BIGINT IDENTITY(1,1) NOT NULL,
    name       NVARCHAR(100) NOT NULL,
    description NVARCHAR(500) NULL,
    image      NVARCHAR(255) NULL,
    is_active  BIT NOT NULL CONSTRAINT DF_categories_is_active DEFAULT (1),
    created_at DATETIME2 NOT NULL CONSTRAINT DF_categories_created_at DEFAULT (GETDATE()),
    CONSTRAINT PK_categories PRIMARY KEY (id),
    CONSTRAINT UQ_categories_name UNIQUE (name)
);
GO

-- =============================================================
-- 5. BRANDS
-- Java: Brand.java
-- =============================================================
CREATE TABLE dbo.brands (
    id          BIGINT IDENTITY(1,1) NOT NULL,
    name        NVARCHAR(100) NOT NULL,
    description NVARCHAR(500) NULL,
    is_active   BIT NOT NULL CONSTRAINT DF_brands_is_active DEFAULT (1),
    created_at  DATETIME2 NOT NULL CONSTRAINT DF_brands_created_at DEFAULT (GETDATE()),
    CONSTRAINT PK_brands PRIMARY KEY (id),
    CONSTRAINT UQ_brands_name UNIQUE (name)
);
GO

-- =============================================================
-- 6. SUPPLIERS
-- Java: Supplier.java
-- =============================================================
CREATE TABLE dbo.suppliers (
    id           BIGINT IDENTITY(1,1) NOT NULL,
    name         NVARCHAR(255) NOT NULL,
    contact_name NVARCHAR(100) NULL,
    phone        NVARCHAR(20) NULL,
    email        NVARCHAR(100) NULL,
    address      NVARCHAR(500) NULL,
    is_active    BIT NOT NULL CONSTRAINT DF_suppliers_is_active DEFAULT (1),
    created_at   DATETIME2 NOT NULL CONSTRAINT DF_suppliers_created_at DEFAULT (GETDATE()),
    CONSTRAINT PK_suppliers PRIMARY KEY (id)
);
GO

-- =============================================================
-- 7. PRODUCTS
-- Java: Product.java
-- Exact fields: name, description, price, sale_price, category_id,
-- brand_id, main_image, sku, barcode, unit, weight_g, is_active,
-- created_at, updated_at.
-- =============================================================
CREATE TABLE dbo.products (
    id          BIGINT IDENTITY(1,1) NOT NULL,
    name        NVARCHAR(255) NOT NULL,
    description NVARCHAR(MAX) NULL,
    price       DECIMAL(18,2) NOT NULL,
    sale_price  DECIMAL(18,2) NULL,
    category_id BIGINT NULL,
    brand_id    BIGINT NULL,
    main_image  NVARCHAR(255) NULL,
    sku         NVARCHAR(50) NULL,
    barcode     NVARCHAR(100) NULL,
    unit        NVARCHAR(50) NULL,
    weight_g    INT NULL,
    is_active   BIT NOT NULL CONSTRAINT DF_products_is_active DEFAULT (1),
    created_at  DATETIME2 NOT NULL CONSTRAINT DF_products_created_at DEFAULT (GETDATE()),
    updated_at  DATETIME2 NULL,
    CONSTRAINT PK_products PRIMARY KEY (id),
    CONSTRAINT UQ_products_name UNIQUE (name),
    CONSTRAINT UQ_products_sku UNIQUE (sku),
    CONSTRAINT CK_products_price_nonnegative CHECK (price >= 0),
    CONSTRAINT CK_products_sale_price_nonnegative CHECK (sale_price IS NULL OR sale_price >= 0),
    CONSTRAINT FK_products_category FOREIGN KEY (category_id) REFERENCES dbo.categories(id) ON DELETE SET NULL,
    CONSTRAINT FK_products_brand FOREIGN KEY (brand_id) REFERENCES dbo.brands(id) ON DELETE SET NULL
);
GO

-- =============================================================
-- 8. PRODUCT IMAGES
-- Java: ProductImage.java
-- =============================================================
CREATE TABLE dbo.product_images (
    id         BIGINT IDENTITY(1,1) NOT NULL,
    product_id BIGINT NULL,
    image_path NVARCHAR(255) NOT NULL,
    sort_order INT NULL CONSTRAINT DF_product_images_sort_order DEFAULT (0),
    CONSTRAINT PK_product_images PRIMARY KEY (id),
    CONSTRAINT FK_product_images_product FOREIGN KEY (product_id) REFERENCES dbo.products(id) ON DELETE CASCADE
);
GO

-- =============================================================
-- 9. INVENTORY
-- Java: Inventory.java
-- @OneToOne product + @Check(current_stock >= 0)
-- =============================================================
CREATE TABLE dbo.inventory (
    id            BIGINT IDENTITY(1,1) NOT NULL,
    product_id    BIGINT NULL,
    current_stock INT NOT NULL CONSTRAINT DF_inventory_current_stock DEFAULT (0),
    minimum_stock INT NOT NULL CONSTRAINT DF_inventory_minimum_stock DEFAULT (5),
    location      NVARCHAR(100) NULL,
    last_updated  DATETIME2 NULL,
    created_at    DATETIME2 NOT NULL CONSTRAINT DF_inventory_created_at DEFAULT (GETDATE()),
    CONSTRAINT PK_inventory PRIMARY KEY (id),
    CONSTRAINT UQ_inventory_product UNIQUE (product_id),
    CONSTRAINT CK_inventory_current_stock_nonnegative CHECK (current_stock >= 0),
    CONSTRAINT FK_inventory_product FOREIGN KEY (product_id) REFERENCES dbo.products(id) ON DELETE CASCADE
);
GO

-- =============================================================
-- 10. GOODS RECEIPT
-- Java: GoodsReceipt.java
-- =============================================================
CREATE TABLE dbo.goods_receipt (
    id           BIGINT IDENTITY(1,1) NOT NULL,
    supplier_id  BIGINT NULL,
    brand_id     BIGINT NULL,
    total_amount DECIMAL(18,2) NOT NULL CONSTRAINT DF_goods_receipt_total DEFAULT (0),
    note         NVARCHAR(500) NULL,
    status       NVARCHAR(50) NOT NULL CONSTRAINT DF_goods_receipt_status DEFAULT (N'DRAFT'),
    created_by   BIGINT NULL,
    created_at   DATETIME2 NOT NULL CONSTRAINT DF_goods_receipt_created_at DEFAULT (GETDATE()),
    updated_at   DATETIME2 NULL,
    CONSTRAINT PK_goods_receipt PRIMARY KEY (id),
    CONSTRAINT CK_goods_receipt_total_nonnegative CHECK (total_amount >= 0),
    CONSTRAINT FK_goods_receipt_supplier FOREIGN KEY (supplier_id) REFERENCES dbo.suppliers(id) ON DELETE NO ACTION,
    CONSTRAINT FK_goods_receipt_brand FOREIGN KEY (brand_id) REFERENCES dbo.brands(id) ON DELETE SET NULL,
    CONSTRAINT FK_goods_receipt_created_by FOREIGN KEY (created_by) REFERENCES dbo.users(id) ON DELETE SET NULL
);
GO

-- =============================================================
-- 11. GOODS RECEIPT ITEMS
-- Java: GoodsReceiptItem.java
-- =============================================================
CREATE TABLE dbo.goods_receipt_items (
    id                BIGINT IDENTITY(1,1) NOT NULL,
    goods_receipt_id  BIGINT NOT NULL,
    product_id        BIGINT NOT NULL,
    quantity          INT NOT NULL,
    import_price      DECIMAL(18,2) NOT NULL,
    batch_name        NVARCHAR(50) NULL,
    expiry_date       DATE NULL,
    passed_quantity   INT NULL,
    rejected_quantity INT NULL,
    reject_reason     NVARCHAR(255) NULL,
    qc_status         NVARCHAR(50) NULL CONSTRAINT DF_goods_receipt_items_qc DEFAULT (N'PASSED'),
    qc_note           NVARCHAR(500) NULL,
    inspected_at      DATETIME2 NULL,
    inspected_by      BIGINT NULL,
    CONSTRAINT PK_goods_receipt_items PRIMARY KEY (id),
    CONSTRAINT CK_goods_receipt_items_quantity_positive CHECK (quantity > 0),
    CONSTRAINT CK_goods_receipt_items_import_price_nonnegative CHECK (import_price >= 0),
    CONSTRAINT FK_goods_receipt_items_receipt FOREIGN KEY (goods_receipt_id) REFERENCES dbo.goods_receipt(id) ON DELETE CASCADE,
    CONSTRAINT FK_goods_receipt_items_product FOREIGN KEY (product_id) REFERENCES dbo.products(id) ON DELETE NO ACTION,
    CONSTRAINT FK_goods_receipt_items_inspected_by FOREIGN KEY (inspected_by) REFERENCES dbo.users(id) ON DELETE SET NULL
);
GO

-- =============================================================
-- 12. PRODUCT BATCHES
-- Java: ProductBatch.java
-- =============================================================
CREATE TABLE dbo.product_batches (
    id               BIGINT IDENTITY(1,1) NOT NULL,
    product_id       BIGINT NOT NULL,
    goods_receipt_id BIGINT NULL,
    batch_name       NVARCHAR(50) NOT NULL,
    quantity         INT NOT NULL CONSTRAINT DF_product_batches_quantity DEFAULT (0),
    expiry_date      DATE NOT NULL,
    import_price     DECIMAL(18,2) NULL,
    sale_price       DECIMAL(18,2) NULL,
    status           NVARCHAR(20) NOT NULL CONSTRAINT DF_product_batches_status DEFAULT ('ACTIVE'),
    created_at       DATETIME2 NOT NULL CONSTRAINT DF_product_batches_created_at DEFAULT (GETDATE()),
    CONSTRAINT PK_product_batches PRIMARY KEY (id),
    CONSTRAINT CK_product_batches_quantity_nonnegative CHECK (quantity >= 0),
    CONSTRAINT FK_product_batches_product FOREIGN KEY (product_id) REFERENCES dbo.products(id) ON DELETE CASCADE,
    CONSTRAINT FK_product_batches_receipt FOREIGN KEY (goods_receipt_id) REFERENCES dbo.goods_receipt(id) ON DELETE SET NULL
);
GO

-- =============================================================
-- 13. INVENTORY LEDGER
-- Java: InventoryLedger.java
-- =============================================================
CREATE TABLE dbo.inventory_ledger (
    id              BIGINT IDENTITY(1,1) NOT NULL,
    inventory_id    BIGINT NOT NULL,
    change_type     NVARCHAR(50) NOT NULL,
    quantity_change INT NOT NULL,
    previous_stock  INT NOT NULL,
    new_stock       INT NOT NULL,
    reference_id    BIGINT NULL,
    note            NVARCHAR(500) NULL,
    created_by      BIGINT NULL,
    created_at      DATETIME2 NOT NULL CONSTRAINT DF_inventory_ledger_created_at DEFAULT (GETDATE()),
    CONSTRAINT PK_inventory_ledger PRIMARY KEY (id),
    CONSTRAINT FK_inventory_ledger_inventory FOREIGN KEY (inventory_id) REFERENCES dbo.inventory(id) ON DELETE CASCADE,
    CONSTRAINT FK_inventory_ledger_created_by FOREIGN KEY (created_by) REFERENCES dbo.users(id) ON DELETE SET NULL
);
GO

-- =============================================================
-- 14. CART
-- Java: Cart.java
-- =============================================================
CREATE TABLE dbo.cart (
    id         BIGINT IDENTITY(1,1) NOT NULL,
    user_id    BIGINT NULL,
    created_at DATETIME2 NOT NULL CONSTRAINT DF_cart_created_at DEFAULT (GETDATE()),
    CONSTRAINT PK_cart PRIMARY KEY (id),
    CONSTRAINT UQ_cart_user UNIQUE (user_id),
    CONSTRAINT FK_cart_user FOREIGN KEY (user_id) REFERENCES dbo.users(id) ON DELETE SET NULL
);
GO

-- =============================================================
-- 15. CART ITEMS
-- Java: CartItem.java
-- =============================================================
CREATE TABLE dbo.cart_items (
    id         BIGINT IDENTITY(1,1) NOT NULL,
    cart_id    BIGINT NULL,
    product_id BIGINT NULL,
    quantity   INT NOT NULL CONSTRAINT DF_cart_items_quantity DEFAULT (1),
    CONSTRAINT PK_cart_items PRIMARY KEY (id),
    CONSTRAINT UQ_cart_items_cart_product UNIQUE (cart_id, product_id),
    CONSTRAINT CK_cart_items_quantity_positive CHECK (quantity > 0),
    CONSTRAINT FK_cart_items_cart FOREIGN KEY (cart_id) REFERENCES dbo.cart(id) ON DELETE CASCADE,
    CONSTRAINT FK_cart_items_product FOREIGN KEY (product_id) REFERENCES dbo.products(id) ON DELETE CASCADE
);
GO

-- =============================================================
-- 16. ORDERS
-- Java: Order.java
-- =============================================================
CREATE TABLE dbo.orders (
    id                     BIGINT IDENTITY(1,1) NOT NULL,
    user_id                BIGINT NULL,
    shipper_id             BIGINT NULL,
    total_amount           DECIMAL(18,2) NOT NULL,
    discount_amount        DECIMAL(18,2) NULL,
    final_amount           DECIMAL(18,2) NOT NULL,
    status                 NVARCHAR(30) NOT NULL CONSTRAINT DF_orders_status DEFAULT (N'CHO_XAC_NHAN'),
    shipping_name          NVARCHAR(100) NOT NULL,
    shipping_phone         NVARCHAR(20) NOT NULL,
    shipping_address       NVARCHAR(500) NOT NULL,
    payment_method         NVARCHAR(50) NOT NULL CONSTRAINT DF_orders_payment_method DEFAULT (N'COD'),
    coupon_code            NVARCHAR(50) NULL,
    idempotency_key        NVARCHAR(128) NULL,
    note                   NVARCHAR(500) NULL,
    delivery_note          NVARCHAR(500) NULL,
    delivered_at           DATETIME2 NULL,
    delivery_failed_reason NVARCHAR(500) NULL,
    created_at             DATETIME2 NOT NULL CONSTRAINT DF_orders_created_at DEFAULT (GETDATE()),
    updated_at             DATETIME2 NULL,
    CONSTRAINT PK_orders PRIMARY KEY (id),
    CONSTRAINT CK_orders_total_nonnegative CHECK (total_amount >= 0),
    CONSTRAINT CK_orders_discount_nonnegative CHECK (discount_amount IS NULL OR discount_amount >= 0),
    CONSTRAINT CK_orders_final_nonnegative CHECK (final_amount >= 0),
    CONSTRAINT FK_orders_user FOREIGN KEY (user_id) REFERENCES dbo.users(id) ON DELETE SET NULL,
    CONSTRAINT FK_orders_shipper FOREIGN KEY (shipper_id) REFERENCES dbo.users(id) ON DELETE NO ACTION
);
CREATE UNIQUE INDEX UQ_orders_idempotency_key ON dbo.orders(idempotency_key) WHERE idempotency_key IS NOT NULL;
GO

-- =============================================================
-- 17. ORDER ITEMS
-- Java: OrderItem.java
-- =============================================================
CREATE TABLE dbo.order_items (
    id            BIGINT IDENTITY(1,1) NOT NULL,
    order_id      BIGINT NULL,
    product_id    BIGINT NULL,
    quantity      INT NOT NULL,
    price         DECIMAL(18,2) NOT NULL,
    product_name  NVARCHAR(255) NOT NULL,
    product_image NVARCHAR(255) NULL,
    CONSTRAINT PK_order_items PRIMARY KEY (id),
    CONSTRAINT CK_order_items_quantity_positive CHECK (quantity > 0),
    CONSTRAINT CK_order_items_price_nonnegative CHECK (price >= 0),
    CONSTRAINT FK_order_items_order FOREIGN KEY (order_id) REFERENCES dbo.orders(id) ON DELETE CASCADE,
    CONSTRAINT FK_order_items_product FOREIGN KEY (product_id) REFERENCES dbo.products(id) ON DELETE SET NULL
);
GO

-- =============================================================
-- 18. PAYMENTS
-- Java: Payment.java
-- =============================================================
CREATE TABLE dbo.payments (
    id              BIGINT IDENTITY(1,1) NOT NULL,
    order_id        BIGINT NULL,
    payment_method  NVARCHAR(50) NOT NULL,
    payment_status  NVARCHAR(30) NOT NULL CONSTRAINT DF_payments_status DEFAULT (N'PENDING'),
    transaction_id  NVARCHAR(100) NULL,
    amount          DECIMAL(18,2) NOT NULL,
    paid_at         DATETIME2 NULL,
    CONSTRAINT PK_payments PRIMARY KEY (id),
    CONSTRAINT UQ_payments_order UNIQUE (order_id),
    CONSTRAINT CK_payments_amount_nonnegative CHECK (amount >= 0),
    CONSTRAINT FK_payments_order FOREIGN KEY (order_id) REFERENCES dbo.orders(id) ON DELETE CASCADE
);
GO

-- =============================================================
-- 19. REVIEWS
-- Java: Review.java
-- =============================================================
CREATE TABLE dbo.reviews (
    id          BIGINT IDENTITY(1,1) NOT NULL,
    product_id  BIGINT NULL,
    user_id     BIGINT NULL,
    rating      INT NOT NULL,
    comment     NVARCHAR(1000) NULL,
    is_approved BIT NOT NULL CONSTRAINT DF_reviews_is_approved DEFAULT (0),
    created_at  DATETIME2 NOT NULL CONSTRAINT DF_reviews_created_at DEFAULT (GETDATE()),
    CONSTRAINT PK_reviews PRIMARY KEY (id),
    CONSTRAINT CK_reviews_rating CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT FK_reviews_product FOREIGN KEY (product_id) REFERENCES dbo.products(id) ON DELETE CASCADE,
    CONSTRAINT FK_reviews_user FOREIGN KEY (user_id) REFERENCES dbo.users(id) ON DELETE SET NULL
);
GO

-- =============================================================
-- 20. COUPONS
-- Java: Coupon.java
-- =============================================================
CREATE TABLE dbo.coupons (
    id                     BIGINT IDENTITY(1,1) NOT NULL,
    code                   NVARCHAR(50) NOT NULL,
    description            NVARCHAR(255) NULL,
    discount_type          NVARCHAR(20) NOT NULL,
    discount_value         DECIMAL(18,2) NOT NULL,
    start_date             DATETIME2 NOT NULL,
    end_date               DATETIME2 NOT NULL,
    min_order_amount       DECIMAL(18,2) NULL,
    used_count             INT NOT NULL CONSTRAINT DF_coupons_used_count DEFAULT (0),
    max_uses               INT NULL CONSTRAINT DF_coupons_max_uses DEFAULT (100),
    is_active              BIT NOT NULL CONSTRAINT DF_coupons_is_active DEFAULT (1),
    applicable_category_id BIGINT NULL,
    max_discount_amount    DECIMAL(18,2) NULL,
    created_at             DATETIME2 NOT NULL CONSTRAINT DF_coupons_created_at DEFAULT (GETDATE()),
    CONSTRAINT PK_coupons PRIMARY KEY (id),
    CONSTRAINT UQ_coupons_code UNIQUE (code),
    CONSTRAINT CK_coupons_discount_value_positive CHECK (discount_value > 0),
    CONSTRAINT CK_coupons_min_order_nonnegative CHECK (min_order_amount IS NULL OR min_order_amount >= 0),
    CONSTRAINT CK_coupons_used_count_nonnegative CHECK (used_count >= 0),
    CONSTRAINT CK_coupons_max_uses_nonnegative CHECK (max_uses IS NULL OR max_uses >= 0),
    CONSTRAINT CK_coupons_max_discount_nonnegative CHECK (max_discount_amount IS NULL OR max_discount_amount >= 0),
    CONSTRAINT FK_coupons_category FOREIGN KEY (applicable_category_id) REFERENCES dbo.categories(id) ON DELETE SET NULL
);
GO

-- =============================================================
-- 21. PAYMENT METHOD CONFIGS
-- Java: PaymentMethodConfig.java -> payment_method_configs
-- =============================================================
CREATE TABLE dbo.payment_method_configs (
    id              BIGINT IDENTITY(1,1) NOT NULL,
    method_key      NVARCHAR(50) NOT NULL,
    name            NVARCHAR(100) NOT NULL,
    description     NVARCHAR(500) NULL,
    is_enabled      BIT NOT NULL CONSTRAINT DF_payment_method_configs_is_enabled DEFAULT (1),
    account_number  NVARCHAR(50) NULL,
    account_holder  NVARCHAR(100) NULL,
    bank_name       NVARCHAR(100) NULL,
    qr_code_url     NVARCHAR(500) NULL,
    transfer_syntax NVARCHAR(200) NULL,
    created_at      DATETIME2(7) NOT NULL CONSTRAINT DF_payment_method_configs_created_at DEFAULT (SYSDATETIME()),
    updated_at      DATETIME2(7) NULL,
    CONSTRAINT PK_payment_method_configs PRIMARY KEY (id),
    CONSTRAINT UQ_payment_method_configs_key UNIQUE (method_key)
);
GO

-- =============================================================

-- =============================================================
-- 22. CUSTOMER MESSAGES
-- Java: CustomerMessage.java
-- =============================================================
CREATE TABLE dbo.customer_messages (
    id                  BIGINT IDENTITY(1,1) NOT NULL,
    user_id             BIGINT NOT NULL,
    sender_type         NVARCHAR(20) NOT NULL,
    sender_name         NVARCHAR(100) NULL,
    message             NVARCHAR(2000) NOT NULL,
    is_read_by_admin    BIT NOT NULL CONSTRAINT DF_customer_messages_is_read_by_admin DEFAULT (0),
    is_read_by_customer BIT NOT NULL CONSTRAINT DF_customer_messages_is_read_by_customer DEFAULT (0),
    created_at          DATETIME2 NOT NULL CONSTRAINT DF_customer_messages_created_at DEFAULT (GETDATE()),
    CONSTRAINT PK_customer_messages PRIMARY KEY (id),
    CONSTRAINT FK_customer_messages_user FOREIGN KEY (user_id) REFERENCES dbo.users(id) ON DELETE CASCADE
);
GO

-- =============================================================
-- 23. ORDER JOBS (Theo dõi tiến trình kỹ thuật của Job trong hàng đợi Valkey)
-- Java: OrderJob.java
-- =============================================================
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
CREATE INDEX IX_order_jobs_order_id ON dbo.order_jobs(order_id);
GO

-- =============================================================
-- 24. OUTBOX EVENTS (Transactional Outbox đảm bảo không mất tin nhắn)
-- Java: OutboxEvent.java
-- =============================================================
CREATE TABLE dbo.outbox_events (
    id             BIGINT IDENTITY(1,1) NOT NULL,
    event_id       NVARCHAR(64) NOT NULL,
    aggregate_type NVARCHAR(50) NOT NULL,      -- ORDER | INVENTORY
    aggregate_id   BIGINT NOT NULL,            -- order_id
    event_type     NVARCHAR(50) NOT NULL,      -- ORDER_CREATED | ORDER_CONFIRMED | ORDER_CANCELLED
    payload        NVARCHAR(MAX) NOT NULL,     -- JSON nội dung sự kiện
    status         NVARCHAR(30) NOT NULL CONSTRAINT DF_outbox_status DEFAULT ('NEW'), -- NEW | PUBLISHED | FAILED
    retry_count    INT NOT NULL CONSTRAINT DF_outbox_retry DEFAULT (0),
    published_at   DATETIME2 NULL,
    created_at     DATETIME2 NOT NULL CONSTRAINT DF_outbox_created DEFAULT (GETDATE()),
    CONSTRAINT PK_outbox_events PRIMARY KEY (id),
    CONSTRAINT UQ_outbox_event_id UNIQUE (event_id)
);
CREATE INDEX IX_outbox_events_status_created ON dbo.outbox_events(status, created_at);
GO

-- 21. INDEXES USED BY COMMON REPOSITORY QUERIES
-- =============================================================
CREATE INDEX IX_users_role_id ON dbo.users(role_id);
CREATE INDEX IX_addresses_user_id ON dbo.addresses(user_id);
CREATE INDEX IX_products_category_id ON dbo.products(category_id);
CREATE INDEX IX_products_brand_id ON dbo.products(brand_id);
CREATE INDEX IX_product_images_product_id ON dbo.product_images(product_id);
CREATE INDEX IX_goods_receipt_supplier_id ON dbo.goods_receipt(supplier_id);
CREATE INDEX IX_goods_receipt_created_by ON dbo.goods_receipt(created_by);
CREATE INDEX IX_goods_receipt_items_receipt_id ON dbo.goods_receipt_items(goods_receipt_id);
CREATE INDEX IX_goods_receipt_items_product_id ON dbo.goods_receipt_items(product_id);
CREATE INDEX IX_product_batches_product_id_expiry ON dbo.product_batches(product_id, expiry_date);
CREATE INDEX IX_product_batches_receipt_id ON dbo.product_batches(goods_receipt_id);
CREATE INDEX IX_inventory_ledger_inventory_created_at ON dbo.inventory_ledger(inventory_id, created_at DESC);
CREATE INDEX IX_inventory_ledger_created_by ON dbo.inventory_ledger(created_by);
CREATE INDEX IX_orders_user_created_at ON dbo.orders(user_id, created_at DESC);
CREATE INDEX IX_orders_shipper_id ON dbo.orders(shipper_id);
CREATE INDEX IX_order_items_order_id ON dbo.order_items(order_id);
CREATE INDEX IX_order_items_product_id ON dbo.order_items(product_id);
CREATE INDEX IX_reviews_product_approved_created ON dbo.reviews(product_id, is_approved, created_at DESC);
CREATE INDEX IX_reviews_user_id ON dbo.reviews(user_id);
CREATE INDEX IX_customer_messages_user_id ON dbo.customer_messages(user_id, created_at DESC);
CREATE INDEX IX_coupons_category_id ON dbo.coupons(applicable_category_id);
GO

-- =============================================================
-- 22. SEED DATA
-- These rows intentionally match the current DataInitializer's
-- categories/brands/products/inventory/coupons and use plain
-- 'pass1234' because current SecurityConfig uses NoOpPasswordEncoder.
-- =============================================================

-- Roles
SET IDENTITY_INSERT dbo.roles ON;
INSERT INTO dbo.roles (id, name) VALUES
(1, N'ROLE_ADMIN'),
(2, N'ROLE_USER'),
(3, N'ROLE_SHIPPER');
SET IDENTITY_INSERT dbo.roles OFF;

-- Users
SET IDENTITY_INSERT dbo.users ON;
INSERT INTO dbo.users
(id, username, email, password, full_name, phone, role_id, is_active, loyalty_points, membership_tier, provider, provider_id, avatar_url, created_at)
VALUES
(1, N'admin',    N'admin@supermarket.com',     N'pass1234', N'Quản Trị Viên',                  N'0912345678', 1, 1, 0,    N'BRONZE',  N'LOCAL', NULL, NULL, GETDATE()),
(2, N'user1',    N'user1@gmail.com',           N'pass1234', N'Nguyễn Văn A',                   N'0987654321', 2, 1, 1250, N'DIAMOND', N'LOCAL', NULL, NULL, GETDATE()),
(3, N'user2',    N'user2@gmail.com',           N'pass1234', N'Trần Thị B',                     N'0977888999', 2, 1, 620,  N'GOLD',    N'LOCAL', NULL, NULL, GETDATE()),
(4, N'shipper1', N'shipper1@supermarket.com', N'pass1234', N'Nguyễn Văn Giao (Quận Tân Phú)', N'0901234567', 3, 1, 0,    N'BRONZE',  N'LOCAL', NULL, NULL, GETDATE()),
(5, N'shipper2', N'shipper2@supermarket.com', N'pass1234', N'Trần Văn Tốc (Quận Tân Bình)',   N'0907654321', 3, 1, 0,    N'BRONZE',  N'LOCAL', NULL, NULL, GETDATE()),
(6, N'shipper3', N'shipper3@supermarket.com', N'pass1234', N'Lê Hoàng Vũ (Quận 12)',          N'0903334455', 3, 1, 0,    N'BRONZE',  N'LOCAL', NULL, NULL, GETDATE()),
(7, N'user3',    N'user3@gmail.com',           N'pass1234', N'Lê Văn Cường',                   N'0933112233', 2, 1, 210,  N'SILVER',  N'LOCAL', NULL, NULL, GETDATE()),
(8, N'user4',    N'user4@gmail.com',           N'pass1234', N'Phạm Thị Dung',                  N'0944556677', 2, 1, 45,   N'BRONZE',  N'LOCAL', NULL, NULL, GETDATE());
SET IDENTITY_INSERT dbo.users OFF;

-- Addresses
SET IDENTITY_INSERT dbo.addresses ON;
INSERT INTO dbo.addresses
(id, user_id, receiver_name, receiver_phone, province, district, ward, detail_address, is_default)
VALUES
(1, 2, N'Nguyễn Văn A', N'0987654321', N'TP Hồ Chí Minh', N'Tân Phú', N'Tây Thạnh', N'Số 120 Tây Thạnh', 1),
(2, 3, N'Trần Thị B', N'0977888999', N'TP Hồ Chí Minh', N'Tân Bình', N'Phường 2', N'45 Bạch Đằng', 1),
(3, 2, N'Nguyễn Văn A', N'0987654321', N'TP Hồ Chí Minh', N'Quận 12', N'Tân Thới Hiệp', N'88 Dương Thị Mười', 0);
SET IDENTITY_INSERT dbo.addresses OFF;

-- Categories: exactly the five categories expected by DataInitializer
SET IDENTITY_INSERT dbo.categories ON;
INSERT INTO dbo.categories (id, name, description, image, is_active, created_at) VALUES
(1, N'Rau Củ Quả', N'Rau củ tươi ngon VietGAP', N'/uploads/cat_rau_cu_qua.jpg', 1, GETDATE()),
(2, N'Thực Phẩm Khô', N'Mì ăn liền, gạo, gia vị', N'/uploads/cat_mi_thuc_pham_kho.jpg', 1, GETDATE()),
(3, N'Sữa & Bơ sữa', N'Sữa tươi tiệt trùng, bơ, sữa chua', N'/uploads/cat_sua_che_pham_sua.jpg', 1, GETDATE()),
(4, N'Đồ Uống', N'Nước ngọt, bia, nước trái cây', N'/uploads/cat_do_uong_giai_khat.jpg', 1, GETDATE()),
(5, N'Hóa Mỹ Phẩm', N'Dầu gội, bột giặt, nước rửa chén', N'/uploads/cat_hoa_pham_gia_dung.jpg', 1, GETDATE());
SET IDENTITY_INSERT dbo.categories OFF;

-- Brands: exactly the five brands expected by DataInitializer
SET IDENTITY_INSERT dbo.brands ON;
INSERT INTO dbo.brands (id, name, description, is_active, created_at) VALUES
(1, N'VietGAP Farm', N'Rau sạch tiêu chuẩn VietGAP', 1, GETDATE()),
(2, N'Acecook', N'Mì gói ăn liền Hảo Hảo', 1, GETDATE()),
(3, N'Vinamilk', N'Sữa tươi, sữa chua quốc dân', 1, GETDATE()),
(4, N'Coca Cola', N'Thương hiệu giải khát', 1, GETDATE()),
(5, N'Unilever', N'Clear, Sunlight, Omo', 1, GETDATE());
SET IDENTITY_INSERT dbo.brands OFF;

-- Products: exactly the ten products created by DataInitializer
SET IDENTITY_INSERT dbo.products ON;
INSERT INTO dbo.products
(id, name, description, price, sale_price, category_id, brand_id, main_image, sku, barcode, unit, weight_g, is_active, created_at, updated_at)
VALUES
(1, N'Xà lách thủy canh sạch', N'Xà lách tươi ngon được trồng theo phương pháp thủy canh, sạch sẽ an toàn.', 25000.00, 20000.00, 1, 1, N'/uploads/xa_lach_thuy_canh.jpg',  N'SP001', N'8931234567901', N'Kg',   1000, 1, GETDATE(), NULL),
(2, N'Cà chua VietGAP 1kg', N'Cà chua đỏ chín tự nhiên, nhiều dinh dưỡng, không hóa chất bảo quản.', 35000.00, 30000.00, 1, 1, N'/uploads/ca_chua_dalat.jpg',  N'SP002', N'8931234567902', N'Kg',   1000, 1, GETDATE(), NULL),
(3, N'Mì Hảo Hảo Tôm Chua Cay', N'Mì ăn liền Hảo Hảo hương vị tôm chua cay truyền thống, thùng 30 gói.', 135000.00, 128000.00, 2, 2, N'/uploads/mi_hao_hao.jpg', N'SP003', N'8931234567903', N'Thùng', 3000, 1, GETDATE(), NULL),
(4, N'Dầu ăn Simply Đậu Nành 1L', N'Dầu ănSimply 100% nguyên chất từ hạt đậu nành chọn lọc, tốt cho tim mạch.', 62000.00, NULL, 2, 5, N'/uploads/dau_an_simply.jpg', N'SP004', N'8931234567904', N'Chai',  1000, 1, GETDATE(), NULL),
(5, N'Sữa tươi Vinamilk ít đường 1L', N'Sữa tươi tiệt trùng Vinamilk bổ sung vitamin AD3 giúp xương chắc khỏe.', 38000.00, 36000.00, 3, 3, N'/uploads/sua_vinamilk_1l.jpg', N'SP005', N'8931234567905', N'Hộp',  1000, 1, GETDATE(), NULL),
(6, N'Sữa chua Vinamilk có đường hộp 100g', N'Sữa chua ăn Vinamilk thơm ngon tự nhiên, hỗ trợ tiêu hóa tốt.', 8000.00, NULL, 3, 3, N'/uploads/sua_chua_vinamilk.jpg', N'SP006', N'8931234567906', N'Hộp',   100, 1, GETDATE(), NULL),
(7, N'Nước ngọt Coca Cola lon 320ml', N'Nước giải khát có ga Coca Cola sảng khoái cực độ.', 11000.00, 10000.00, 4, 4, N'/uploads/coca_cola.jpg', N'SP007', N'8931234567907', N'Lon',    320, 1, GETDATE(), NULL),
(8, N'Bia Heineken lon 330ml', N'Bia Heineken Premium chất lượng thượng hạng từ Hà Lan.', 22000.00, 21000.00, 4, 4, N'/uploads/bia_heineken.jpg', N'SP008', N'8931234567908', N'Lon',    330, 1, GETDATE(), NULL),
(9, N'Dầu gội Clear Bạc Hà Mát Lạnh 630ml', N'Dầu gội sạch gàu số 1 Việt Nam với tinh chất bạc hà.', 175000.00, 160000.00, 5, 5, N'/uploads/dau_goi_clear.webp', N'SP009', N'8931234567909', N'Chai',   630, 1, GETDATE(), NULL),
(10, N'Nước lau sàn Sunlight Hoa Lilia 1kg', N'Nước lau sàn Sunlight hương hoa Lilia thơm ngát, sạch bóng.', 32000.00, NULL, 5, 5, N'/uploads/nuoc_lau_san_sunlight.jpg', N'SP010', N'8931234567910', N'Chai',  1000, 1, GETDATE(), NULL);
SET IDENTITY_INSERT dbo.products OFF;

-- Inventory: exactly aligned to DataInitializer
SET IDENTITY_INSERT dbo.inventory ON;
INSERT INTO dbo.inventory
(id, product_id, current_stock, minimum_stock, location, last_updated, created_at)
VALUES
(1,  1,  4, 5, N'Kệ A1 - Rau củ', GETDATE(), GETDATE()),
(2,  2, 25, 5, N'Kệ A2 - Rau củ', GETDATE(), GETDATE()),
(3,  3,  3, 5, N'Kệ B1 - Hàng khô', GETDATE(), GETDATE()),
(4,  4, 50, 5, N'Kệ B2 - Hàng khô', GETDATE(), GETDATE()),
(5,  5, 12, 5, N'Tủ lạnh 1 - Sữa', GETDATE(), GETDATE()),
(6,  6,100, 5, N'Tủ lạnh 1 - Sữa', GETDATE(), GETDATE()),
(7,  7,120, 5, N'Kệ C1 - Đồ uống', GETDATE(), GETDATE()),
(8,  8, 60, 5, N'Kệ C2 - Đồ uống', GETDATE(), GETDATE()),
(9,  9,  2, 5, N'Kệ D1 - Hóa phẩm', GETDATE(), GETDATE()),
(10,10, 30, 5, N'Kệ D2 - Hóa phẩm', GETDATE(), GETDATE());
SET IDENTITY_INSERT dbo.inventory OFF;

-- Product images
INSERT INTO dbo.product_images (product_id, image_path, sort_order) VALUES
(1, N'/uploads/xa_lach_thuy_canh.jpg', 0),
(2, N'/uploads/ca_chua_dalat.jpg', 0),
(3, N'/uploads/mi_hao_hao.jpg', 0),
(4, N'/uploads/dau_an_simply.jpg', 0),
(5, N'/uploads/sua_vinamilk_1l.jpg', 0),
(6, N'/uploads/sua_chua_vinamilk.jpg', 0),
(7, N'/uploads/coca_cola.jpg', 0),
(8, N'/uploads/bia_heineken.jpg', 0),
(9, N'/uploads/dau_goi_clear.webp', 0),
(10, N'/uploads/nuoc_lau_san_sunlight.jpg', 0);

-- Coupons: exactly compatible with Coupon.java and DataInitializer
SET IDENTITY_INSERT dbo.coupons ON;
INSERT INTO dbo.coupons
(id, code, description, discount_type, discount_value, start_date, end_date, min_order_amount, used_count, max_uses, is_active, applicable_category_id, max_discount_amount, created_at)
VALUES
(1, N'WELCOME10', N'Giảm 10% cho đơn hàng đầu tiên (Tối đa 50k)', N'PERCENTAGE', 10.00, DATEADD(DAY,-1,GETDATE()), DATEADD(YEAR,1,GETDATE()), 100000.00, 0, 1000, 1, NULL, 50000.00, GETDATE()),
(2, N'RAUXANH15', N'Giảm 15% cho danh mục Rau Củ Quả VietGAP (Tối đa 25k)', N'PERCENTAGE', 15.00, DATEADD(DAY,-1,GETDATE()), DATEADD(YEAR,1,GETDATE()), 30000.00, 0, 500, 1, 1, 25000.00, GETDATE()),
(3, N'KHO20K', N'Giảm 20.000đ cho Thực Phẩm Khô & Gia vị từ 100k', N'FIXED_AMOUNT', 20000.00, DATEADD(DAY,-1,GETDATE()), DATEADD(YEAR,1,GETDATE()), 100000.00, 0, 300, 1, 2, NULL, GETDATE()),
(4, N'FREESHIP', N'Miễn phí vận chuyển cho mọi đơn hàng', N'FIXED_AMOUNT', 15000.00, DATEADD(DAY,-1,GETDATE()), DATEADD(YEAR,1,GETDATE()), 0.00, 0, 500, 1, NULL, NULL, GETDATE()),
(5, N'MINI50K', N'Siêu voucher giảm 50.000đ cho đơn từ 500.000đ', N'FIXED_AMOUNT', 50000.00, DATEADD(DAY,-1,GETDATE()), DATEADD(YEAR,1,GETDATE()), 500000.00, 0, 200, 1, NULL, NULL, GETDATE());
SET IDENTITY_INSERT dbo.coupons OFF;

-- Reviews
INSERT INTO dbo.reviews (product_id, user_id, rating, comment, is_approved, created_at) VALUES
(1, 2, 5, N'Rau rất tươi xanh, đóng gói sạch sẽ.', 1, GETDATE()),
(1, 3, 4, N'Giao hàng nhanh, rau tươi ngon.', 1, GETDATE()),
(3, 2, 5, N'Mì Hảo Hảo ăn ngon, date xa.', 1, GETDATE());

-- =============================================================
-- 23. OPTIONAL DEMO DATA FOR GOODS RECEIPTS / BATCHES / LEDGER
-- =============================================================
INSERT INTO dbo.suppliers (name, contact_name, phone, email, address, is_active) VALUES
(N'Công ty TNHH Nông Sản Xanh', N'Nguyễn Minh Khoa', N'0909000001', N'nongsanxanh@example.com', N'TP. Hồ Chí Minh', 1),
(N'Công ty TNHH Phân Phối FMCG Việt', N'Trần Quốc Huy', N'0909000002', N'fmcg@example.com', N'Bình Dương', 1);

INSERT INTO dbo.goods_receipt (supplier_id, total_amount, note, status, created_by) VALUES
(1, 2800000.00, N'Nhập rau củ và sữa', N'COMPLETED', 1),
(2, 3440000.00, N'Nhập hàng khô và đồ uống', N'COMPLETED', 1);

INSERT INTO dbo.goods_receipt_items (goods_receipt_id, product_id, quantity, import_price, batch_name, expiry_date, passed_quantity, rejected_quantity, qc_status, inspected_at, inspected_by) VALUES
(1, 1, 100, 15000.00, N'LH2026A', DATEADD(DAY,30,CAST(GETDATE() AS DATE)), 100, 0, N'PASSED', GETDATE(), 1),
(1, 2,  80, 20000.00, N'CT2026A', DATEADD(DAY,30,CAST(GETDATE() AS DATE)),  80, 0, N'PASSED', GETDATE(), 1),
(1, 5,  50, 28000.00, N'ST2026A', DATEADD(DAY,180,CAST(GETDATE() AS DATE)), 50, 0, N'PASSED', GETDATE(), 1),
(2, 3,  20, 110000.00, N'MH2026A', DATEADD(DAY,300,CAST(GETDATE() AS DATE)), 20, 0, N'PASSED', GETDATE(), 1),
(2, 4,  30, 50000.00, N'DA2026A', DATEADD(DAY,365,CAST(GETDATE() AS DATE)), 30, 0, N'PASSED', GETDATE(), 1),
(2, 7,  50, 8000.00, N'CC2026A', DATEADD(DAY,180,CAST(GETDATE() AS DATE)), 50, 0, N'PASSED', GETDATE(), 1);

INSERT INTO dbo.product_batches (product_id, goods_receipt_id, batch_name, quantity, expiry_date, import_price, sale_price, status) VALUES
(1, 1, N'LH2026A',      4, DATEADD(DAY, 15, CAST(GETDATE() AS DATE)), 15000.00, NULL,     N'ACTIVE'),
(2, 1, N'CT2026_SALE',  5, DATEADD(DAY,  4, CAST(GETDATE() AS DATE)), 20000.00, 25000.00, N'ACTIVE'),
(2, 1, N'CT2026A',     20, DATEADD(DAY, 30, CAST(GETDATE() AS DATE)), 20000.00, NULL,     N'ACTIVE'),
(3, 2, N'MH2026A',      3, DATEADD(DAY,300, CAST(GETDATE() AS DATE)), 110000.00, NULL,    N'ACTIVE'),
(4, 2, N'DA2026A',     50, DATEADD(DAY,365, CAST(GETDATE() AS DATE)), 50000.00, NULL,     N'ACTIVE'),
(5, 1, N'ST2026A',     12, DATEADD(DAY,180, CAST(GETDATE() AS DATE)), 28000.00, NULL,     N'ACTIVE'),
(5, 1, N'ST2026_EXP',   3, DATEADD(DAY, -5, CAST(GETDATE() AS DATE)), 28000.00, NULL,     N'ACTIVE'),
(5, 1, N'ST2025_DISP',  0, DATEADD(DAY,-25, CAST(GETDATE() AS DATE)), 28000.00, NULL,     N'DISPOSED'),
(7, 2, N'CC2026A',    120, DATEADD(DAY,180, CAST(GETDATE() AS DATE)), 8000.00,  NULL,     N'ACTIVE');

INSERT INTO dbo.inventory_ledger
(inventory_id, change_type, quantity_change, previous_stock, new_stock, reference_id, note, created_by)
VALUES
(1, N'IMPORT',           100,  0, 100, 1,    N'Nhập lô LH2026A', 1),
(1, N'SELL',             -96, 100,  4, NULL, N'Đồng bộ tồn kho bán hàng', 1),
(2, N'IMPORT',            80,  0,  80, 1,    N'Nhập lô CT2026A & CT2026_SALE', 1),
(2, N'MANUAL_ADJUST',    -55, 80,  25, NULL, N'Điều chỉnh kiểm kê', 1),
(3, N'IMPORT',            20,  0,  20, 2,    N'Nhập lô MH2026A', 1),
(3, N'MANUAL_ADJUST',    -17, 20,   3, NULL, N'Điều chỉnh kiểm kê', 1),
(5, N'EXPIRED_DISPOSAL',  -2, 14,  12, NULL, N'Xuất hủy lô quá hạn ST2025_DISP (2 hộp)', 1);


-- Customer Messages Seed
SET IDENTITY_INSERT dbo.customer_messages ON;
INSERT INTO dbo.customer_messages
(id, user_id, sender_type, sender_name, message, is_read_by_admin, is_read_by_customer, created_at)
VALUES
(1, 2, N'CUSTOMER', N'Nguyễn Văn A', N'Xin chào MiniMart, mình muốn hỏi về nguồn gốc xuất xứ của rau xà lách sạch?', 1, 1, DATEADD(DAY,-2,GETDATE())),
(2, 2, N'ADMIN', N'Quản Trị Viên', N'Dạ chào anh A, rau xà lách của MiniMart được trồng theo tiêu chuẩn VietGAP thủy canh tại Đà Lạt ạ!', 1, 1, DATEADD(DAY,-2,DATEADD(MINUTE,15,GETDATE()))),
(3, 3, N'CUSTOMER', N'Trần Thị B', N'Shop ơi mình vừa đặt đơn hàng giao qua Tân Bình thì khoảng bao lâu nhận được ạ?', 1, 1, DATEADD(HOUR,-3,GETDATE())),
(4, 3, N'ADMIN', N'Quản Trị Viên', N'Dạ Shipper đang chuẩn bị đơn và sẽ giao đến bạn trong vòng 2 tiếng nhé!', 1, 1, DATEADD(HOUR,-2,GETDATE()));
SET IDENTITY_INSERT dbo.customer_messages OFF;

-- =============================================================
-- 24. DEMO CART
-- =============================================================
INSERT INTO dbo.cart (user_id) VALUES (2), (3);
INSERT INTO dbo.cart_items (cart_id, product_id, quantity) VALUES
(1, 5, 2),
(1, 7, 3),
(2, 1, 1);

-- =============================================================
-- 25. 50 HISTORICAL ORDERS & BASKETS (TP.HCM: TÂN PHÚ, TÂN BÌNH, QUẬN 12)
-- Cung cấp đầy đủ dữ liệu giao dịch cho Thuật toán Apriori tính Confidence & Lift
SET IDENTITY_INSERT dbo.orders ON;
INSERT INTO dbo.orders
(id, user_id, shipper_id, total_amount, discount_amount, final_amount, status, shipping_name, shipping_phone, shipping_address, payment_method, coupon_code, note, delivery_note, delivered_at, delivery_failed_reason, created_at, updated_at)
VALUES
(1, 2, 4, 224000.00, 0.00, 224000.00, N'HOAN_THANH', N'Nguyễn Văn A', N'0987654321', N'123 Lũy Bán Bích, Phường Hòa Thạnh, Quận Tân Phú, TP. Hồ Chí Minh', N'MOMO', NULL, N'Đơn hàng mẫu 1', N'Giao hàng tận nơi thành công', DATEADD(HOUR, -1715, GETDATE()), NULL, DATEADD(HOUR, -1717, GETDATE()), NULL),
(2, 3, 4, 138000.00, 0.00, 138000.00, N'HOAN_THANH', N'Trần Thị B', N'0977888999', N'45 Thoại Ngọc Hầu, Phường Phú Thạnh, Quận Tân Phú, TP. Hồ Chí Minh', N'COD', NULL, N'Đơn hàng mẫu 2', N'Giao hàng tận nơi thành công', DATEADD(HOUR, -1680, GETDATE()), NULL, DATEADD(HOUR, -1682, GETDATE()), NULL),
(3, 7, 4, 88000.00, 0.00, 88000.00, N'HOAN_THANH', N'Lê Văn Cường', N'0933112233', N'88 Tân Kỳ Tân Quý, Phường Tân Sơn Nhì, Quận Tân Phú, TP. Hồ Chí Minh', N'BANK_TRANSFER', NULL, N'Đơn hàng mẫu 3', N'Giao hàng tận nơi thành công', DATEADD(HOUR, -1645, GETDATE()), NULL, DATEADD(HOUR, -1647, GETDATE()), NULL),
(4, 8, 4, 50000.00, 30000.00, 20000.00, N'HOAN_THANH', N'Phạm Thị Dung', N'0944556677', N'15 Lê Trọng Tấn, Phường Tây Thạnh, Quận Tân Phú, TP. Hồ Chí Minh', N'MOMO', N'WELCOME10', N'Đơn hàng mẫu 4', N'Giao hàng tận nơi thành công', DATEADD(HOUR, -1610, GETDATE()), NULL, DATEADD(HOUR, -1612, GETDATE()), NULL),
(5, 2, 4, 318000.00, 0.00, 318000.00, N'HOAN_THANH', N'Nguyễn Văn A', N'0987654321', N'210 Vườn Lài, Phường Phú Thọ Hòa, Quận Tân Phú, TP. Hồ Chí Minh', N'COD', NULL, N'Đơn hàng mẫu 5', N'Giao hàng tận nơi thành công', DATEADD(HOUR, -1575, GETDATE()), NULL, DATEADD(HOUR, -1577, GETDATE()), NULL),
(6, 3, 5, 192000.00, 0.00, 192000.00, N'HOAN_THANH', N'Trần Thị B', N'0977888999', N'45 Cộng Hòa, Phường 13, Quận Tân Bình, TP. Hồ Chí Minh', N'BANK_TRANSFER', NULL, N'Đơn hàng mẫu 6', N'Giao hàng tận nơi thành công', DATEADD(HOUR, -1540, GETDATE()), NULL, DATEADD(HOUR, -1542, GETDATE()), NULL),
(7, 7, 5, 172000.00, 0.00, 172000.00, N'HOAN_THANH', N'Lê Văn Cường', N'0933112233', N'202 Hoàng Văn Thụ, Phường 2, Quận Tân Bình, TP. Hồ Chí Minh', N'MOMO', NULL, N'Đơn hàng mẫu 7', N'Giao hàng tận nơi thành công', DATEADD(HOUR, -1505, GETDATE()), NULL, DATEADD(HOUR, -1507, GETDATE()), NULL),
(8, 8, 5, 190000.00, 25000.00, 165000.00, N'HOAN_THANH', N'Phạm Thị Dung', N'0944556677', N'73 Trường Chinh, Phường 12, Quận Tân Bình, TP. Hồ Chí Minh', N'COD', N'RAUXANH15', N'Đơn hàng mẫu 8', N'Giao hàng tận nơi thành công', DATEADD(HOUR, -1470, GETDATE()), NULL, DATEADD(HOUR, -1472, GETDATE()), NULL),
(9, 2, 5, 108000.00, 0.00, 108000.00, N'HOAN_THANH', N'Nguyễn Văn A', N'0987654321', N'180 Phổ Quang, Phường 2, Quận Tân Bình, TP. Hồ Chí Minh', N'BANK_TRANSFER', NULL, N'Đơn hàng mẫu 9', N'Giao hàng tận nơi thành công', DATEADD(HOUR, -1435, GETDATE()), NULL, DATEADD(HOUR, -1437, GETDATE()), NULL),
(10, 3, 5, 124000.00, 0.00, 124000.00, N'GIAO_THAT_BAI', N'Trần Thị B', N'0977888999', N'56 Bạch Đằng, Phường 2, Quận Tân Bình, TP. Hồ Chí Minh', N'MOMO', NULL, N'Đơn hàng mẫu 10', NULL, NULL, N'Khách hàng bận không nghe máy', DATEADD(HOUR, -1402, GETDATE()), NULL),
(11, 7, 6, 224000.00, 0.00, 224000.00, N'HOAN_THANH', N'Lê Văn Cường', N'0933112233', N'78 Lê Văn Khương, Phường Thới An, Quận 12, TP. Hồ Chí Minh', N'COD', NULL, N'Đơn hàng mẫu 11', N'Giao hàng tận nơi thành công', DATEADD(HOUR, -1365, GETDATE()), NULL, DATEADD(HOUR, -1367, GETDATE()), NULL),
(12, 8, 6, 138000.00, 30000.00, 108000.00, N'HOAN_THANH', N'Phạm Thị Dung', N'0944556677', N'105 Nguyễn Ảnh Thủ, Phường Hiệp Thành, Quận 12, TP. Hồ Chí Minh', N'BANK_TRANSFER', N'WELCOME10', N'Đơn hàng mẫu 12', N'Giao hàng tận nơi thành công', DATEADD(HOUR, -1330, GETDATE()), NULL, DATEADD(HOUR, -1332, GETDATE()), NULL),
(13, 2, 6, 88000.00, 0.00, 88000.00, N'HOAN_THANH', N'Nguyễn Văn A', N'0987654321', N'340 Hà Huy Giáp, Phường Thạnh Lộc, Quận 12, TP. Hồ Chí Minh', N'MOMO', NULL, N'Đơn hàng mẫu 13', N'Giao hàng tận nơi thành công', DATEADD(HOUR, -1295, GETDATE()), NULL, DATEADD(HOUR, -1297, GETDATE()), NULL),
(14, 3, 6, 50000.00, 0.00, 50000.00, N'HOAN_THANH', N'Trần Thị B', N'0977888999', N'92 Tô Ký, Phường Tân Chánh Hiệp, Quận 12, TP. Hồ Chí Minh', N'COD', NULL, N'Đơn hàng mẫu 14', N'Giao hàng tận nơi thành công', DATEADD(HOUR, -1260, GETDATE()), NULL, DATEADD(HOUR, -1262, GETDATE()), NULL),
(15, 7, NULL, 318000.00, 0.00, 318000.00, N'HUY', N'Lê Văn Cường', N'0933112233', N'15 Quốc Lộ 1A, Phường An Phú Đông, Quận 12, TP. Hồ Chí Minh', N'BANK_TRANSFER', NULL, N'Đơn hàng mẫu 15', NULL, NULL, NULL, DATEADD(HOUR, -1227, GETDATE()), NULL),
(16, 8, 4, 192000.00, 25000.00, 167000.00, N'HOAN_THANH', N'Phạm Thị Dung', N'0944556677', N'123 Lũy Bán Bích, Phường Hòa Thạnh, Quận Tân Phú, TP. Hồ Chí Minh', N'MOMO', N'RAUXANH15', N'Đơn hàng mẫu 16', N'Giao hàng tận nơi thành công', DATEADD(HOUR, -1190, GETDATE()), NULL, DATEADD(HOUR, -1192, GETDATE()), NULL),
(17, 2, 4, 172000.00, 0.00, 172000.00, N'HOAN_THANH', N'Nguyễn Văn A', N'0987654321', N'45 Thoại Ngọc Hầu, Phường Phú Thạnh, Quận Tân Phú, TP. Hồ Chí Minh', N'COD', NULL, N'Đơn hàng mẫu 17', N'Giao hàng tận nơi thành công', DATEADD(HOUR, -1155, GETDATE()), NULL, DATEADD(HOUR, -1157, GETDATE()), NULL),
(18, 3, 4, 190000.00, 0.00, 190000.00, N'HOAN_THANH', N'Trần Thị B', N'0977888999', N'88 Tân Kỳ Tân Quý, Phường Tân Sơn Nhì, Quận Tân Phú, TP. Hồ Chí Minh', N'BANK_TRANSFER', NULL, N'Đơn hàng mẫu 18', N'Giao hàng tận nơi thành công', DATEADD(HOUR, -1120, GETDATE()), NULL, DATEADD(HOUR, -1122, GETDATE()), NULL),
(19, 7, 4, 108000.00, 0.00, 108000.00, N'HOAN_THANH', N'Lê Văn Cường', N'0933112233', N'15 Lê Trọng Tấn, Phường Tây Thạnh, Quận Tân Phú, TP. Hồ Chí Minh', N'MOMO', NULL, N'Đơn hàng mẫu 19', N'Giao hàng tận nơi thành công', DATEADD(HOUR, -1085, GETDATE()), NULL, DATEADD(HOUR, -1087, GETDATE()), NULL),
(20, 8, 4, 124000.00, 30000.00, 94000.00, N'HOAN_THANH', N'Phạm Thị Dung', N'0944556677', N'210 Vườn Lài, Phường Phú Thọ Hòa, Quận Tân Phú, TP. Hồ Chí Minh', N'COD', N'WELCOME10', N'Đơn hàng mẫu 20', N'Giao hàng tận nơi thành công', DATEADD(HOUR, -1050, GETDATE()), NULL, DATEADD(HOUR, -1052, GETDATE()), NULL),
(21, 2, 5, 224000.00, 0.00, 224000.00, N'HOAN_THANH', N'Nguyễn Văn A', N'0987654321', N'45 Cộng Hòa, Phường 13, Quận Tân Bình, TP. Hồ Chí Minh', N'BANK_TRANSFER', NULL, N'Đơn hàng mẫu 21', N'Giao hàng tận nơi thành công', DATEADD(HOUR, -1015, GETDATE()), NULL, DATEADD(HOUR, -1017, GETDATE()), NULL),
(22, 3, 5, 138000.00, 0.00, 138000.00, N'HOAN_THANH', N'Trần Thị B', N'0977888999', N'202 Hoàng Văn Thụ, Phường 2, Quận Tân Bình, TP. Hồ Chí Minh', N'MOMO', NULL, N'Đơn hàng mẫu 22', N'Giao hàng tận nơi thành công', DATEADD(HOUR, -980, GETDATE()), NULL, DATEADD(HOUR, -982, GETDATE()), NULL),
(23, 7, 5, 88000.00, 0.00, 88000.00, N'HOAN_THANH', N'Lê Văn Cường', N'0933112233', N'73 Trường Chinh, Phường 12, Quận Tân Bình, TP. Hồ Chí Minh', N'COD', NULL, N'Đơn hàng mẫu 23', N'Giao hàng tận nơi thành công', DATEADD(HOUR, -945, GETDATE()), NULL, DATEADD(HOUR, -947, GETDATE()), NULL),
(24, 8, 5, 50000.00, 25000.00, 25000.00, N'HOAN_THANH', N'Phạm Thị Dung', N'0944556677', N'180 Phổ Quang, Phường 2, Quận Tân Bình, TP. Hồ Chí Minh', N'BANK_TRANSFER', N'RAUXANH15', N'Đơn hàng mẫu 24', N'Giao hàng tận nơi thành công', DATEADD(HOUR, -910, GETDATE()), NULL, DATEADD(HOUR, -912, GETDATE()), NULL),
(25, 2, 5, 318000.00, 0.00, 318000.00, N'HOAN_THANH', N'Nguyễn Văn A', N'0987654321', N'56 Bạch Đằng, Phường 2, Quận Tân Bình, TP. Hồ Chí Minh', N'MOMO', NULL, N'Đơn hàng mẫu 25', N'Giao hàng tận nơi thành công', DATEADD(HOUR, -875, GETDATE()), NULL, DATEADD(HOUR, -877, GETDATE()), NULL),
(26, 3, 6, 192000.00, 0.00, 192000.00, N'HOAN_THANH', N'Trần Thị B', N'0977888999', N'78 Lê Văn Khương, Phường Thới An, Quận 12, TP. Hồ Chí Minh', N'COD', NULL, N'Đơn hàng mẫu 26', N'Giao hàng tận nơi thành công', DATEADD(HOUR, -840, GETDATE()), NULL, DATEADD(HOUR, -842, GETDATE()), NULL),
(27, 7, 6, 172000.00, 0.00, 172000.00, N'HOAN_THANH', N'Lê Văn Cường', N'0933112233', N'105 Nguyễn Ảnh Thủ, Phường Hiệp Thành, Quận 12, TP. Hồ Chí Minh', N'BANK_TRANSFER', NULL, N'Đơn hàng mẫu 27', N'Giao hàng tận nơi thành công', DATEADD(HOUR, -805, GETDATE()), NULL, DATEADD(HOUR, -807, GETDATE()), NULL),
(28, 8, 6, 190000.00, 30000.00, 160000.00, N'HOAN_THANH', N'Phạm Thị Dung', N'0944556677', N'340 Hà Huy Giáp, Phường Thạnh Lộc, Quận 12, TP. Hồ Chí Minh', N'MOMO', N'WELCOME10', N'Đơn hàng mẫu 28', N'Giao hàng tận nơi thành công', DATEADD(HOUR, -770, GETDATE()), NULL, DATEADD(HOUR, -772, GETDATE()), NULL),
(29, 2, 6, 108000.00, 0.00, 108000.00, N'HOAN_THANH', N'Nguyễn Văn A', N'0987654321', N'92 Tô Ký, Phường Tân Chánh Hiệp, Quận 12, TP. Hồ Chí Minh', N'COD', NULL, N'Đơn hàng mẫu 29', N'Giao hàng tận nơi thành công', DATEADD(HOUR, -735, GETDATE()), NULL, DATEADD(HOUR, -737, GETDATE()), NULL),
(30, 3, 6, 124000.00, 0.00, 124000.00, N'HOAN_THANH', N'Trần Thị B', N'0977888999', N'15 Quốc Lộ 1A, Phường An Phú Đông, Quận 12, TP. Hồ Chí Minh', N'BANK_TRANSFER', NULL, N'Đơn hàng mẫu 30', N'Giao hàng tận nơi thành công', DATEADD(HOUR, -700, GETDATE()), NULL, DATEADD(HOUR, -702, GETDATE()), NULL),
(31, 7, 4, 224000.00, 0.00, 224000.00, N'HOAN_THANH', N'Lê Văn Cường', N'0933112233', N'123 Lũy Bán Bích, Phường Hòa Thạnh, Quận Tân Phú, TP. Hồ Chí Minh', N'MOMO', NULL, N'Đơn hàng mẫu 31', N'Giao hàng tận nơi thành công', DATEADD(HOUR, -665, GETDATE()), NULL, DATEADD(HOUR, -667, GETDATE()), NULL),
(32, 8, 4, 138000.00, 25000.00, 113000.00, N'HOAN_THANH', N'Phạm Thị Dung', N'0944556677', N'45 Thoại Ngọc Hầu, Phường Phú Thạnh, Quận Tân Phú, TP. Hồ Chí Minh', N'COD', N'RAUXANH15', N'Đơn hàng mẫu 32', N'Giao hàng tận nơi thành công', DATEADD(HOUR, -630, GETDATE()), NULL, DATEADD(HOUR, -632, GETDATE()), NULL),
(33, 2, 4, 88000.00, 0.00, 88000.00, N'HOAN_THANH', N'Nguyễn Văn A', N'0987654321', N'88 Tân Kỳ Tân Quý, Phường Tân Sơn Nhì, Quận Tân Phú, TP. Hồ Chí Minh', N'BANK_TRANSFER', NULL, N'Đơn hàng mẫu 33', N'Giao hàng tận nơi thành công', DATEADD(HOUR, -595, GETDATE()), NULL, DATEADD(HOUR, -597, GETDATE()), NULL),
(34, 3, 4, 50000.00, 0.00, 50000.00, N'HOAN_THANH', N'Trần Thị B', N'0977888999', N'15 Lê Trọng Tấn, Phường Tây Thạnh, Quận Tân Phú, TP. Hồ Chí Minh', N'MOMO', NULL, N'Đơn hàng mẫu 34', N'Giao hàng tận nơi thành công', DATEADD(HOUR, -560, GETDATE()), NULL, DATEADD(HOUR, -562, GETDATE()), NULL),
(35, 7, NULL, 318000.00, 0.00, 318000.00, N'HUY', N'Lê Văn Cường', N'0933112233', N'210 Vườn Lài, Phường Phú Thọ Hòa, Quận Tân Phú, TP. Hồ Chí Minh', N'COD', NULL, N'Đơn hàng mẫu 35', NULL, NULL, NULL, DATEADD(HOUR, -527, GETDATE()), NULL),
(36, 8, 5, 192000.00, 30000.00, 162000.00, N'HOAN_THANH', N'Phạm Thị Dung', N'0944556677', N'45 Cộng Hòa, Phường 13, Quận Tân Bình, TP. Hồ Chí Minh', N'BANK_TRANSFER', N'WELCOME10', N'Đơn hàng mẫu 36', N'Giao hàng tận nơi thành công', DATEADD(HOUR, -490, GETDATE()), NULL, DATEADD(HOUR, -492, GETDATE()), NULL),
(37, 2, 5, 172000.00, 0.00, 172000.00, N'HOAN_THANH', N'Nguyễn Văn A', N'0987654321', N'202 Hoàng Văn Thụ, Phường 2, Quận Tân Bình, TP. Hồ Chí Minh', N'MOMO', NULL, N'Đơn hàng mẫu 37', N'Giao hàng tận nơi thành công', DATEADD(HOUR, -455, GETDATE()), NULL, DATEADD(HOUR, -457, GETDATE()), NULL),
(38, 3, 5, 190000.00, 0.00, 190000.00, N'HOAN_THANH', N'Trần Thị B', N'0977888999', N'73 Trường Chinh, Phường 12, Quận Tân Bình, TP. Hồ Chí Minh', N'COD', NULL, N'Đơn hàng mẫu 38', N'Giao hàng tận nơi thành công', DATEADD(HOUR, -420, GETDATE()), NULL, DATEADD(HOUR, -422, GETDATE()), NULL),
(39, 7, 5, 108000.00, 0.00, 108000.00, N'HOAN_THANH', N'Lê Văn Cường', N'0933112233', N'180 Phổ Quang, Phường 2, Quận Tân Bình, TP. Hồ Chí Minh', N'BANK_TRANSFER', NULL, N'Đơn hàng mẫu 39', N'Giao hàng tận nơi thành công', DATEADD(HOUR, -385, GETDATE()), NULL, DATEADD(HOUR, -387, GETDATE()), NULL),
(40, 8, 5, 124000.00, 25000.00, 99000.00, N'HOAN_THANH', N'Phạm Thị Dung', N'0944556677', N'56 Bạch Đằng, Phường 2, Quận Tân Bình, TP. Hồ Chí Minh', N'MOMO', N'RAUXANH15', N'Đơn hàng mẫu 40', N'Giao hàng tận nơi thành công', DATEADD(HOUR, -350, GETDATE()), NULL, DATEADD(HOUR, -352, GETDATE()), NULL),
(41, 2, 6, 224000.00, 0.00, 224000.00, N'HOAN_THANH', N'Nguyễn Văn A', N'0987654321', N'78 Lê Văn Khương, Phường Thới An, Quận 12, TP. Hồ Chí Minh', N'COD', NULL, N'Đơn hàng mẫu 41', N'Giao hàng tận nơi thành công', DATEADD(HOUR, -315, GETDATE()), NULL, DATEADD(HOUR, -317, GETDATE()), NULL),
(42, 3, 6, 138000.00, 0.00, 138000.00, N'HOAN_THANH', N'Trần Thị B', N'0977888999', N'105 Nguyễn Ảnh Thủ, Phường Hiệp Thành, Quận 12, TP. Hồ Chí Minh', N'BANK_TRANSFER', NULL, N'Đơn hàng mẫu 42', N'Giao hàng tận nơi thành công', DATEADD(HOUR, -280, GETDATE()), NULL, DATEADD(HOUR, -282, GETDATE()), NULL),
(43, 7, 6, 88000.00, 0.00, 88000.00, N'HOAN_THANH', N'Lê Văn Cường', N'0933112233', N'340 Hà Huy Giáp, Phường Thạnh Lộc, Quận 12, TP. Hồ Chí Minh', N'MOMO', NULL, N'Đơn hàng mẫu 43', N'Giao hàng tận nơi thành công', DATEADD(HOUR, -245, GETDATE()), NULL, DATEADD(HOUR, -247, GETDATE()), NULL),
(44, 8, 6, 50000.00, 30000.00, 20000.00, N'HOAN_THANH', N'Phạm Thị Dung', N'0944556677', N'92 Tô Ký, Phường Tân Chánh Hiệp, Quận 12, TP. Hồ Chí Minh', N'COD', N'WELCOME10', N'Đơn hàng mẫu 44', N'Giao hàng tận nơi thành công', DATEADD(HOUR, -210, GETDATE()), NULL, DATEADD(HOUR, -212, GETDATE()), NULL),
(45, 2, 6, 318000.00, 0.00, 318000.00, N'GIAO_THAT_BAI', N'Nguyễn Văn A', N'0987654321', N'15 Quốc Lộ 1A, Phường An Phú Đông, Quận 12, TP. Hồ Chí Minh', N'BANK_TRANSFER', NULL, N'Đơn hàng mẫu 45', NULL, NULL, N'Khách hàng bận không nghe máy', DATEADD(HOUR, -177, GETDATE()), NULL),
(46, 3, 4, 192000.00, 0.00, 192000.00, N'HOAN_THANH', N'Trần Thị B', N'0977888999', N'123 Lũy Bán Bích, Phường Hòa Thạnh, Quận Tân Phú, TP. Hồ Chí Minh', N'MOMO', NULL, N'Đơn hàng mẫu 46', N'Giao hàng tận nơi thành công', DATEADD(HOUR, -140, GETDATE()), NULL, DATEADD(HOUR, -142, GETDATE()), NULL),
(47, 7, 4, 172000.00, 0.00, 172000.00, N'HOAN_THANH', N'Lê Văn Cường', N'0933112233', N'45 Thoại Ngọc Hầu, Phường Phú Thạnh, Quận Tân Phú, TP. Hồ Chí Minh', N'COD', NULL, N'Đơn hàng mẫu 47', N'Giao hàng tận nơi thành công', DATEADD(HOUR, -105, GETDATE()), NULL, DATEADD(HOUR, -107, GETDATE()), NULL),
(48, 8, 4, 190000.00, 0.00, 190000.00, N'DA_XAC_NHAN', N'Phạm Thị Dung', N'0944556677', N'88 Tân Kỳ Tân Quý, Phường Tân Sơn Nhì, Quận Tân Phú, TP. Hồ Chí Minh', N'BANK_TRANSFER', NULL, N'Đơn hàng mẫu 48', NULL, NULL, NULL, DATEADD(HOUR, -72, GETDATE()), NULL),
(49, 2, 4, 108000.00, 0.00, 108000.00, N'DA_XAC_NHAN', N'Nguyễn Văn A', N'0987654321', N'15 Lê Trọng Tấn, Phường Tây Thạnh, Quận Tân Phú, TP. Hồ Chí Minh', N'MOMO', NULL, N'Đơn hàng mẫu 49', NULL, NULL, NULL, DATEADD(HOUR, -37, GETDATE()), NULL),
(50, 3, 4, 124000.00, 0.00, 124000.00, N'DA_XAC_NHAN', N'Trần Thị B', N'0977888999', N'210 Vườn Lài, Phường Phú Thọ Hòa, Quận Tân Phú, TP. Hồ Chí Minh', N'COD', NULL, N'Đơn hàng mẫu 50', NULL, NULL, NULL, DATEADD(HOUR, -2, GETDATE()), NULL);
SET IDENTITY_INSERT dbo.orders OFF;

SET IDENTITY_INSERT dbo.order_items ON;
INSERT INTO dbo.order_items
(id, order_id, product_id, quantity, price, product_name, product_image)
VALUES
(1, 1, 1, 2, 20000.00, N'Xà lách thủy canh sạch', N'/uploads/xa_lach_thuy_canh.jpg'),
(2, 1, 2, 2, 30000.00, N'Cà chua VietGAP 1kg', N'/uploads/ca_chua_dalat.jpg'),
(3, 1, 4, 2, 62000.00, N'Dầu ăn Simply Đậu Nành 1L', N'/uploads/dau_an_simply.jpg'),
(4, 2, 3, 1, 128000.00, N'Mì Hảo Hảo Tôm Chua Cay', N'/uploads/mi_hao_hao.jpg'),
(5, 2, 7, 1, 10000.00, N'Nước ngọt Coca Cola lon 320ml', N'/uploads/coca_cola.jpg'),
(6, 3, 5, 2, 36000.00, N'Sữa tươi Vinamilk ít đường 1L', N'/uploads/sua_vinamilk_1l.jpg'),
(7, 3, 6, 2, 8000.00, N'Sữa chua Vinamilk có đường hộp 100g', N'/uploads/sua_chua_vinamilk.jpg'),
(8, 4, 1, 1, 20000.00, N'Xà lách thủy canh sạch', N'/uploads/xa_lach_thuy_canh.jpg'),
(9, 4, 2, 1, 30000.00, N'Cà chua VietGAP 1kg', N'/uploads/ca_chua_dalat.jpg'),
(10, 5, 3, 2, 128000.00, N'Mì Hảo Hảo Tôm Chua Cay', N'/uploads/mi_hao_hao.jpg'),
(11, 5, 7, 2, 10000.00, N'Nước ngọt Coca Cola lon 320ml', N'/uploads/coca_cola.jpg'),
(12, 5, 8, 2, 21000.00, N'Bia Heineken lon 330ml', N'/uploads/bia_heineken.jpg'),
(13, 6, 9, 1, 160000.00, N'Dầu gội Clear Bạc Hà Mát Lạnh 630ml', N'/uploads/dau_goi_clear.webp'),
(14, 6, 10, 1, 32000.00, N'Nước lau sàn Sunlight Hoa Lilia 1kg', N'/uploads/nuoc_lau_san_sunlight.jpg'),
(15, 7, 1, 2, 20000.00, N'Xà lách thủy canh sạch', N'/uploads/xa_lach_thuy_canh.jpg'),
(16, 7, 2, 2, 30000.00, N'Cà chua VietGAP 1kg', N'/uploads/ca_chua_dalat.jpg'),
(17, 7, 5, 2, 36000.00, N'Sữa tươi Vinamilk ít đường 1L', N'/uploads/sua_vinamilk_1l.jpg'),
(18, 8, 4, 1, 62000.00, N'Dầu ăn Simply Đậu Nành 1L', N'/uploads/dau_an_simply.jpg'),
(19, 8, 3, 1, 128000.00, N'Mì Hảo Hảo Tôm Chua Cay', N'/uploads/mi_hao_hao.jpg'),
(20, 9, 5, 2, 36000.00, N'Sữa tươi Vinamilk ít đường 1L', N'/uploads/sua_vinamilk_1l.jpg'),
(21, 9, 6, 2, 8000.00, N'Sữa chua Vinamilk có đường hộp 100g', N'/uploads/sua_chua_vinamilk.jpg'),
(22, 9, 7, 2, 10000.00, N'Nước ngọt Coca Cola lon 320ml', N'/uploads/coca_cola.jpg'),
(23, 10, 2, 1, 30000.00, N'Cà chua VietGAP 1kg', N'/uploads/ca_chua_dalat.jpg'),
(24, 10, 4, 1, 62000.00, N'Dầu ăn Simply Đậu Nành 1L', N'/uploads/dau_an_simply.jpg'),
(25, 10, 10, 1, 32000.00, N'Nước lau sàn Sunlight Hoa Lilia 1kg', N'/uploads/nuoc_lau_san_sunlight.jpg'),
(26, 11, 1, 2, 20000.00, N'Xà lách thủy canh sạch', N'/uploads/xa_lach_thuy_canh.jpg'),
(27, 11, 2, 2, 30000.00, N'Cà chua VietGAP 1kg', N'/uploads/ca_chua_dalat.jpg'),
(28, 11, 4, 2, 62000.00, N'Dầu ăn Simply Đậu Nành 1L', N'/uploads/dau_an_simply.jpg'),
(29, 12, 3, 1, 128000.00, N'Mì Hảo Hảo Tôm Chua Cay', N'/uploads/mi_hao_hao.jpg'),
(30, 12, 7, 1, 10000.00, N'Nước ngọt Coca Cola lon 320ml', N'/uploads/coca_cola.jpg'),
(31, 13, 5, 2, 36000.00, N'Sữa tươi Vinamilk ít đường 1L', N'/uploads/sua_vinamilk_1l.jpg'),
(32, 13, 6, 2, 8000.00, N'Sữa chua Vinamilk có đường hộp 100g', N'/uploads/sua_chua_vinamilk.jpg'),
(33, 14, 1, 1, 20000.00, N'Xà lách thủy canh sạch', N'/uploads/xa_lach_thuy_canh.jpg'),
(34, 14, 2, 1, 30000.00, N'Cà chua VietGAP 1kg', N'/uploads/ca_chua_dalat.jpg'),
(35, 15, 3, 2, 128000.00, N'Mì Hảo Hảo Tôm Chua Cay', N'/uploads/mi_hao_hao.jpg'),
(36, 15, 7, 2, 10000.00, N'Nước ngọt Coca Cola lon 320ml', N'/uploads/coca_cola.jpg'),
(37, 15, 8, 2, 21000.00, N'Bia Heineken lon 330ml', N'/uploads/bia_heineken.jpg'),
(38, 16, 9, 1, 160000.00, N'Dầu gội Clear Bạc Hà Mát Lạnh 630ml', N'/uploads/dau_goi_clear.webp'),
(39, 16, 10, 1, 32000.00, N'Nước lau sàn Sunlight Hoa Lilia 1kg', N'/uploads/nuoc_lau_san_sunlight.jpg'),
(40, 17, 1, 2, 20000.00, N'Xà lách thủy canh sạch', N'/uploads/xa_lach_thuy_canh.jpg'),
(41, 17, 2, 2, 30000.00, N'Cà chua VietGAP 1kg', N'/uploads/ca_chua_dalat.jpg'),
(42, 17, 5, 2, 36000.00, N'Sữa tươi Vinamilk ít đường 1L', N'/uploads/sua_vinamilk_1l.jpg'),
(43, 18, 4, 1, 62000.00, N'Dầu ăn Simply Đậu Nành 1L', N'/uploads/dau_an_simply.jpg'),
(44, 18, 3, 1, 128000.00, N'Mì Hảo Hảo Tôm Chua Cay', N'/uploads/mi_hao_hao.jpg'),
(45, 19, 5, 2, 36000.00, N'Sữa tươi Vinamilk ít đường 1L', N'/uploads/sua_vinamilk_1l.jpg'),
(46, 19, 6, 2, 8000.00, N'Sữa chua Vinamilk có đường hộp 100g', N'/uploads/sua_chua_vinamilk.jpg'),
(47, 19, 7, 2, 10000.00, N'Nước ngọt Coca Cola lon 320ml', N'/uploads/coca_cola.jpg'),
(48, 20, 2, 1, 30000.00, N'Cà chua VietGAP 1kg', N'/uploads/ca_chua_dalat.jpg'),
(49, 20, 4, 1, 62000.00, N'Dầu ăn Simply Đậu Nành 1L', N'/uploads/dau_an_simply.jpg'),
(50, 20, 10, 1, 32000.00, N'Nước lau sàn Sunlight Hoa Lilia 1kg', N'/uploads/nuoc_lau_san_sunlight.jpg'),
(51, 21, 1, 2, 20000.00, N'Xà lách thủy canh sạch', N'/uploads/xa_lach_thuy_canh.jpg'),
(52, 21, 2, 2, 30000.00, N'Cà chua VietGAP 1kg', N'/uploads/ca_chua_dalat.jpg'),
(53, 21, 4, 2, 62000.00, N'Dầu ăn Simply Đậu Nành 1L', N'/uploads/dau_an_simply.jpg'),
(54, 22, 3, 1, 128000.00, N'Mì Hảo Hảo Tôm Chua Cay', N'/uploads/mi_hao_hao.jpg'),
(55, 22, 7, 1, 10000.00, N'Nước ngọt Coca Cola lon 320ml', N'/uploads/coca_cola.jpg'),
(56, 23, 5, 2, 36000.00, N'Sữa tươi Vinamilk ít đường 1L', N'/uploads/sua_vinamilk_1l.jpg'),
(57, 23, 6, 2, 8000.00, N'Sữa chua Vinamilk có đường hộp 100g', N'/uploads/sua_chua_vinamilk.jpg'),
(58, 24, 1, 1, 20000.00, N'Xà lách thủy canh sạch', N'/uploads/xa_lach_thuy_canh.jpg'),
(59, 24, 2, 1, 30000.00, N'Cà chua VietGAP 1kg', N'/uploads/ca_chua_dalat.jpg'),
(60, 25, 3, 2, 128000.00, N'Mì Hảo Hảo Tôm Chua Cay', N'/uploads/mi_hao_hao.jpg'),
(61, 25, 7, 2, 10000.00, N'Nước ngọt Coca Cola lon 320ml', N'/uploads/coca_cola.jpg'),
(62, 25, 8, 2, 21000.00, N'Bia Heineken lon 330ml', N'/uploads/bia_heineken.jpg'),
(63, 26, 9, 1, 160000.00, N'Dầu gội Clear Bạc Hà Mát Lạnh 630ml', N'/uploads/dau_goi_clear.webp'),
(64, 26, 10, 1, 32000.00, N'Nước lau sàn Sunlight Hoa Lilia 1kg', N'/uploads/nuoc_lau_san_sunlight.jpg'),
(65, 27, 1, 2, 20000.00, N'Xà lách thủy canh sạch', N'/uploads/xa_lach_thuy_canh.jpg'),
(66, 27, 2, 2, 30000.00, N'Cà chua VietGAP 1kg', N'/uploads/ca_chua_dalat.jpg'),
(67, 27, 5, 2, 36000.00, N'Sữa tươi Vinamilk ít đường 1L', N'/uploads/sua_vinamilk_1l.jpg'),
(68, 28, 4, 1, 62000.00, N'Dầu ăn Simply Đậu Nành 1L', N'/uploads/dau_an_simply.jpg'),
(69, 28, 3, 1, 128000.00, N'Mì Hảo Hảo Tôm Chua Cay', N'/uploads/mi_hao_hao.jpg'),
(70, 29, 5, 2, 36000.00, N'Sữa tươi Vinamilk ít đường 1L', N'/uploads/sua_vinamilk_1l.jpg'),
(71, 29, 6, 2, 8000.00, N'Sữa chua Vinamilk có đường hộp 100g', N'/uploads/sua_chua_vinamilk.jpg'),
(72, 29, 7, 2, 10000.00, N'Nước ngọt Coca Cola lon 320ml', N'/uploads/coca_cola.jpg'),
(73, 30, 2, 1, 30000.00, N'Cà chua VietGAP 1kg', N'/uploads/ca_chua_dalat.jpg'),
(74, 30, 4, 1, 62000.00, N'Dầu ăn Simply Đậu Nành 1L', N'/uploads/dau_an_simply.jpg'),
(75, 30, 10, 1, 32000.00, N'Nước lau sàn Sunlight Hoa Lilia 1kg', N'/uploads/nuoc_lau_san_sunlight.jpg'),
(76, 31, 1, 2, 20000.00, N'Xà lách thủy canh sạch', N'/uploads/xa_lach_thuy_canh.jpg'),
(77, 31, 2, 2, 30000.00, N'Cà chua VietGAP 1kg', N'/uploads/ca_chua_dalat.jpg'),
(78, 31, 4, 2, 62000.00, N'Dầu ăn Simply Đậu Nành 1L', N'/uploads/dau_an_simply.jpg'),
(79, 32, 3, 1, 128000.00, N'Mì Hảo Hảo Tôm Chua Cay', N'/uploads/mi_hao_hao.jpg'),
(80, 32, 7, 1, 10000.00, N'Nước ngọt Coca Cola lon 320ml', N'/uploads/coca_cola.jpg'),
(81, 33, 5, 2, 36000.00, N'Sữa tươi Vinamilk ít đường 1L', N'/uploads/sua_vinamilk_1l.jpg'),
(82, 33, 6, 2, 8000.00, N'Sữa chua Vinamilk có đường hộp 100g', N'/uploads/sua_chua_vinamilk.jpg'),
(83, 34, 1, 1, 20000.00, N'Xà lách thủy canh sạch', N'/uploads/xa_lach_thuy_canh.jpg'),
(84, 34, 2, 1, 30000.00, N'Cà chua VietGAP 1kg', N'/uploads/ca_chua_dalat.jpg'),
(85, 35, 3, 2, 128000.00, N'Mì Hảo Hảo Tôm Chua Cay', N'/uploads/mi_hao_hao.jpg'),
(86, 35, 7, 2, 10000.00, N'Nước ngọt Coca Cola lon 320ml', N'/uploads/coca_cola.jpg'),
(87, 35, 8, 2, 21000.00, N'Bia Heineken lon 330ml', N'/uploads/bia_heineken.jpg'),
(88, 36, 9, 1, 160000.00, N'Dầu gội Clear Bạc Hà Mát Lạnh 630ml', N'/uploads/dau_goi_clear.webp'),
(89, 36, 10, 1, 32000.00, N'Nước lau sàn Sunlight Hoa Lilia 1kg', N'/uploads/nuoc_lau_san_sunlight.jpg'),
(90, 37, 1, 2, 20000.00, N'Xà lách thủy canh sạch', N'/uploads/xa_lach_thuy_canh.jpg'),
(91, 37, 2, 2, 30000.00, N'Cà chua VietGAP 1kg', N'/uploads/ca_chua_dalat.jpg'),
(92, 37, 5, 2, 36000.00, N'Sữa tươi Vinamilk ít đường 1L', N'/uploads/sua_vinamilk_1l.jpg'),
(93, 38, 4, 1, 62000.00, N'Dầu ăn Simply Đậu Nành 1L', N'/uploads/dau_an_simply.jpg'),
(94, 38, 3, 1, 128000.00, N'Mì Hảo Hảo Tôm Chua Cay', N'/uploads/mi_hao_hao.jpg'),
(95, 39, 5, 2, 36000.00, N'Sữa tươi Vinamilk ít đường 1L', N'/uploads/sua_vinamilk_1l.jpg'),
(96, 39, 6, 2, 8000.00, N'Sữa chua Vinamilk có đường hộp 100g', N'/uploads/sua_chua_vinamilk.jpg'),
(97, 39, 7, 2, 10000.00, N'Nước ngọt Coca Cola lon 320ml', N'/uploads/coca_cola.jpg'),
(98, 40, 2, 1, 30000.00, N'Cà chua VietGAP 1kg', N'/uploads/ca_chua_dalat.jpg'),
(99, 40, 4, 1, 62000.00, N'Dầu ăn Simply Đậu Nành 1L', N'/uploads/dau_an_simply.jpg'),
(100, 40, 10, 1, 32000.00, N'Nước lau sàn Sunlight Hoa Lilia 1kg', N'/uploads/nuoc_lau_san_sunlight.jpg'),
(101, 41, 1, 2, 20000.00, N'Xà lách thủy canh sạch', N'/uploads/xa_lach_thuy_canh.jpg'),
(102, 41, 2, 2, 30000.00, N'Cà chua VietGAP 1kg', N'/uploads/ca_chua_dalat.jpg'),
(103, 41, 4, 2, 62000.00, N'Dầu ăn Simply Đậu Nành 1L', N'/uploads/dau_an_simply.jpg'),
(104, 42, 3, 1, 128000.00, N'Mì Hảo Hảo Tôm Chua Cay', N'/uploads/mi_hao_hao.jpg'),
(105, 42, 7, 1, 10000.00, N'Nước ngọt Coca Cola lon 320ml', N'/uploads/coca_cola.jpg'),
(106, 43, 5, 2, 36000.00, N'Sữa tươi Vinamilk ít đường 1L', N'/uploads/sua_vinamilk_1l.jpg'),
(107, 43, 6, 2, 8000.00, N'Sữa chua Vinamilk có đường hộp 100g', N'/uploads/sua_chua_vinamilk.jpg'),
(108, 44, 1, 1, 20000.00, N'Xà lách thủy canh sạch', N'/uploads/xa_lach_thuy_canh.jpg'),
(109, 44, 2, 1, 30000.00, N'Cà chua VietGAP 1kg', N'/uploads/ca_chua_dalat.jpg'),
(110, 45, 3, 2, 128000.00, N'Mì Hảo Hảo Tôm Chua Cay', N'/uploads/mi_hao_hao.jpg'),
(111, 45, 7, 2, 10000.00, N'Nước ngọt Coca Cola lon 320ml', N'/uploads/coca_cola.jpg'),
(112, 45, 8, 2, 21000.00, N'Bia Heineken lon 330ml', N'/uploads/bia_heineken.jpg'),
(113, 46, 9, 1, 160000.00, N'Dầu gội Clear Bạc Hà Mát Lạnh 630ml', N'/uploads/dau_goi_clear.webp'),
(114, 46, 10, 1, 32000.00, N'Nước lau sàn Sunlight Hoa Lilia 1kg', N'/uploads/nuoc_lau_san_sunlight.jpg'),
(115, 47, 1, 2, 20000.00, N'Xà lách thủy canh sạch', N'/uploads/xa_lach_thuy_canh.jpg'),
(116, 47, 2, 2, 30000.00, N'Cà chua VietGAP 1kg', N'/uploads/ca_chua_dalat.jpg'),
(117, 47, 5, 2, 36000.00, N'Sữa tươi Vinamilk ít đường 1L', N'/uploads/sua_vinamilk_1l.jpg'),
(118, 48, 4, 1, 62000.00, N'Dầu ăn Simply Đậu Nành 1L', N'/uploads/dau_an_simply.jpg'),
(119, 48, 3, 1, 128000.00, N'Mì Hảo Hảo Tôm Chua Cay', N'/uploads/mi_hao_hao.jpg'),
(120, 49, 5, 2, 36000.00, N'Sữa tươi Vinamilk ít đường 1L', N'/uploads/sua_vinamilk_1l.jpg'),
(121, 49, 6, 2, 8000.00, N'Sữa chua Vinamilk có đường hộp 100g', N'/uploads/sua_chua_vinamilk.jpg'),
(122, 49, 7, 2, 10000.00, N'Nước ngọt Coca Cola lon 320ml', N'/uploads/coca_cola.jpg'),
(123, 50, 2, 1, 30000.00, N'Cà chua VietGAP 1kg', N'/uploads/ca_chua_dalat.jpg'),
(124, 50, 4, 1, 62000.00, N'Dầu ăn Simply Đậu Nành 1L', N'/uploads/dau_an_simply.jpg'),
(125, 50, 10, 1, 32000.00, N'Nước lau sàn Sunlight Hoa Lilia 1kg', N'/uploads/nuoc_lau_san_sunlight.jpg');
SET IDENTITY_INSERT dbo.order_items OFF;

SET IDENTITY_INSERT dbo.payments ON;
INSERT INTO dbo.payments
(id, order_id, payment_method, payment_status, transaction_id, amount, paid_at)
VALUES
(1, 1, N'MOMO', N'COMPLETED', N'MOMO-0001', 224000.00, DATEADD(HOUR, -1717, GETDATE())),
(2, 2, N'COD', N'COMPLETED', N'COD-0002', 138000.00, DATEADD(HOUR, -1682, GETDATE())),
(3, 3, N'BANK_TRANSFER', N'COMPLETED', N'BANK-0003', 88000.00, DATEADD(HOUR, -1647, GETDATE())),
(4, 4, N'MOMO', N'COMPLETED', N'MOMO-0004', 20000.00, DATEADD(HOUR, -1612, GETDATE())),
(5, 5, N'COD', N'COMPLETED', N'COD-0005', 318000.00, DATEADD(HOUR, -1577, GETDATE())),
(6, 6, N'BANK_TRANSFER', N'COMPLETED', N'BANK-0006', 192000.00, DATEADD(HOUR, -1542, GETDATE())),
(7, 7, N'MOMO', N'COMPLETED', N'MOMO-0007', 172000.00, DATEADD(HOUR, -1507, GETDATE())),
(8, 8, N'COD', N'COMPLETED', N'COD-0008', 165000.00, DATEADD(HOUR, -1472, GETDATE())),
(9, 9, N'BANK_TRANSFER', N'COMPLETED', N'BANK-0009', 108000.00, DATEADD(HOUR, -1437, GETDATE())),
(10, 10, N'MOMO', N'PENDING', N'MOMO-0010', 124000.00, NULL),
(11, 11, N'COD', N'COMPLETED', N'COD-0011', 224000.00, DATEADD(HOUR, -1367, GETDATE())),
(12, 12, N'BANK_TRANSFER', N'COMPLETED', N'BANK-0012', 108000.00, DATEADD(HOUR, -1332, GETDATE())),
(13, 13, N'MOMO', N'COMPLETED', N'MOMO-0013', 88000.00, DATEADD(HOUR, -1297, GETDATE())),
(14, 14, N'COD', N'COMPLETED', N'COD-0014', 50000.00, DATEADD(HOUR, -1262, GETDATE())),
(15, 15, N'BANK_TRANSFER', N'FAILED', N'BANK-0015', 318000.00, NULL),
(16, 16, N'MOMO', N'COMPLETED', N'MOMO-0016', 167000.00, DATEADD(HOUR, -1192, GETDATE())),
(17, 17, N'COD', N'COMPLETED', N'COD-0017', 172000.00, DATEADD(HOUR, -1157, GETDATE())),
(18, 18, N'BANK_TRANSFER', N'COMPLETED', N'BANK-0018', 190000.00, DATEADD(HOUR, -1122, GETDATE())),
(19, 19, N'MOMO', N'COMPLETED', N'MOMO-0019', 108000.00, DATEADD(HOUR, -1087, GETDATE())),
(20, 20, N'COD', N'COMPLETED', N'COD-0020', 94000.00, DATEADD(HOUR, -1052, GETDATE())),
(21, 21, N'BANK_TRANSFER', N'COMPLETED', N'BANK-0021', 224000.00, DATEADD(HOUR, -1017, GETDATE())),
(22, 22, N'MOMO', N'COMPLETED', N'MOMO-0022', 138000.00, DATEADD(HOUR, -982, GETDATE())),
(23, 23, N'COD', N'COMPLETED', N'COD-0023', 88000.00, DATEADD(HOUR, -947, GETDATE())),
(24, 24, N'BANK_TRANSFER', N'COMPLETED', N'BANK-0024', 25000.00, DATEADD(HOUR, -912, GETDATE())),
(25, 25, N'MOMO', N'COMPLETED', N'MOMO-0025', 318000.00, DATEADD(HOUR, -877, GETDATE())),
(26, 26, N'COD', N'COMPLETED', N'COD-0026', 192000.00, DATEADD(HOUR, -842, GETDATE())),
(27, 27, N'BANK_TRANSFER', N'COMPLETED', N'BANK-0027', 172000.00, DATEADD(HOUR, -807, GETDATE())),
(28, 28, N'MOMO', N'COMPLETED', N'MOMO-0028', 160000.00, DATEADD(HOUR, -772, GETDATE())),
(29, 29, N'COD', N'COMPLETED', N'COD-0029', 108000.00, DATEADD(HOUR, -737, GETDATE())),
(30, 30, N'BANK_TRANSFER', N'COMPLETED', N'BANK-0030', 124000.00, DATEADD(HOUR, -702, GETDATE())),
(31, 31, N'MOMO', N'COMPLETED', N'MOMO-0031', 224000.00, DATEADD(HOUR, -667, GETDATE())),
(32, 32, N'COD', N'COMPLETED', N'COD-0032', 113000.00, DATEADD(HOUR, -632, GETDATE())),
(33, 33, N'BANK_TRANSFER', N'COMPLETED', N'BANK-0033', 88000.00, DATEADD(HOUR, -597, GETDATE())),
(34, 34, N'MOMO', N'COMPLETED', N'MOMO-0034', 50000.00, DATEADD(HOUR, -562, GETDATE())),
(35, 35, N'COD', N'FAILED', N'COD-0035', 318000.00, NULL),
(36, 36, N'BANK_TRANSFER', N'COMPLETED', N'BANK-0036', 162000.00, DATEADD(HOUR, -492, GETDATE())),
(37, 37, N'MOMO', N'COMPLETED', N'MOMO-0037', 172000.00, DATEADD(HOUR, -457, GETDATE())),
(38, 38, N'COD', N'COMPLETED', N'COD-0038', 190000.00, DATEADD(HOUR, -422, GETDATE())),
(39, 39, N'BANK_TRANSFER', N'COMPLETED', N'BANK-0039', 108000.00, DATEADD(HOUR, -387, GETDATE())),
(40, 40, N'MOMO', N'COMPLETED', N'MOMO-0040', 99000.00, DATEADD(HOUR, -352, GETDATE())),
(41, 41, N'COD', N'COMPLETED', N'COD-0041', 224000.00, DATEADD(HOUR, -317, GETDATE())),
(42, 42, N'BANK_TRANSFER', N'COMPLETED', N'BANK-0042', 138000.00, DATEADD(HOUR, -282, GETDATE())),
(43, 43, N'MOMO', N'COMPLETED', N'MOMO-0043', 88000.00, DATEADD(HOUR, -247, GETDATE())),
(44, 44, N'COD', N'COMPLETED', N'COD-0044', 20000.00, DATEADD(HOUR, -212, GETDATE())),
(45, 45, N'BANK_TRANSFER', N'PENDING', N'BANK-0045', 318000.00, NULL),
(46, 46, N'MOMO', N'COMPLETED', N'MOMO-0046', 192000.00, DATEADD(HOUR, -142, GETDATE())),
(47, 47, N'COD', N'COMPLETED', N'COD-0047', 172000.00, DATEADD(HOUR, -107, GETDATE())),
(48, 48, N'BANK_TRANSFER', N'COMPLETED', N'BANK-0048', 190000.00, DATEADD(HOUR, -72, GETDATE())),
(49, 49, N'MOMO', N'COMPLETED', N'MOMO-0049', 108000.00, DATEADD(HOUR, -37, GETDATE())),
(50, 50, N'COD', N'PENDING', N'COD-0050', 124000.00, NULL);
SET IDENTITY_INSERT dbo.payments OFF;

-- Payment Method Configs Seed
SET IDENTITY_INSERT dbo.payment_method_configs ON;
INSERT INTO dbo.payment_method_configs
(id, method_key, name, description, is_enabled, account_number, account_holder, bank_name, qr_code_url, transfer_syntax, created_at)
VALUES
(1, N'COD',           N'Thanh toán khi nhận hàng (COD)', N'Thanh toán bằng tiền mặt trực tiếp cho Shipper khi nhận hàng tận nơi.', 1, NULL, NULL, NULL, NULL, NULL, GETDATE()),
(2, N'BANK_TRANSFER', N'Chuyển khoản VietQR',           N'Quét mã VietQR chuyển khoản nhanh 24/7 qua ứng dụng ngân hàng bất kỳ.',  1, N'0336174371', N'NGUYEN TIEN TAI', N'MBBank (Ngân hàng Quân Đội)', N'https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=2|99|0336174371|NGUYEN%20TIEN%20TAI||0|0|0|MINIMART', N'MINIMART {orderId} {phone}', GETDATE()),
(3, N'MOMO',          N'Ví điện tử MoMo',               N'Quét mã QR qua ứng dụng Ví MoMo tiện lợi và nhanh chóng.',               1, N'0336174371', N'NGUYEN TIEN TAI', N'Ví điện tử MoMo',             N'https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=2|99|0336174371|NGUYEN%20TIEN%20TAI||0|0|0|MINIMART', N'MINIMART {orderId} {phone}', GETDATE());
SET IDENTITY_INSERT dbo.payment_method_configs OFF;

-- Coupon used by demo order 4 (insert it after coupon table exists)
UPDATE dbo.coupons
SET used_count = used_count + 1
WHERE code = N'WELCOME10';

-- =============================================================
-- 26. RESET IDENTITY SEEDS
-- Keeps future JPA IDENTITY inserts above the seed IDs.
-- =============================================================
DBCC CHECKIDENT ('dbo.roles', RESEED);
DBCC CHECKIDENT ('dbo.users', RESEED);
DBCC CHECKIDENT ('dbo.addresses', RESEED);
DBCC CHECKIDENT ('dbo.categories', RESEED);
DBCC CHECKIDENT ('dbo.brands', RESEED);
DBCC CHECKIDENT ('dbo.suppliers', RESEED);
DBCC CHECKIDENT ('dbo.products', RESEED);
DBCC CHECKIDENT ('dbo.product_images', RESEED);
DBCC CHECKIDENT ('dbo.inventory', RESEED);
DBCC CHECKIDENT ('dbo.goods_receipt', RESEED);
DBCC CHECKIDENT ('dbo.goods_receipt_items', RESEED);
DBCC CHECKIDENT ('dbo.product_batches', RESEED);
DBCC CHECKIDENT ('dbo.inventory_ledger', RESEED);
DBCC CHECKIDENT ('dbo.cart', RESEED);
DBCC CHECKIDENT ('dbo.cart_items', RESEED);
DBCC CHECKIDENT ('dbo.orders', RESEED);
DBCC CHECKIDENT ('dbo.order_items', RESEED);
DBCC CHECKIDENT ('dbo.payments', RESEED);
DBCC CHECKIDENT ('dbo.payment_method_configs', RESEED);
DBCC CHECKIDENT ('dbo.reviews', RESEED);
DBCC CHECKIDENT ('dbo.coupons', RESEED);
DBCC CHECKIDENT ('dbo.customer_messages', RESEED);
DBCC CHECKIDENT ('dbo.order_jobs', RESEED);
DBCC CHECKIDENT ('dbo.outbox_events', RESEED);
GO

-- =============================================================
-- 27. VALIDATION
-- =============================================================
PRINT N'=============================================';
PRINT N'✅ Database [valkey] created successfully';
PRINT N'✅ Schema aligned with Valkey queue & JPA entities';
PRINT N'✅ FK delete actions chosen to avoid SQL Server cascade-path conflicts';
PRINT N'=============================================';

SELECT N'roles' AS table_name, COUNT(*) AS row_count FROM dbo.roles
UNION ALL SELECT N'users', COUNT(*) FROM dbo.users
UNION ALL SELECT N'addresses', COUNT(*) FROM dbo.addresses
UNION ALL SELECT N'categories', COUNT(*) FROM dbo.categories
UNION ALL SELECT N'brands', COUNT(*) FROM dbo.brands
UNION ALL SELECT N'suppliers', COUNT(*) FROM dbo.suppliers
UNION ALL SELECT N'products', COUNT(*) FROM dbo.products
UNION ALL SELECT N'product_images', COUNT(*) FROM dbo.product_images
UNION ALL SELECT N'inventory', COUNT(*) FROM dbo.inventory
UNION ALL SELECT N'goods_receipt', COUNT(*) FROM dbo.goods_receipt
UNION ALL SELECT N'goods_receipt_items', COUNT(*) FROM dbo.goods_receipt_items
UNION ALL SELECT N'product_batches', COUNT(*) FROM dbo.product_batches
UNION ALL SELECT N'inventory_ledger', COUNT(*) FROM dbo.inventory_ledger
UNION ALL SELECT N'cart', COUNT(*) FROM dbo.cart
UNION ALL SELECT N'cart_items', COUNT(*) FROM dbo.cart_items
UNION ALL SELECT N'orders', COUNT(*) FROM dbo.orders
UNION ALL SELECT N'order_items', COUNT(*) FROM dbo.order_items
UNION ALL SELECT N'payments', COUNT(*) FROM dbo.payments
UNION ALL SELECT N'payment_method_configs', COUNT(*) FROM dbo.payment_method_configs
UNION ALL SELECT N'reviews', COUNT(*) FROM dbo.reviews
UNION ALL SELECT N'coupons', COUNT(*) FROM dbo.coupons
UNION ALL SELECT N'customer_messages', COUNT(*) FROM dbo.customer_messages
UNION ALL SELECT N'order_jobs', COUNT(*) FROM dbo.order_jobs
UNION ALL SELECT N'outbox_events', COUNT(*) FROM dbo.outbox_events;

SELECT p.id, p.name, c.name AS category_name, b.name AS brand_name, i.current_stock
FROM dbo.products p
LEFT JOIN dbo.categories c ON c.id = p.category_id
LEFT JOIN dbo.brands b ON b.id = p.brand_id
LEFT JOIN dbo.inventory i ON i.product_id = p.id
ORDER BY p.id;

SELECT o.id, o.total_amount, o.discount_amount, o.final_amount,
       SUM(oi.quantity * oi.price) AS item_total
FROM dbo.orders o
LEFT JOIN dbo.order_items oi ON oi.order_id = o.id
GROUP BY o.id, o.total_amount, o.discount_amount, o.final_amount
ORDER BY o.id;
GO
