package com.groceryshop.controller;

import com.groceryshop.config.DataInitializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RestoreController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataInitializer dataInitializer;

    @GetMapping("/api/public/restore-data")
    public String restore() {
        try {
            // Bước 1: Xóa toàn bộ dữ liệu theo thứ tự FK-safe
            jdbcTemplate.execute("DELETE FROM payments");
            jdbcTemplate.execute("DELETE FROM order_items");
            jdbcTemplate.execute("DELETE FROM orders");
            jdbcTemplate.execute("DELETE FROM reviews");
            jdbcTemplate.execute("DELETE FROM cart_items");
            jdbcTemplate.execute("DELETE FROM cart");
            jdbcTemplate.execute("DELETE FROM inventory");
            jdbcTemplate.execute("DELETE FROM product_images");
            jdbcTemplate.execute("DELETE FROM products");
            jdbcTemplate.execute("DELETE FROM addresses");
            jdbcTemplate.execute("DELETE FROM users");
            jdbcTemplate.execute("DELETE FROM roles");
            jdbcTemplate.execute("DELETE FROM categories");
            jdbcTemplate.execute("DELETE FROM brands");
            jdbcTemplate.execute("DELETE FROM coupons");

            // Bước 2: Reset IDENTITY về 0
            jdbcTemplate.execute("DBCC CHECKIDENT ('roles',        RESEED, 0)");
            jdbcTemplate.execute("DBCC CHECKIDENT ('users',        RESEED, 0)");
            jdbcTemplate.execute("DBCC CHECKIDENT ('categories',   RESEED, 0)");
            jdbcTemplate.execute("DBCC CHECKIDENT ('brands',       RESEED, 0)");
            jdbcTemplate.execute("DBCC CHECKIDENT ('products',     RESEED, 0)");
            jdbcTemplate.execute("DBCC CHECKIDENT ('product_images', RESEED, 0)");
            jdbcTemplate.execute("DBCC CHECKIDENT ('inventory',    RESEED, 0)");
            jdbcTemplate.execute("DBCC CHECKIDENT ('cart',         RESEED, 0)");
            jdbcTemplate.execute("DBCC CHECKIDENT ('cart_items',   RESEED, 0)");
            jdbcTemplate.execute("DBCC CHECKIDENT ('orders',       RESEED, 0)");
            jdbcTemplate.execute("DBCC CHECKIDENT ('order_items',  RESEED, 0)");
            jdbcTemplate.execute("DBCC CHECKIDENT ('payments',     RESEED, 0)");
            jdbcTemplate.execute("DBCC CHECKIDENT ('reviews',      RESEED, 0)");
            jdbcTemplate.execute("DBCC CHECKIDENT ('coupons',      RESEED, 0)");
            jdbcTemplate.execute("DBCC CHECKIDENT ('addresses',    RESEED, 0)");

            // Bước 3: Gọi DataInitializer để insert lại bằng JPA (Unicode hoàn toàn chính xác)
            dataInitializer.run();

            return "✅ Khôi phục dữ liệu thành công! Tiếng Việt đã được sửa đúng.";
        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Lỗi khi khôi phục: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
        }
    }
}
