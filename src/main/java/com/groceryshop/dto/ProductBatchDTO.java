package com.groceryshop.dto;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductBatchDTO {
    private Long id;
    private Long productId;
    private String productName;
    private String productSku;
    private Long goodsReceiptId;
    private String batchName;
    private Integer quantity;
    private LocalDate expiryDate;
    private Integer discountPercentage;
    private java.math.BigDecimal importPrice;
    private java.math.BigDecimal originalPrice;
    private java.math.BigDecimal salePrice;
    private String status;
    private Boolean isExpired;
    private Long daysRemaining;
    private java.math.BigDecimal totalLoss;
    private LocalDateTime createdAt;
}
