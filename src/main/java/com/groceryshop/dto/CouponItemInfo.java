package com.groceryshop.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponItemInfo {
    private Long productId;
    private Long categoryId;
    private BigDecimal price;
    private Integer quantity;
}
