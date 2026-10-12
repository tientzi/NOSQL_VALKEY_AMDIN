package com.groceryshop.controller;

import com.groceryshop.entity.Order;
import com.groceryshop.entity.Payment;
import com.groceryshop.entity.PaymentMethodConfig;
import com.groceryshop.repository.OrderRepository;
import com.groceryshop.repository.PaymentMethodConfigRepository;
import com.groceryshop.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
public class PaymentConfigController {

    @Autowired
    private PaymentMethodConfigRepository paymentMethodConfigRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private com.groceryshop.service.FileStorageService fileStorageService;

    @Autowired
    private com.groceryshop.service.OrderService orderService;

    /**
     * Public API cho trang Checkout: Chỉ lấy các phương thức đang BẬT
     */
    @GetMapping("/api/public/payment-methods")
    public ResponseEntity<List<PaymentMethodConfig>> getPublicPaymentMethods() {
        return ResponseEntity.ok(paymentMethodConfigRepository.findByIsEnabledTrue());
    }

    /**
     * Admin API: Lấy toàn bộ cấu hình phương thức thanh toán
     */
    @GetMapping("/api/admin/payment-methods")
    public ResponseEntity<List<PaymentMethodConfig>> getAllPaymentMethods() {
        return ResponseEntity.ok(paymentMethodConfigRepository.findAll());
    }

    /**
     * Admin API: Tải lên ảnh mã QR riêng biệt và tự động xóa ảnh cũ nếu có
     */
    @PostMapping(value = "/api/admin/payment-methods/upload-qr", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadQrImage(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @RequestParam(value = "oldQrUrl", required = false) String oldQrUrl) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Vui lòng chọn file ảnh mã QR hợp lệ"));
            }

            // Tự động xóa file ảnh QR cũ khỏi server (uploads/) theo đúng yêu cầu đã chốt (Q4)
            if (oldQrUrl != null && !oldQrUrl.trim().isEmpty() && oldQrUrl.startsWith("/uploads/")) {
                fileStorageService.deleteFile(oldQrUrl.trim());
            }

            String savedUrl = fileStorageService.saveFile(file);
            return ResponseEntity.ok(Map.of("url", savedUrl));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi khi tải ảnh mã QR: " + e.getMessage()));
        }
    }

    /**
     * Admin API: Cập nhật cấu hình phương thức thanh toán
     */
    @PutMapping("/api/admin/payment-methods/{id}")
    public ResponseEntity<?> updatePaymentMethod(
            @PathVariable Long id,
            @RequestBody PaymentMethodConfig request) {
        PaymentMethodConfig config = paymentMethodConfigRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cấu hình phương thức thanh toán #" + id));

        if (request.getName() != null) config.setName(request.getName());
        if (request.getDescription() != null) config.setDescription(request.getDescription());
        if (request.getIsEnabled() != null) config.setIsEnabled(request.getIsEnabled());
        if (request.getBankName() != null) config.setBankName(request.getBankName());
        if (request.getAccountNumber() != null) config.setAccountNumber(request.getAccountNumber());
        if (request.getAccountHolder() != null) config.setAccountHolder(request.getAccountHolder());
        if (request.getTransferSyntax() != null) config.setTransferSyntax(request.getTransferSyntax());

        String newQr = request.getQrCodeUrl() != null ? request.getQrCodeUrl() : request.getQrImageUrl();
        String oldQr = config.getQrCodeUrl() != null ? config.getQrCodeUrl() : config.getQrImageUrl();
        if (newQr != null && !newQr.equals(oldQr)) {
            if (oldQr != null && oldQr.startsWith("/uploads/") && !oldQr.equals(newQr)) {
                fileStorageService.deleteFile(oldQr);
            }
            config.setQrCodeUrl(newQr);
            config.setQrImageUrl(newQr);
        }

        config.setUpdatedAt(LocalDateTime.now());

        return ResponseEntity.ok(paymentMethodConfigRepository.save(config));
    }

    /**
     * Admin API: Lấy danh sách lịch sử giao dịch thanh toán để đối soát
     */
    @GetMapping("/api/admin/payments/transactions")
    public ResponseEntity<?> getAllTransactions() {
        List<Payment> payments = paymentRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Payment p : payments) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", p.getId());
            map.put("orderId", p.getOrder() != null ? p.getOrder().getId() : null);
            map.put("customerName", p.getOrder() != null ? p.getOrder().getShippingName() : "—");
            map.put("orderStatus", p.getOrder() != null ? p.getOrder().getStatus() : null);
            map.put("deliveryNote", p.getOrder() != null ? p.getOrder().getDeliveryNote() : null);
            map.put("paymentMethod", p.getPaymentMethod());
            map.put("paymentStatus", p.getPaymentStatus());
            map.put("amount", p.getAmount());
            map.put("paidAt", p.getPaidAt());
            map.put("createdAt", p.getOrder() != null ? p.getOrder().getCreatedAt() : null);
            result.add(map);
        }
        result.sort((a, b) -> {
            LocalDateTime tA = (LocalDateTime) a.get("createdAt");
            LocalDateTime tB = (LocalDateTime) b.get("createdAt");
            if (tA == null) return 1;
            if (tB == null) return -1;
            return tB.compareTo(tA);
        });
        return ResponseEntity.ok(result);
    }

    /**
     * Admin API: Duyệt giao dịch thanh toán của khách hàng
     */
    @PostMapping("/api/admin/payments/transactions/{id}/approve")
    @Transactional
    public ResponseEntity<?> approvePayment(@PathVariable Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giao dịch thanh toán #" + id));

        boolean isCod = "TIEN_MAT".equalsIgnoreCase(payment.getPaymentMethod()) || "COD".equalsIgnoreCase(payment.getPaymentMethod());
        if (isCod) {
            payment.setPaymentStatus("APPROVED_COD");
        } else {
            payment.setPaymentStatus("COMPLETED");
            payment.setPaidAt(LocalDateTime.now());
        }
        paymentRepository.save(payment);

        Order order = payment.getOrder();
        if (order != null && "CHO_XAC_NHAN".equals(order.getStatus())) {
            order.setStatus("DA_XAC_NHAN");
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);
        }

        Map<String, Object> res = new HashMap<>();
        res.put("message", isCod ? "Đã duyệt đơn hàng Tiền mặt (COD) thành công!" : "Đã duyệt thanh toán QR thành công!");
        res.put("paymentStatus", payment.getPaymentStatus());
        return ResponseEntity.ok(res);
    }

    /**
     * Admin API: Từ chối giao dịch thanh toán
     */
    @PostMapping("/api/admin/payments/transactions/{id}/reject")
    @Transactional
    public ResponseEntity<?> rejectPayment(@PathVariable Long id, @RequestBody(required = false) Map<String, String> payload) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giao dịch thanh toán #" + id));

        payment.setPaymentStatus("FAILED");
        paymentRepository.save(payment);

        String reason = payload != null ? payload.getOrDefault("reason", "Chưa nhận được tiền hoặc thông tin thanh toán không hợp lệ") : "Chưa nhận được tiền";
        Order order = payment.getOrder();
        if (order != null) {
            order.setStatus("HUY");
            order.setDeliveryNote("[Từ chối thanh toán]: " + reason);
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);

            // Tự động hoàn lại tồn kho, lô hàng và mã giảm giá
            orderService.restoreInventoryAndCoupon(order, null, "Từ chối thanh toán đơn #" + order.getId() + ": " + reason);
        }

        Map<String, Object> res = new HashMap<>();
        res.put("message", "Đã từ chối thanh toán, hủy đơn và hoàn trả kho thành công!");
        res.put("paymentStatus", "FAILED");
        return ResponseEntity.ok(res);
    }
}
