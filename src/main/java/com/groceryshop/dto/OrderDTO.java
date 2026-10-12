package com.groceryshop.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.Pattern;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDTO {
    private Long id;
    private Long userId;
    private String username;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private String status;
    private String shippingName;
    @Pattern(regexp = "^\\d{10}$", message = "Số điện thoại phải có đúng 10 chữ số")
    private String shippingPhone;
    private String shippingAddress;
    private String paymentMethod;
    private String paymentStatus;
    private String couponCode;
    private String note;
    private Long shipperId;
    private String shipperName;
    private String shipperPhone;
    private String deliveryNote;
    private LocalDateTime deliveredAt;
    private String deliveryFailedReason;
    private String detectedDistrict;
    private String membershipTier;
    private BigDecimal tierDiscountAmount;
    private String idempotencyKey;
    private String jobId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<OrderItemDTO> items;
}
