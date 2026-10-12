package com.groceryshop.config;

import com.groceryshop.entity.*;
import com.groceryshop.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final CouponRepository couponRepository;
    private final ProductBatchRepository productBatchRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentMethodConfigRepository paymentConfigRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(
            RoleRepository roleRepository,
            UserRepository userRepository,
            CategoryRepository categoryRepository,
            BrandRepository brandRepository,
            ProductRepository productRepository,
            InventoryRepository inventoryRepository,
            CouponRepository couponRepository,
            ProductBatchRepository productBatchRepository,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            PaymentRepository paymentRepository,
            PaymentMethodConfigRepository paymentConfigRepository,
            PasswordEncoder passwordEncoder) {

        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.brandRepository = brandRepository;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.couponRepository = couponRepository;
        this.productBatchRepository = productBatchRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.paymentRepository = paymentRepository;
        this.paymentConfigRepository = paymentConfigRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        System.out.println("==========================================");
        System.out.println("   BAT DAU KHOI TAO DU LIEU SUPERMARKET");
        System.out.println("==========================================");

        initializeRoles();
        initializeUsers();
        initializeCategories();
        initializeBrands();
        initializeProductsAndBatches();
        initializeCoupons();
        initializePaymentConfigs();
        initializeSampleOrders();
        seedFiftyHistoricalOrders();
        normalizeAllExistingOrdersAndAddresses();

        System.out.println("==========================================");
        System.out.println("   KHOI TAO DU LIEU HOAN TAT 100%");
        System.out.println("==========================================");
    }

    // =========================================================
    // 1. ROLES
    // =========================================================
    private void initializeRoles() {
        createRoleIfNotExist("ROLE_ADMIN");
        createRoleIfNotExist("ROLE_USER");
        createRoleIfNotExist("ROLE_SHIPPER");
        System.out.println("[ROLE] Da khoi tao ROLE_ADMIN, ROLE_USER, ROLE_SHIPPER.");
    }

    private void createRoleIfNotExist(String roleName) {
        if (roleRepository.findByName(roleName).isEmpty()) {
            Role role = new Role();
            role.setName(roleName);
            roleRepository.save(role);
        }
    }

    // =========================================================
    // 2. USERS
    // =========================================================
    private void initializeUsers() {
        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseThrow(() -> new IllegalStateException("Khong tim thay ROLE_ADMIN"));
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new IllegalStateException("Khong tim thay ROLE_USER"));
        Role shipperRole = roleRepository.findByName("ROLE_SHIPPER")
                .orElseThrow(() -> new IllegalStateException("Khong tim thay ROLE_SHIPPER"));

        // 1. ADMIN
        createUserIfNotExist("admin", "admin@supermarket.com", "pass1234", "Quản Trị Viên", "0912345678", adminRole, "DIAMOND", 9999);

        // 2. SHIPPERS (Phân công 3 quận trọng điểm TP.HCM)
        createUserIfNotExist("shipper1", "shipper1@supermarket.com", "pass1234", "Nguyễn Văn Giao (Quận Tân Phú)", "0901234567", shipperRole, "BRONZE", 0);
        createUserIfNotExist("shipper2", "shipper2@supermarket.com", "pass1234", "Trần Văn Tốc (Quận Tân Bình)", "0907654321", shipperRole, "BRONZE", 0);
        createUserIfNotExist("shipper3", "shipper3@supermarket.com", "pass1234", "Lê Hoàng Vũ (Quận 12)", "0908889999", shipperRole, "BRONZE", 0);

        // 3. USERS / CUSTOMERS (Đủ 4 hạng VIP để test)
        createUserIfNotExist("user1", "user1@gmail.com", "pass1234", "Nguyễn Văn An", "0987654321", userRole, "DIAMOND", 1250);
        createUserIfNotExist("user2", "user2@gmail.com", "pass1234", "Trần Thị Bình", "0977888999", userRole, "GOLD", 620);
        createUserIfNotExist("user3", "user3@gmail.com", "pass1234", "Lê Hoàng Cường", "0933112233", userRole, "SILVER", 210);
        createUserIfNotExist("user4", "user4@gmail.com", "pass1234", "Phạm Thu Dung", "0911556677", userRole, "BRONZE", 45);

        System.out.println("[USER] Da tao admin, shipper1-3 (Tan Phu, Tan Binh, Q12), user1-4 kem hang VIP.");
    }

    private User createUserIfNotExist(String username, String email, String rawPass, String fullName, String phone, Role role, String tier, int points) {
        return userRepository.findByUsername(username).map(existing -> {
            if (fullName != null) existing.setFullName(fullName);
            if (phone != null) existing.setPhone(phone);
            if (tier != null) existing.setMembershipTier(tier);
            existing.setLoyaltyPoints(points);
            return userRepository.save(existing);
        }).orElseGet(() -> {
            User user = new User();
            user.setUsername(username);
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode(rawPass));
            user.setFullName(fullName);
            user.setPhone(phone);
            user.setRole(role);
            user.setMembershipTier(tier != null ? tier : "BRONZE");
            user.setLoyaltyPoints(points);
            user.setIsActive(true);
            return userRepository.save(user);
        });
    }

    // =========================================================
    // 3. CATEGORIES
    // =========================================================
    private void initializeCategories() {
        createCategoryIfNotExist("Rau Củ Quả", "rau-cu-qua", "Rau xanh, củ quả tươi sạch từ nông trại đạt chuẩn VietGAP", "/uploads/cat_rau_cu_qua.jpg");
        createCategoryIfNotExist("Thực Phẩm Tươi Sống", "thuc-pham-tuoi-song", "Thịt, cá, thủy hải sản tươi sống bảo quản lạnh tiêu chuẩn", "/uploads/cat_thuc_pham_tuoi_song.jpg");
        createCategoryIfNotExist("Sữa & Chế Phẩm Sữa", "sua-che-pham-sua", "Sữa tươi, sữa chua, phô mai bổ sung dinh dưỡng cho gia đình", "/uploads/cat_sua_che_pham_sua.jpg");
        createCategoryIfNotExist("Mì & Thực Phẩm Khô", "mi-thuc-pham-kho", "Mì ăn liền, bún, miến, đồ hộp ăn liền tiện lợi", "/uploads/cat_mi_thuc_pham_kho.jpg");
        createCategoryIfNotExist("Đồ Uống & Giải Khát", "do-uong-nuoc-giai-khat", "Nước ngọt, nước suối khoáng, nước ép hoa quả mát lạnh", "/uploads/cat_do_uong_giai_khat.jpg");
        createCategoryIfNotExist("Bánh Kẹo & Ăn Vặt", "banh-keo-do-an-vat", "Bánh quy, snack giòn rụm, kẹo ngọt thơm ngon", "/uploads/cat_banh_keo_an_vat.jpg");
        createCategoryIfNotExist("Gia Vị & Dầu Ăn", "gia-vi-dau-an", "Nước mắm, dầu ăn, hạt nêm, tiêu ớt nêm nếm món ngon", "/uploads/cat_gia_vi_dau_an.jpg");
        createCategoryIfNotExist("Hóa Phẩm & Gia Dụng", "hoa-pham-nha-cua", "Nước giặt, nước rửa chén, xà phòng chăm sóc gia đình", "/uploads/cat_hoa_pham_gia_dung.jpg");
    }

    private Category createCategoryIfNotExist(String name, String slug, String description, String image) {
        return categoryRepository.findByName(name).map(c -> {
            c.setImage(image);
            if (c.getDescription() == null) c.setDescription(description);
            return categoryRepository.save(c);
        }).orElseGet(() -> {
            Category c = new Category();
            c.setName(name);
            c.setDescription(description);
            c.setImage(image);
            return categoryRepository.save(c);
        });
    }

    // =========================================================
    // 4. BRANDS
    // =========================================================
    private void initializeBrands() {
        createBrandIfNotExist("Nông Trại VietGAP", "nong-trai-vietgap", "Sản phẩm nông nghiệp an toàn VietGAP");
        createBrandIfNotExist("Vinamilk", "vinamilk", "Thương hiệu sữa quốc gia số 1 Việt Nam");
        createBrandIfNotExist("TH True Milk", "th-true-milk", "Sữa tươi sạch nguyên chất từ trang trại TH");
        createBrandIfNotExist("Acecook", "acecook", "Biểu tượng mì ăn liền chất lượng Nhật Bản");
        createBrandIfNotExist("Masan", "masan", "Chinsu, Omachi, Nam Ngư - gia vị đậm đà bữa cơm");
        createBrandIfNotExist("Unilever", "unilever", "Tập đoàn tiêu dùng Omo, Sunlight, Knorr");
        createBrandIfNotExist("Coca-Cola", "coca-cola", "Thương hiệu nước giải khát hàng đầu thế giới");
        createBrandIfNotExist("Orion", "orion", "Bánh Chocopie, Custas trứ danh Hàn Quốc");
    }

    private Brand createBrandIfNotExist(String name, String slug, String description) {
        return brandRepository.findByName(name).orElseGet(() -> {
            Brand b = new Brand();
            b.setName(name);
            b.setDescription(description);
            return brandRepository.save(b);
        });
    }

    // =========================================================
    // 5. PRODUCTS & BATCHES (Cập nhật đè giá thị trường & ảnh local chuẩn 100%)
    // =========================================================
    private void initializeProductsAndBatches() {
        Category rauCu = categoryRepository.findByName("Rau Củ Quả").orElse(null);
        Category tuoiSong = categoryRepository.findByName("Thực Phẩm Tươi Sống").orElse(null);
        Category sua = categoryRepository.findByName("Sữa & Chế Phẩm Sữa").orElse(null);
        Category miKho = categoryRepository.findByName("Mì & Thực Phẩm Khô").orElse(null);
        Category doUong = categoryRepository.findByName("Đồ Uống & Giải Khát").orElse(null);
        Category banhKeo = categoryRepository.findByName("Bánh Kẹo & Ăn Vặt").orElse(null);
        Category giaVi = categoryRepository.findByName("Gia Vị & Dầu Ăn").orElse(null);
        Category hoaPham = categoryRepository.findByName("Hóa Phẩm & Gia Dụng").orElse(null);

        Brand vietgap = brandRepository.findByName("Nông Trại VietGAP").orElse(null);
        Brand vinamilk = brandRepository.findByName("Vinamilk").orElse(null);
        Brand thMilk = brandRepository.findByName("TH True Milk").orElse(null);
        Brand acecook = brandRepository.findByName("Acecook").orElse(null);
        Brand masan = brandRepository.findByName("Masan").orElse(null);
        Brand unilever = brandRepository.findByName("Unilever").orElse(null);
        Brand cocacola = brandRepository.findByName("Coca-Cola").orElse(null);
        Brand orion = brandRepository.findByName("Orion").orElse(null);

        // 1-4: Rau củ quả
        setupProductWithBatch("Xà lách thủy canh sạch 500g", "Xà lách tươi ngon được trồng theo phương pháp thủy canh VietGAP, an toàn tuyệt đối.", 25000, 20000.0, rauCu, vietgap, "/uploads/xa_lach_thuy_canh.jpg", "SKU-RCQ-001", "8935001001", "Túi 500g", 30, "Kệ A1 - Rau củ", 12);
        setupProductWithBatch("Cà chua VietGAP 1kg", "Cà chua đỏ mọng chín tự nhiên, giàu vitamin C và chất chống oxy hóa.", 32000, 26000.0, rauCu, vietgap, "/uploads/ca_chua_dalat.jpg", "SKU-RCQ-002", "8935001002", "Túi 1kg", 35, "Kệ A2 - Rau củ", 15);
        setupProductWithBatch("Dưa leo giống Nhật 1kg", "Dưa leo giòn ngọt, ruột đặc, vỏ mỏng thích hợp ăn sống và làm salad.", 28000, null, rauCu, vietgap, "/uploads/dua_chuot_nhat.jpg", "SKU-RCQ-003", "8935001003", "Túi 1kg", 25, "Kệ A3 - Rau củ", 10);
        setupProductWithBatch("Khoai tây Đà Lạt túi 1kg", "Khoai tây ruột vàng bở thơm bùi, nguồn gốc nông trại Đà Lạt không mọc mầm.", 38000, 32000.0, rauCu, vietgap, "/uploads/khoai_tay_dalat.jpg", "SKU-RCQ-004", "8935001004", "Túi 1kg", 40, "Kệ A4 - Củ quả", 45);
        setupProductWithBatch("Táo Envy New Zealand nhập khẩu 1kg", "Táo Envy giòn ngọt đậm đà, hương thơm dịu nhẹ chứa nhiều vitamin và khoáng chất.", 115000, 99000.0, rauCu, vietgap, "/uploads/tao_envy.jpg", "SKU-RCQ-005", "8935001005", "Túi 1kg", 25, "Kệ A5 - Trái cây", 20);
        setupProductWithBatch("Chuối Nam Mỹ già hương tươi 1kg", "Chuối già Nam Mỹ vỏ vàng óng, thịt dẻo ngọt giàu kali bổ dưỡng cho sức khỏe.", 32000, null, rauCu, vietgap, "/uploads/chuoi_nam_my.jpg", "SKU-RCQ-006", "8935001006", "Nải 1kg", 30, "Kệ A5 - Trái cây", 8);

        // 5-8: Thực phẩm tươi sống
        setupProductWithBatch("Thịt ba chỉ heo sạch 500g", "Thịt ba chỉ heo tươi sạch chuẩn VietGAP, tỷ lệ nạc mỡ cân đối.", 75000, 68000.0, tuoiSong, vietgap, "/uploads/thit_ba_chi.jpg", "SKU-TTS-001", "8935002001", "Khay 500g", 25, "Tủ mát 1 - Thịt heo", 5);
        setupProductWithBatch("Ức gà phi lê tươi 500g", "Ức gà bỏ da ít béo, dồi dào protein cho người tập gym và ăn kiêng.", 45000, 39000.0, tuoiSong, vietgap, "/uploads/uc_ga_phile.jpg", "SKU-TTS-002", "8935002002", "Khay 500g", 22, "Tủ mát 2 - Thịt gà", 7);
        setupProductWithBatch("Cá hồi phi lê tươi Nauy 300g", "Phi lê cá hồi nhập khẩu Nauy cắt miếng, giàu Omega-3 và DHA tự nhiên.", 160000, 145000.0, tuoiSong, vietgap, "/uploads/ca_hoi_phile.jpg", "SKU-TTS-003", "8935002003", "Khay 300g", 15, "Tủ đông 1 - Hải sản", 18);
        setupProductWithBatch("Trứng gà tươi Ba Huân hộp 10 quả", "Trứng gà tươi sạch tiệt trùng vỏ, lòng đỏ sậm giàu dinh dưỡng.", 36000, null, tuoiSong, vietgap, "/uploads/trung_ga_ba_huan.jpg", "SKU-TTS-004", "8935002004", "Hộp 10 quả", 50, "Kệ A5 - Trứng", 25);

        // 9-12: Sữa & Chế phẩm sữa
        setupProductWithBatch("Sữa tươi Vinamilk ít đường 1L", "Sữa tươi tiệt trùng 100% sữa bò tươi nguyên chất, bổ sung Vitamin A, D3.", 36000, 34000.0, sua, vinamilk, "/uploads/sua_vinamilk_1l.jpg", "SKU-SUA-001", "8935003001", "Hộp 1L", 45, "Tủ mát 3 - Sữa tươi", 90);
        setupProductWithBatch("Sữa chua Vinamilk có đường 100g", "Sữa chua lên men tự nhiên bổ sung men vi sinh hỗ trợ hệ tiêu hóa khỏe mạnh.", 7500, null, sua, vinamilk, "/uploads/sua_chua_vinamilk.jpg", "SKU-SUA-002", "8935003002", "Hộp 100g", 80, "Tủ mát 3 - Sữa chua", 14);
        setupProductWithBatch("Sữa tươi TH True Milk tiệt trùng 1L", "Sữa tươi sạch nguyên chất hoàn toàn từ đồng cỏ TH, vị thơm ngậy đặc trưng.", 39000, 36000.0, sua, thMilk, "/uploads/sua_th_truemilk_1l.jpg", "SKU-SUA-003", "8935003003", "Hộp 1L", 35, "Tủ mát 4 - Sữa TH", 85);
        setupProductWithBatch("Bơ lát Anchor tự nhiên 200g", "Bơ động vật nguyên chất từ New Zealand, béo ngậy thơm lừng cho các món nướng.", 75000, 68000.0, sua, vinamilk, "/uploads/bo_lat_anchor.jpg", "SKU-SUA-004", "8935003004", "Gói 200g", 25, "Tủ mát 4 - Phô mai", 60);

        // 13-16: Mì & Thực phẩm khô
        setupProductWithBatch("Mì Hảo Hảo Tôm Chua Cay gói 75g", "Huyền thoại mì ăn liền Việt Nam hương vị tôm chua cay đậm đà khó cưỡng.", 4800, 4500.0, miKho, acecook, "/uploads/mi_hao_hao.jpg", "SKU-KHO-001", "8935004001", "Gói 75g", 150, "Kệ B1 - Mì ăn liền", 180);
        setupProductWithBatch("Mì Omachi Sườn Hầm Ngũ Quả gói 80g", "Mì khoai tây Omachi không lo bị nóng, nước sốt sườn hầm ngũ quả đậm đà.", 8500, 7800.0, miKho, masan, "/uploads/mi_omachi.jpg", "SKU-KHO-002", "8935004002", "Gói 80g", 120, "Kệ B1 - Mì ăn liền", 200);
        setupProductWithBatch("Gạo ST25 Thơm Thượng Hạng túi 5kg", "Gạo ngon nhất thế giới hạt thon dài trắng đều, cơm dẻo mềm thơm mùi lá dứa.", 195000, 185000.0, miKho, vietgap, "/uploads/gao_st25.jpg", "SKU-KHO-003", "8935004003", "Túi 5kg", 40, "Kệ B2 - Gạo nông sản", 180);
        setupProductWithBatch("Xúc xích tiệt trùng Vissan gói 5 cây", "Xúc xích heo tiệt trùng dinh dưỡng thơm ngon tiện lợi cho bữa xế nhanh gọn.", 22000, 19000.0, miKho, masan, "/uploads/xuc_xich_vissan.jpg", "SKU-KHO-004", "8935004004", "Gói 5 cây", 60, "Kệ B2 - Đồ hộp", 45);

        // 17-20: Đồ uống
        setupProductWithBatch("Nước ngọt Coca-Cola lon 320ml", "Nước ngọt có ga Coca-Cola sảng khoái đánh tan cơn khát tức thì.", 10000, 9500.0, doUong, cocacola, "/uploads/coca_cola.jpg", "SKU-DU-001", "8935005001", "Lon 320ml", 120, "Kệ C1 - Nước ngọt", 240);
        setupProductWithBatch("Nước khoáng LaVie chai 500ml", "Nước khoáng thiên nhiên LaVie thanh khiết mát lành bổ sung khoáng chất mỗi ngày.", 6000, null, doUong, cocacola, "/uploads/nuoc_khoang_lavie.jpg", "SKU-DU-002", "8935005002", "Chai 500ml", 90, "Kệ C2 - Nước khoáng", 300);
        setupProductWithBatch("Trà Ô Long Tea+ Plus chai 455ml", "Trà Ô Long đậm vị chứa chất OTPP tự nhiên giúp hạn chế hấp thu chất béo.", 11000, 10000.0, doUong, masan, "/uploads/tra_olong.jpg", "SKU-DU-003", "8935005003", "Chai 455ml", 80, "Kệ C2 - Trà giải khát", 180);
        setupProductWithBatch("Nước tăng lực Red Bull lon 250ml", "Red Bull nắp vàng bừng tỉnh tỉnh táo sảng khoái cho ngày dài năng động.", 14000, null, doUong, masan, "/uploads/red_bull.jpg", "SKU-DU-004", "8935005004", "Lon 250ml", 75, "Kệ C1 - Nước ngọt", 240);

        // 21-24: Bánh kẹo & Đồ ăn vặt
        setupProductWithBatch("Bánh Chocopie Orion hộp 12 cái 396g", "Bánh xốp phủ socola mềm dẻo với lớp kem marshmallow béo ngậy ngọt ngào.", 55000, 49000.0, banhKeo, orion, "/uploads/banh_chocopie.jpg", "SKU-BK-001", "8935006001", "Hộp 12 cái", 40, "Kệ D1 - Bánh kẹo", 180);
        setupProductWithBatch("Bánh Custas Orion nhân kem trứng hộp 6 cái", "Bánh bông lan mềm mại nhân kem trứng ngọt ngào phù hợp bữa sáng dinh dưỡng.", 30000, 26000.0, banhKeo, orion, "/uploads/banh_custas.jpg", "SKU-BK-002", "8935006002", "Hộp 6 cái", 45, "Kệ D1 - Bánh kẹo", 30);
        setupProductWithBatch("Snack khoai tây Lay's vị tự nhiên gói 56g", "Khoai tây lát mỏng chiên giòn rụm từ củ khoai tươi chọn lọc hảo hạng.", 13000, null, banhKeo, orion, "/uploads/snack_lays.jpg", "SKU-BK-003", "8935006003", "Gói 56g", 85, "Kệ D2 - Snack", 90);
        setupProductWithBatch("Kẹo dẻo Chupa Chups Cầu Vồng gói 55g", "Kẹo dẻo hương trái cây chua ngọt dẻo dai với dải cầu vồng ngộ nghĩnh.", 10000, null, banhKeo, orion, "/uploads/keo_deo_chupachups.jpg", "SKU-BK-004", "8935006004", "Gói 55g", 70, "Kệ D2 - Kẹo ngọt", 80);

        // 25-28: Gia vị & Dầu ăn
        setupProductWithBatch("Dầu ăn Simply Đậu Nành 1L", "Dầu ăn nguyên chất 100% hạt đậu nành dồi dào Omega 3-6-9 tốt cho tim mạch.", 62000, 56000.0, giaVi, unilever, "/uploads/dau_an_simply.jpg", "SKU-GV-001", "8935007001", "Chai 1L", 45, "Kệ E1 - Dầu ăn", 360);
        setupProductWithBatch("Nước mắm Nam Ngư Đệ Nhị chai 900ml", "Nước mắm Nam Ngư đậm đà chuẩn vị truyền thống cho bữa cơm gia đình tròn vị.", 32000, 29000.0, giaVi, masan, "/uploads/nuoc_mam_nam_ngu.jpg", "SKU-GV-002", "8935007002", "Chai 900ml", 60, "Kệ E2 - Nước mắm", 360);
        setupProductWithBatch("Tương ớt Chinsu chai 250g", "Tương ớt cay nồng đậm vị từ ớt chỉ thiên chín cây thơm lừng mỗi món chiên xào.", 14000, null, giaVi, masan, "/uploads/tuong_ot_chinsu.jpg", "SKU-GV-003", "8935007003", "Chai 250g", 90, "Kệ E2 - Nước chấm", 300);
        setupProductWithBatch("Hạt nêm Knorr Thịt Thăn Xương Ống 400g", "Hạt nêm Knorr chiết xuất từ thịt thăn và tủy xương ngọt thanh nước dùng.", 36000, 32000.0, giaVi, unilever, "/uploads/hat_nem_knorr.jpg", "SKU-GV-004", "8935007004", "Gói 400g", 65, "Kệ E1 - Hạt nêm", 360);

        // 29-32: Hóa phẩm gia dụng
        setupProductWithBatch("Nước giặt OMO Matic Cửa Trên túi 2kg", "Công nghệ bọt xoáy thẩm thấu đánh bay vết bẩn cứng đầu cho quần áo trắng sáng.", 120000, 109000.0, hoaPham, unilever, "/uploads/nuoc_giat_omo.jpg", "SKU-HP-001", "8935008001", "Túi 2kg", 30, "Kệ F1 - Giặt tẩy", 720);
        setupProductWithBatch("Nước rửa chén Sunlight Chanh túi 750g", "Chiết xuất chanh tươi đánh sạch dầu mỡ trên chén dĩa chỉ trong một lần rửa.", 27000, 24000.0, hoaPham, unilever, "/uploads/nuoc_rua_chen_sunlight.jpg", "SKU-HP-002", "8935008002", "Túi 750g", 60, "Kệ F2 - Rửa chén", 720);
        setupProductWithBatch("Nước lau sàn Sunlight Tinh Dầu Hoa Hạ 1kg", "Lau sạch sáng bóng bề mặt gạch men và lưu hương hoa thơm ngát 24 giờ.", 32000, null, hoaPham, unilever, "/uploads/nuoc_lau_san_sunlight.jpg", "SKU-HP-003", "8935008003", "Chai 1kg", 45, "Kệ F2 - Lau sàn", 720);

        System.out.println("[PRODUCT & BATCH] Da cap nhat de 31 san pham voi anh local chuan va muc gia thi truong thuc te.");
    }

    private void setupProductWithBatch(
            String name, String desc, double price, Double salePrice,
            Category cat, Brand brand, String img, String sku, String barcode, String unit,
            int stock, String location, int expiryDays) {

        Product p = productRepository.findByName(name).orElseGet(() -> {
            Product prod = new Product();
            prod.setName(name);
            prod.setSku(sku);
            return prod;
        });

        p.setName(name);
        p.setDescription(desc);
        p.setPrice(BigDecimal.valueOf(price));
        p.setSalePrice(salePrice != null ? BigDecimal.valueOf(salePrice) : null);
        p.setCategory(cat);
        p.setBrand(brand);
        p.setMainImage(img);
        p.setSku(sku);
        p.setBarcode(barcode);
        p.setUnit(unit);
        p.setIsActive(true);
        Product saved = productRepository.save(p);

        // Tạo / cập nhật tồn kho
        Inventory inv = inventoryRepository.findByProductId(saved.getId()).orElseGet(() -> {
            Inventory newInv = new Inventory();
            newInv.setProduct(saved);
            return newInv;
        });
        inv.setCurrentStock(stock);
        inv.setMinimumStock(5);
        inv.setLocation(location);
        inv.setLastUpdated(LocalDateTime.now());
        inventoryRepository.save(inv);

        // Tạo lô hàng nếu chưa có
        if (productBatchRepository.findAll().stream().noneMatch(b -> b.getProduct().getId().equals(saved.getId()))) {
            ProductBatch batch = new ProductBatch();
            batch.setProduct(saved);
            batch.setBatchName("LÔ-" + saved.getSku());
            batch.setQuantity(stock);
            batch.setExpiryDate(LocalDate.now().plusDays(expiryDays));
            productBatchRepository.save(batch);
        }
    }

    // =========================================================
    // 6. COUPONS
    // =========================================================
    private void initializeCoupons() {
        if (couponRepository.count() > 0) return;

        Category rauCat = categoryRepository.findByName("Rau Củ Quả").orElse(null);
        Category thucPhamKho = categoryRepository.findByName("Thực Phẩm Khô").orElse(null);

        createCoupon("WELCOME10", "Giảm 10% cho đơn hàng đầu tiên (Tối đa 50k)", "PERCENTAGE", BigDecimal.valueOf(10), BigDecimal.valueOf(100000), BigDecimal.valueOf(50000), null, 500);
        createCoupon("RAUXANH15", "Giảm 15% cho danh mục Rau Củ Quả VietGAP (Tối đa 25k)", "PERCENTAGE", BigDecimal.valueOf(15), BigDecimal.valueOf(30000), BigDecimal.valueOf(25000), rauCat, 500);
        createCoupon("KHO20K", "Giảm 20.000đ cho Thực Phẩm Khô & Gia vị từ 100k", "FIXED_AMOUNT", BigDecimal.valueOf(20000), BigDecimal.valueOf(100000), null, thucPhamKho, 300);
        createCoupon("FREESHIP", "Miễn phí vận chuyển cho đơn hàng từ 200.000đ", "FIXED_AMOUNT", BigDecimal.valueOf(15000), BigDecimal.valueOf(200000), null, null, 1000);
        createCoupon("MINI50K", "Siêu voucher giảm 50.000đ cho đơn từ 500.000đ", "FIXED_AMOUNT", BigDecimal.valueOf(50000), BigDecimal.valueOf(500000), null, null, 200);

        System.out.println("[COUPON] Da tao cac ma giam gia WELCOME10, RAUXANH15, KHO20K, FREESHIP, MINI50K.");
    }

    private void createCoupon(String code, String desc, String type, BigDecimal val, BigDecimal minOrder, BigDecimal maxDiscount, Category applicableCat, int usage) {
        Coupon c = new Coupon();
        c.setCode(code);
        c.setDescription(desc);
        c.setDiscountType(type);
        c.setDiscountValue(val);
        c.setMinOrderAmount(minOrder);
        c.setMaxDiscountAmount(maxDiscount);
        c.setApplicableCategory(applicableCat);
        c.setMaxUses(usage);
        c.setUsedCount(0);
        c.setStartDate(LocalDateTime.now().minusDays(1));
        c.setEndDate(LocalDateTime.now().plusMonths(6));
        c.setIsActive(true);
        couponRepository.save(c);
    }

    // =========================================================
    // 7. PAYMENT CONFIGS (COD, BANK_TRANSFER, MOMO)
    // =========================================================
    private void initializePaymentConfigs() {
        if (paymentConfigRepository.count() > 0) return;

        // 1. COD
        PaymentMethodConfig cod = PaymentMethodConfig.builder()
                .methodKey("COD")
                .name("Thanh toán khi nhận hàng (COD)")
                .description("Nhận hàng, kiểm tra đầy đủ sản phẩm và thanh toán tiền mặt trực tiếp cho nhân viên giao hàng.")
                .isEnabled(true)
                .build();
        paymentConfigRepository.save(cod);

        // 2. Chuyển khoản ngân hàng qua mã QR
        PaymentMethodConfig bank = PaymentMethodConfig.builder()
                .methodKey("BANK_TRANSFER")
                .name("Chuyển khoản QR Ngân hàng (Vietcombank)")
                .description("Quét mã VietQR tiện lợi, xác nhận chuyển khoản để hoàn tất đơn hàng nhanh chóng.")
                .isEnabled(true)
                .bankName("Vietcombank - CN Hà Nội")
                .accountNumber("9988776655")
                .accountHolder("NGUYEN TIEN TAI - MINIMART")
                .qrCodeUrl("/uploads/maqr.jpg")
                .transferSyntax("MINIMART [MÃ ĐƠN HÀNG]")
                .build();
        paymentConfigRepository.save(bank);

        // 3. Ví MoMo
        PaymentMethodConfig momo = PaymentMethodConfig.builder()
                .methodKey("MOMO")
                .name("Ví điện tử MoMo")
                .description("Quét mã MoMo một chạm an toàn, bảo mật thông tin tài khoản.")
                .isEnabled(true)
                .bankName("Ví điện tử MoMo")
                .accountNumber("0336174371")
                .accountHolder("NGUYEN TIEN TAI")
                .qrCodeUrl("/uploads/maqr.jpg")
                .transferSyntax("MOMO [MÃ ĐƠN HÀNG]")
                .build();
        paymentConfigRepository.save(momo);

        System.out.println("[PAYMENT CONFIG] Da khoi tao 3 phuong thuc thanh toan: COD, BANK_TRANSFER, MOMO.");
    }

    // =========================================================
    // 8. SAMPLE ORDERS (Đơn mẫu hiện tại)
    // =========================================================
    private void initializeSampleOrders() {
        if (orderRepository.count() >= 5) {
            return;
        }

        User user1 = userRepository.findByUsername("user1").orElse(null);
        User user2 = userRepository.findByUsername("user2").orElse(null);
        User user3 = userRepository.findByUsername("user3").orElse(null);
        User shipper1 = userRepository.findByUsername("shipper1").orElse(null);
        User shipper2 = userRepository.findByUsername("shipper2").orElse(null);
        User shipper3 = userRepository.findByUsername("shipper3").orElse(null);
        List<Product> products = productRepository.findAll();

        if (user1 == null || products.size() < 4) return;

        createSingleOrder(
                user1, null, "CHO_XAC_NHAN", "COD", "PENDING",
                "Nguyễn Văn An", "0987654321", "123 Lũy Bán Bích, Phường Hòa Thạnh, Quận Tân Phú, TP. Hồ Chí Minh",
                "Giao giờ hành chính giúp tôi",
                List.of(products.get(0), products.get(2)), List.of(2, 1),
                LocalDateTime.now().minusHours(4)
        );

        createSingleOrder(
                user2, null, "DA_XAC_NHAN", "MOMO", "COMPLETED",
                "Trần Thị Bình", "0977888999", "45 Cộng Hòa, Phường 13, Quận Tân Bình, TP. Hồ Chí Minh",
                "Đã quét mã MoMo, gọi trước khi đến 15 phút",
                List.of(products.get(1), products.get(4)), List.of(1, 2),
                LocalDateTime.now().minusHours(3)
        );

        createSingleOrder(
                user1, shipper1, "DA_NHAN_DON", "COD", "PENDING",
                "Nguyễn Văn An", "0987654321", "88 Tân Kỳ Tân Quý, Phường Tân Sơn Nhì, Quận Tân Phú, TP. Hồ Chí Minh",
                "Shipper nhớ mang túi giữ lạnh cho đồ ăn",
                List.of(products.get(3)), List.of(3),
                LocalDateTime.now().minusHours(2)
        );

        createSingleOrder(
                user2, shipper2, "DANG_GIAO", "COD", "PENDING",
                "Trần Thị Bình", "0977888999", "202 Hoàng Văn Thụ, Phường 2, Quận Tân Bình, TP. Hồ Chí Minh",
                "Nhà trong hẻm cạnh cây xăng",
                List.of(products.get(5), products.get(6)), List.of(2, 1),
                LocalDateTime.now().minusHours(1)
        );

        Order completedOrder = createSingleOrder(
                user3, shipper3, "DA_GIAO", "COD", "COMPLETED",
                "Lê Hoàng Cường", "0933112233", "78 Lê Văn Khương, Phường Thới An, Quận 12, TP. Hồ Chí Minh",
                "Đã giao nhận tận tay khách",
                List.of(products.get(8)), List.of(2),
                LocalDateTime.now().minusMinutes(30)
        );
        if (completedOrder != null) {
            completedOrder.setDeliveredAt(LocalDateTime.now().minusMinutes(10));
            completedOrder.setDeliveryNote("Khách đã nhận đủ và thanh toán COD");
            orderRepository.save(completedOrder);
        }

        System.out.println("[ORDER] Da tao cac don hang mau ban dau (3 quan HCM: Tan Phu, Tan Binh, Quan 12).");
    }

    // =========================================================
    // 9. SEED 50 HISTORICAL ORDERS TRƯỚC THÁNG 4/2026 (GIỮ NGUYÊN ĐƠN HIỆN CÓ)
    // =========================================================
    private void seedFiftyHistoricalOrders() {
        if (orderRepository.count() >= 45) {
            System.out.println("[HISTORICAL ORDERS] Da ton tai >= 45 don hang -> Bo qua seed lich su.");
            return;
        }

        System.out.println("[HISTORICAL ORDERS] Bat dau seed 50 hoa don / don hang lich su trai dai T10/2025 -> T03/2026...");

        List<User> customers = List.of(
                userRepository.findByUsername("user1").orElse(null),
                userRepository.findByUsername("user2").orElse(null),
                userRepository.findByUsername("user3").orElse(null),
                userRepository.findByUsername("user4").orElse(null)
        );
        List<User> shippers = List.of(
                userRepository.findByUsername("shipper1").orElse(null),
                userRepository.findByUsername("shipper2").orElse(null),
                userRepository.findByUsername("shipper3").orElse(null)
        );

        List<Product> products = productRepository.findAll();
        if (products.size() < 10) return;

        // Danh sách 15 địa chỉ thực tế chi tiết tại TP.HCM (Tân Phú, Tân Bình, Quận 12)
        String[] addresses = {
                // Quận Tân Phú (5 địa chỉ - Shipper 1)
                "123 Lũy Bán Bích, Phường Hòa Thạnh, Quận Tân Phú, TP. Hồ Chí Minh",
                "45 Thoại Ngọc Hầu, Phường Phú Thạnh, Quận Tân Phú, TP. Hồ Chí Minh",
                "88 Tân Kỳ Tân Quý, Phường Tân Sơn Nhì, Quận Tân Phú, TP. Hồ Chí Minh",
                "15 Lê Trọng Tấn, Phường Tây Thạnh, Quận Tân Phú, TP. Hồ Chí Minh",
                "210 Vườn Lài, Phường Phú Thọ Hòa, Quận Tân Phú, TP. Hồ Chí Minh",

                // Quận Tân Bình (5 địa chỉ - Shipper 2)
                "45 Cộng Hòa, Phường 13, Quận Tân Bình, TP. Hồ Chí Minh",
                "202 Hoàng Văn Thụ, Phường 2, Quận Tân Bình, TP. Hồ Chí Minh",
                "73 Trường Chinh, Phường 12, Quận Tân Bình, TP. Hồ Chí Minh",
                "180 Phổ Quang, Phường 2, Quận Tân Bình, TP. Hồ Chí Minh",
                "56 Bạch Đằng, Phường 2, Quận Tân Bình, TP. Hồ Chí Minh",

                // Quận 12 (5 địa chỉ - Shipper 3)
                "78 Lê Văn Khương, Phường Thới An, Quận 12, TP. Hồ Chí Minh",
                "105 Nguyễn Ảnh Thủ, Phường Hiệp Thành, Quận 12, TP. Hồ Chí Minh",
                "340 Hà Huy Giáp, Phường Thạnh Lộc, Quận 12, TP. Hồ Chí Minh",
                "92 Tô Ký, Phường Tân Chánh Hiệp, Quận 12, TP. Hồ Chí Minh",
                "15 Quốc Lộ 1A, Phường An Phú Đông, Quận 12, TP. Hồ Chí Minh"
        };

        // Phân bổ 50 đơn qua 6 tháng:
        // T10/2025: 8 đơn
        // T11/2025: 8 đơn
        // T12/2025: 10 đơn
        // T01/2026: 9 đơn
        // T02/2026: 7 đơn
        // T03/2026: 8 đơn
        LocalDateTime[] dates = new LocalDateTime[50];
        // T10/2025 (8 đơn)
        dates[0] = LocalDateTime.of(2025, 10, 3, 9, 15);
        dates[1] = LocalDateTime.of(2025, 10, 7, 14, 30);
        dates[2] = LocalDateTime.of(2025, 10, 11, 10, 0);
        dates[3] = LocalDateTime.of(2025, 10, 15, 16, 45);
        dates[4] = LocalDateTime.of(2025, 10, 19, 11, 20);
        dates[5] = LocalDateTime.of(2025, 10, 22, 18, 10);
        dates[6] = LocalDateTime.of(2025, 10, 26, 13, 5);
        dates[7] = LocalDateTime.of(2025, 10, 30, 20, 0);

        // T11/2025 (8 đơn)
        dates[8] = LocalDateTime.of(2025, 11, 2, 8, 40);
        dates[9] = LocalDateTime.of(2025, 11, 6, 12, 15);
        dates[10] = LocalDateTime.of(2025, 11, 10, 17, 30);
        dates[11] = LocalDateTime.of(2025, 11, 14, 15, 10);
        dates[12] = LocalDateTime.of(2025, 11, 18, 10, 50);
        dates[13] = LocalDateTime.of(2025, 11, 21, 19, 25);
        dates[14] = LocalDateTime.of(2025, 11, 25, 14, 5);
        dates[15] = LocalDateTime.of(2025, 11, 29, 11, 35);

        // T12/2025 (10 đơn - Mùa giáng sinh & mua sắm cao điểm)
        dates[16] = LocalDateTime.of(2025, 12, 2, 9, 0);
        dates[17] = LocalDateTime.of(2025, 12, 5, 14, 15);
        dates[18] = LocalDateTime.of(2025, 12, 9, 16, 40);
        dates[19] = LocalDateTime.of(2025, 12, 13, 11, 20);
        dates[20] = LocalDateTime.of(2025, 12, 16, 18, 50);
        dates[21] = LocalDateTime.of(2025, 12, 20, 10, 10);
        dates[22] = LocalDateTime.of(2025, 12, 23, 15, 30);
        dates[23] = LocalDateTime.of(2025, 12, 24, 20, 15);
        dates[24] = LocalDateTime.of(2025, 12, 28, 13, 45);
        dates[25] = LocalDateTime.of(2025, 12, 31, 17, 0);

        // T01/2026 (9 đơn - Tết Nguyên Đán cận kề)
        dates[26] = LocalDateTime.of(2026, 1, 3, 10, 0);
        dates[27] = LocalDateTime.of(2026, 1, 7, 14, 20);
        dates[28] = LocalDateTime.of(2026, 1, 11, 16, 45);
        dates[29] = LocalDateTime.of(2026, 1, 15, 11, 30);
        dates[30] = LocalDateTime.of(2026, 1, 18, 18, 15);
        dates[31] = LocalDateTime.of(2026, 1, 21, 9, 40);
        dates[32] = LocalDateTime.of(2026, 1, 24, 15, 10);
        dates[33] = LocalDateTime.of(2026, 1, 27, 12, 0);
        dates[34] = LocalDateTime.of(2026, 1, 30, 17, 30);

        // T02/2026 (7 đơn)
        dates[35] = LocalDateTime.of(2026, 2, 3, 9, 20);
        dates[36] = LocalDateTime.of(2026, 2, 7, 14, 40);
        dates[37] = LocalDateTime.of(2026, 2, 12, 11, 10);
        dates[38] = LocalDateTime.of(2026, 2, 16, 16, 35);
        dates[39] = LocalDateTime.of(2026, 2, 20, 18, 0);
        dates[40] = LocalDateTime.of(2026, 2, 24, 13, 15);
        dates[41] = LocalDateTime.of(2026, 2, 27, 10, 45);

        // T03/2026 (8 đơn)
        dates[42] = LocalDateTime.of(2026, 3, 2, 11, 0);
        dates[43] = LocalDateTime.of(2026, 3, 6, 15, 20);
        dates[44] = LocalDateTime.of(2026, 3, 10, 9, 30);
        dates[45] = LocalDateTime.of(2026, 3, 14, 17, 45);
        dates[46] = LocalDateTime.of(2026, 3, 18, 14, 10);
        dates[47] = LocalDateTime.of(2026, 3, 22, 12, 35);
        dates[48] = LocalDateTime.of(2026, 3, 26, 19, 0);
        dates[49] = LocalDateTime.of(2026, 3, 30, 16, 15);

        // Tỷ lệ trạng thái: 42 HOAN_THANH, 3 DA_GIAO, 3 HUY, 2 GIAO_THAT_BAI
        for (int i = 0; i < 50; i++) {
            LocalDateTime orderTime = dates[i];
            User customer = customers.get(i % customers.size());
            String addr = addresses[i % addresses.length];
            User shipper = null;
            if (addr.contains("Tân Phú")) shipper = shippers.get(0); // shipper1: Tân Phú
            else if (addr.contains("Tân Bình")) shipper = shippers.get(1); // shipper2: Tân Bình
            else if (addr.contains("Quận 12")) shipper = shippers.get(2); // shipper3: Quận 12

            String status = "HOAN_THANH";
            String payStatus = "COMPLETED";
            String payMethod = (i % 3 == 0) ? "BANK_TRANSFER" : ((i % 3 == 1) ? "MOMO" : "COD");
            String failedReason = null;

            if (i == 47 || i == 48 || i == 49) {
                status = "DA_GIAO";
                payStatus = "COMPLETED";
            } else if (i == 15 || i == 25 || i == 39) {
                status = "HUY";
                payStatus = "FAILED";
            } else if (i == 10 || i == 32) {
                status = "GIAO_THAT_BAI";
                payStatus = "PENDING";
                failedReason = (i == 10) ? "Khách hàng không nghe máy sau 3 lần gọi" : "Sai địa chỉ nhận hàng, không liên lạc được";
            }

            // Chọn 2 - 4 mặt hàng cho mỗi đơn
            List<Product> orderProds = new ArrayList<>();
            List<Integer> quantities = new ArrayList<>();

            int numItems = 2 + (i % 3);
            for (int k = 0; k < numItems; k++) {
                Product prod = products.get((i * 3 + k) % products.size());
                if (!orderProds.contains(prod)) {
                    orderProds.add(prod);
                    quantities.add(1 + (k % 3));
                }
            }

            Order saved = createSingleOrder(
                    customer,
                    "HUY".equals(status) ? null : shipper,
                    status,
                    payMethod,
                    payStatus,
                    customer != null ? customer.getFullName() : "Khách Hàng MiniMart",
                    customer != null ? customer.getPhone() : "0987654321",
                    addr,
                    "Đơn hàng lịch sử tự động",
                    orderProds,
                    quantities,
                    orderTime
            );

            if (saved != null) {
                if ("HOAN_THANH".equals(status) || "DA_GIAO".equals(status)) {
                    saved.setDeliveredAt(orderTime.plusHours(2));
                    saved.setDeliveryNote("Giao hàng thành công đúng hẹn.");
                } else if ("GIAO_THAT_BAI".equals(status)) {
                    saved.setDeliveryFailedReason(failedReason);
                }
                orderRepository.save(saved);
            }
        }

        System.out.println("[HISTORICAL ORDERS] Da tao thanh cong 50 don hang va hoa don lich su (42 Hoan thanh, 3 Da giao, 3 Huy, 2 That bai).");
    }

    private Order createSingleOrder(
            User customer, User shipper, String status, String payMethod, String payStatus,
            String shipName, String shipPhone, String shipAddr, String note,
            List<Product> items, List<Integer> quantities, LocalDateTime orderTime) {

        BigDecimal total = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        Order order = new Order();
        order.setUser(customer);
        order.setShipper(shipper);
        order.setStatus(status);
        order.setShippingName(shipName);
        order.setShippingPhone(shipPhone);
        order.setShippingAddress(shipAddr);
        order.setPaymentMethod(payMethod);
        order.setNote(note);
        order.setCreatedAt(orderTime);

        for (int i = 0; i < items.size(); i++) {
            Product p = items.get(i);
            int q = quantities.get(i);
            BigDecimal itemPrice = p.getSalePrice() != null ? p.getSalePrice() : p.getPrice();
            total = total.add(itemPrice.multiply(BigDecimal.valueOf(q)));

            OrderItem oi = new OrderItem();
            oi.setOrder(order);
            oi.setProduct(p);
            oi.setProductName(p.getName());
            oi.setProductImage(p.getMainImage());
            oi.setPrice(itemPrice);
            oi.setQuantity(q);
            orderItems.add(oi);
        }

        // Áp dụng chiết khấu hạng hội viên nếu có
        BigDecimal discount = BigDecimal.ZERO;
        if (customer != null && customer.getMembershipTier() != null) {
            if ("SILVER".equals(customer.getMembershipTier())) discount = total.multiply(BigDecimal.valueOf(0.02));
            else if ("GOLD".equals(customer.getMembershipTier())) discount = total.multiply(BigDecimal.valueOf(0.05));
            else if ("DIAMOND".equals(customer.getMembershipTier())) discount = total.multiply(BigDecimal.valueOf(0.08));
        }

        order.setTotalAmount(total);
        order.setDiscountAmount(discount);
        order.setFinalAmount(total.subtract(discount).max(BigDecimal.ZERO));
        order.setItems(orderItems);

        Order saved = orderRepository.save(order);

        Payment payment = new Payment();
        payment.setOrder(saved);
        payment.setPaymentMethod(payMethod);
        payment.setPaymentStatus(payStatus);
        payment.setAmount(saved.getFinalAmount());
        if ("COMPLETED".equals(payStatus)) {
            payment.setPaidAt(orderTime.plusMinutes(15));
        }
        paymentRepository.save(payment);
        saved.setPayment(payment);

        return orderRepository.save(saved);
    }

    // =========================================================
    // 10. NORMALIZE EXISTING ORDERS & ADDRESSES TO 3 HCM DISTRICTS
    // =========================================================
    private void normalizeAllExistingOrdersAndAddresses() {
        System.out.println("[NORMALIZE] Bat dau dong bo toan bo don hang & dia chi ve 3 quan HCM (Tan Phu, Tan Binh, Quan 12)...");

        User shipper1 = userRepository.findByUsername("shipper1").orElse(null);
        User shipper2 = userRepository.findByUsername("shipper2").orElse(null);
        User shipper3 = userRepository.findByUsername("shipper3").orElse(null);

        String[] hcmAddresses = {
                // Tân Phú (Shipper 1)
                "123 Lũy Bán Bích, Phường Hòa Thạnh, Quận Tân Phú, TP. Hồ Chí Minh",
                "45 Thoại Ngọc Hầu, Phường Phú Thạnh, Quận Tân Phú, TP. Hồ Chí Minh",
                "88 Tân Kỳ Tân Quý, Phường Tân Sơn Nhì, Quận Tân Phú, TP. Hồ Chí Minh",
                "15 Lê Trọng Tấn, Phường Tây Thạnh, Quận Tân Phú, TP. Hồ Chí Minh",
                "210 Vườn Lài, Phường Phú Thọ Hòa, Quận Tân Phú, TP. Hồ Chí Minh",
                // Tân Bình (Shipper 2)
                "45 Cộng Hòa, Phường 13, Quận Tân Bình, TP. Hồ Chí Minh",
                "202 Hoàng Văn Thụ, Phường 2, Quận Tân Bình, TP. Hồ Chí Minh",
                "73 Trường Chinh, Phường 12, Quận Tân Bình, TP. Hồ Chí Minh",
                "180 Phổ Quang, Phường 2, Quận Tân Bình, TP. Hồ Chí Minh",
                "56 Bạch Đằng, Phường 2, Quận Tân Bình, TP. Hồ Chí Minh",
                // Quận 12 (Shipper 3)
                "78 Lê Văn Khương, Phường Thới An, Quận 12, TP. Hồ Chí Minh",
                "105 Nguyễn Ảnh Thủ, Phường Hiệp Thành, Quận 12, TP. Hồ Chí Minh",
                "340 Hà Huy Giáp, Phường Thạnh Lộc, Quận 12, TP. Hồ Chí Minh",
                "92 Tô Ký, Phường Tân Chánh Hiệp, Quận 12, TP. Hồ Chí Minh",
                "15 Quốc Lộ 1A, Phường An Phú Đông, Quận 12, TP. Hồ Chí Minh"
        };

        List<Order> orders = orderRepository.findAll();
        int addrIdx = 0;
        int updatedCount = 0;

        for (Order order : orders) {
            String addr = order.getShippingAddress();
            boolean changed = false;

            if (addr == null || (!addr.contains("Tân Phú") && !addr.contains("Tân Bình") && !addr.contains("Quận 12"))) {
                addr = hcmAddresses[addrIdx % hcmAddresses.length];
                order.setShippingAddress(addr);
                changed = true;
                addrIdx++;
            }

            if (!"HUY".equals(order.getStatus())) {
                User correctShipper = null;
                if (addr.contains("Tân Phú")) correctShipper = shipper1;
                else if (addr.contains("Tân Bình")) correctShipper = shipper2;
                else if (addr.contains("Quận 12")) correctShipper = shipper3;

                if (correctShipper != null && (order.getShipper() == null || !order.getShipper().getId().equals(correctShipper.getId()))) {
                    order.setShipper(correctShipper);
                    changed = true;
                }
            }

            if (changed) {
                orderRepository.save(order);
                updatedCount++;
            }
        }

        // Tự động thăng hạng tích lũy cho tài khoản demo theo đúng yêu cầu đã chốt
        userRepository.findByUsername("user1").ifPresent(u -> {
            u.setMembershipTier("DIAMOND");
            u.setLoyaltyPoints(Math.max(u.getLoyaltyPoints() != null ? u.getLoyaltyPoints() : 0, 1250));
            userRepository.save(u);
        });
        userRepository.findByUsername("user2").ifPresent(u -> {
            u.setMembershipTier("GOLD");
            u.setLoyaltyPoints(Math.max(u.getLoyaltyPoints() != null ? u.getLoyaltyPoints() : 0, 620));
            userRepository.save(u);
        });
        userRepository.findByUsername("user3").ifPresent(u -> {
            u.setMembershipTier("SILVER");
            u.setLoyaltyPoints(Math.max(u.getLoyaltyPoints() != null ? u.getLoyaltyPoints() : 0, 210));
            userRepository.save(u);
        });
        userRepository.findByUsername("user4").ifPresent(u -> {
            u.setMembershipTier("BRONZE");
            if (u.getLoyaltyPoints() == null) u.setLoyaltyPoints(45);
            userRepository.save(u);
        });

        System.out.println("[NORMALIZE] Hoan tat dong bo " + updatedCount + " don hang ve 3 quan HCM va phan bo dung Shipper!");
    }
}