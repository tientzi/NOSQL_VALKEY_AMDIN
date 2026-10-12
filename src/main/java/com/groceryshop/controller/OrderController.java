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
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping("/api/orders")
    public ResponseEntity<OrderDTO> createOrder(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKeyHeader,
            @RequestBody OrderDTO orderDTO) {
        if (idempotencyKeyHeader != null && !idempotencyKeyHeader.trim().isEmpty() && orderDTO.getIdempotencyKey() == null) {
            orderDTO.setIdempotencyKey(idempotencyKeyHeader.trim());
        }
        OrderDTO created = orderService.createOrder(userPrincipal.getId(), orderDTO);
        return ResponseEntity.status(org.springframework.http.HttpStatus.ACCEPTED).body(created);
    }

    @GetMapping("/api/orders/jobs/{jobId}/status")
    public ResponseEntity<Map<String, Object>> getJobStatus(@PathVariable String jobId) {
        return ResponseEntity.ok(orderService.getJobStatus(jobId));
    }

    @GetMapping("/api/orders/my-orders")
    public ResponseEntity<List<OrderDTO>> getMyOrders(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(orderService.getMyOrders(userPrincipal.getId()));
    }

    @PutMapping("/api/orders/{id}/cancel")
    public ResponseEntity<OrderDTO> cancelOrder(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id) {
        return ResponseEntity.ok(orderService.cancelOrder(userPrincipal.getId(), id));
    }

    @GetMapping("/api/admin/orders")
    public ResponseEntity<List<OrderDTO>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @GetMapping("/api/admin/orders/{id}")
    public ResponseEntity<OrderDTO> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @PutMapping("/api/admin/orders/{id}/status")
    public ResponseEntity<OrderDTO> updateOrderStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        String status = payload.get("status");
        return ResponseEntity.ok(orderService.updateOrderStatus(id, status));
    }

    @PatchMapping("/api/admin/orders/{id}/confirm-payment")
    public ResponseEntity<OrderDTO> confirmPayment(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.confirmPayment(id));
    }
}
