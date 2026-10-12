package com.groceryshop.controller;

import com.groceryshop.entity.Inventory;
import com.groceryshop.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/inventory")
public class InventoryController {

    @Autowired
    private InventoryService inventoryService;

    @GetMapping
    public ResponseEntity<List<Inventory>> getAllInventory() {
        return ResponseEntity.ok(inventoryService.getAllInventory());
    }

    @GetMapping("/low-stock")
    public ResponseEntity<List<Inventory>> getLowStockWarnings() {
        return ResponseEntity.ok(inventoryService.getLowStockWarnings());
    }

    /**
     * Chỉ cho phép cập nhật vị trí kệ hoặc ngưỡng tối thiểu, không cho phép can thiệp số lượng tồn.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Inventory> updateInventoryInfo(
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload) {
        String location = payload.get("location") != null ? payload.get("location").toString() : null;
        Integer minimumStock = payload.get("minimumStock") != null ? Integer.parseInt(payload.get("minimumStock").toString()) : null;
        return ResponseEntity.ok(inventoryService.updateInventoryInfo(id, location, minimumStock));
    }

    /**
     * Chặn thao tác cộng tồn trực tiếp, yêu cầu lập Phiếu Nhập Kho theo quy trình chuẩn.
     */
    @PostMapping("/{id}/add-stock")
    public ResponseEntity<Map<String, String>> addStock(
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload) {
        return ResponseEntity.badRequest().body(Map.of(
            "error", "Thao tác bị khóa! Quy trình chuẩn: Tồn kho chỉ được gia tăng thông qua Phiếu Nhập Kho (Goods Receipt). Vui lòng tạo Phiếu nhập kho."
        ));
    }
}
