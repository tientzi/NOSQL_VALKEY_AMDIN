package com.groceryshop.controller;

import com.groceryshop.dto.ProductBatchDTO;
import com.groceryshop.security.UserPrincipal;
import com.groceryshop.service.ProductBatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/batches")
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('ADMIN')")
public class ProductBatchController {

    @Autowired
    private ProductBatchService batchService;

    @GetMapping
    public ResponseEntity<List<ProductBatchDTO>> getAllBatches() {
        return ResponseEntity.ok(batchService.getAllBatches());
    }

    @GetMapping("/expiring")
    public ResponseEntity<List<ProductBatchDTO>> getExpiringBatches(@RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(batchService.getExpiringBatches(days));
    }

    @GetMapping("/expired")
    public ResponseEntity<List<ProductBatchDTO>> getExpiredBatches() {
        return ResponseEntity.ok(batchService.getExpiredBatches());
    }

    @PostMapping("/{batchId}/dispose")
    public ResponseEntity<Map<String, String>> disposeBatch(
            @AuthenticationPrincipal UserPrincipal userDetails,
            @PathVariable Long batchId) {
        Long userId = userDetails != null ? userDetails.getId() : null;
        batchService.disposeBatch(batchId, userId);
        return ResponseEntity.ok(Map.of("message", "Đã xuất hủy lô hàng quá hạn thành công và ghi sổ cái kho."));
    }

    @PostMapping("/batch/{batchId}/clearance-sale")
    public ResponseEntity<Map<String, String>> applyBatchClearanceSale(
            @PathVariable Long batchId,
            @RequestBody Map<String, Object> payload) {
        Integer discount = payload.get("discountPercentage") != null && !payload.get("discountPercentage").toString().isBlank()
                ? Integer.parseInt(payload.get("discountPercentage").toString())
                : null;
        Double customSalePrice = payload.get("customSalePrice") != null && !payload.get("customSalePrice").toString().isBlank()
                ? Double.parseDouble(payload.get("customSalePrice").toString())
                : null;

        batchService.applyBatchClearanceSale(batchId, discount, customSalePrice);
        return ResponseEntity.ok(Map.of("message", "Đã cập nhật giá khuyến mãi xả hàng cho lô thành công"));
    }

    @PostMapping("/{productId}/clearance-sale")
    public ResponseEntity<Map<String, String>> applyClearanceSale(
            @PathVariable Long productId,
            @RequestBody Map<String, Object> payload) {
        Integer discount = payload.get("discountPercentage") != null && !payload.get("discountPercentage").toString().isBlank()
                ? Integer.parseInt(payload.get("discountPercentage").toString())
                : null;
        Double customSalePrice = payload.get("customSalePrice") != null && !payload.get("customSalePrice").toString().isBlank()
                ? Double.parseDouble(payload.get("customSalePrice").toString())
                : null;

        batchService.applyClearanceSale(productId, discount, customSalePrice);
        return ResponseEntity.ok(Map.of("message", "Đã cập nhật giá khuyến mãi xả hàng thành công"));
    }
}
