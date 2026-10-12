package com.groceryshop.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponDTO {
    private Long id;
    private String code;
    private String description;
    private String discountType;
    private BigDecimal discountValue;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private BigDecimal minOrderAmount;
    private Integer usedCount;
    private Integer maxUses;
    private Boolean isActive;
    private Long applicableCategoryId;
    private String applicableCategoryName;
    private BigDecimal maxDiscountAmount;
}
