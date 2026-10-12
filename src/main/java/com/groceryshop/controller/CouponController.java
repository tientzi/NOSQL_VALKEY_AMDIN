package com.groceryshop.controller;

import com.groceryshop.dto.CouponDTO;
import com.groceryshop.service.CouponService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class CouponController {

    @Autowired
    private CouponService couponService;

    @PostMapping("/api/public/coupons/apply")
    public ResponseEntity<?> applyCoupon(@RequestBody Map<String, Object> payload) {
        String code = payload.get("code").toString();
        BigDecimal amount = new BigDecimal(payload.get("amount").toString());
        List<com.groceryshop.dto.CouponItemInfo> items = new java.util.ArrayList<>();
        if (payload.containsKey("items") && payload.get("items") instanceof List) {
            List<?> rawList = (List<?>) payload.get("items");
            for (Object obj : rawList) {
                if (obj instanceof Map) {
                    Map<?, ?> itemMap = (Map<?, ?>) obj;
                    Long productId = itemMap.get("productId") != null ? Long.valueOf(itemMap.get("productId").toString()) : null;
                    Long categoryId = itemMap.get("categoryId") != null ? Long.valueOf(itemMap.get("categoryId").toString()) : null;
                    BigDecimal price = itemMap.get("price") != null ? new BigDecimal(itemMap.get("price").toString()) : BigDecimal.ZERO;
                    Integer quantity = itemMap.get("quantity") != null ? Integer.valueOf(itemMap.get("quantity").toString()) : 1;
                    items.add(new com.groceryshop.dto.CouponItemInfo(productId, categoryId, price, quantity));
                }
            }
        }
        BigDecimal discount = couponService.calculateDiscountForItems(code, amount, items);
        Map<String, Object> response = new HashMap<>();
        response.put("discountAmount", discount);
        response.put("code", code);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/public/coupons")
    public ResponseEntity<List<CouponDTO>> getPublicCoupons() {
        return ResponseEntity.ok(couponService.getActivePublicCoupons());
    }

    @GetMapping("/api/admin/coupons")
    public ResponseEntity<List<CouponDTO>> getAllCoupons() {
        return ResponseEntity.ok(couponService.getAllCoupons());
    }

    @GetMapping("/api/admin/coupons/{id}")
    public ResponseEntity<CouponDTO> getCouponById(@PathVariable Long id) {
        return ResponseEntity.ok(couponService.getCouponById(id));
    }

    @PostMapping("/api/admin/coupons")
    public ResponseEntity<CouponDTO> createCoupon(@RequestBody CouponDTO dto) {
        return ResponseEntity.ok(couponService.createCoupon(dto));
    }

    @PutMapping("/api/admin/coupons/{id}")
    public ResponseEntity<CouponDTO> updateCoupon(@PathVariable Long id, @RequestBody CouponDTO dto) {
        return ResponseEntity.ok(couponService.updateCoupon(id, dto));
    }

    @DeleteMapping("/api/admin/coupons/{id}")
    public ResponseEntity<?> deleteCoupon(@PathVariable Long id) {
        couponService.deleteCoupon(id);
        return ResponseEntity.ok().body("{\"message\": \"Xóa coupon thành công\"}");
    }
}
