package com.groceryshop.controller;

import com.groceryshop.dto.OrderDTO;
import com.groceryshop.security.UserPrincipal;
import com.groceryshop.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/shipper/orders")
public class ShipperController {

    @Autowired
    private OrderService orderService;

    /**
     * Lấy danh sách các đơn hàng đã xác nhận, chưa có shipper nhận
     */
    @GetMapping("/available")
    public ResponseEntity<List<OrderDTO>> getAvailableOrders() {
        return ResponseEntity.ok(orderService.getAvailableOrdersForShipper());
    }

    /**
     * Khóa API tự nhận đơn: Đơn hàng chỉ được phân bổ bởi Admin theo khu vực
     */
    @PostMapping("/{id}/accept")
    public ResponseEntity<?> acceptOrder(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        throw new com.groceryshop.exception.BadRequestException("Tính năng tự nhận đơn đã chuyển sang cơ chế điều phối tự động bởi Admin theo khu vực giao hàng.");
    }

    /**
     * Lấy danh sách các đơn hàng của chính shipper đang đăng nhập
     */
    @GetMapping("/my-deliveries")
    public ResponseEntity<List<OrderDTO>> getMyDeliveries(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(orderService.getMyDeliveries(userPrincipal.getId()));
    }

    /**
     * Xem chi tiết 1 đơn hàng (gồm danh sách sản phẩm, địa chỉ, người nhận, COD/MoMo)
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderDTO> getOrderDetail(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    /**
     * Cập nhật trạng thái giao hàng (DANG_GIAO, DA_GIAO, GIAO_THAT_BAI)
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderDTO> updateDeliveryStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody Map<String, String> payload) {
        String status = payload.get("status");
        String note = payload.get("note");
        String reason = payload.get("reason");
        return ResponseEntity.ok(orderService.updateDeliveryStatus(id, userPrincipal.getId(), status, note, reason));
    }

    /**
     * Backward-compatibility cho các gọi GET /api/shipper/orders cũ
     */
    @GetMapping
    public ResponseEntity<List<OrderDTO>> getAllShipperOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }
}
