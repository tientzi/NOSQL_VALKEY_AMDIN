package com.groceryshop.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierProductDTO {
    private Long id;
    private String name;
    private String sku;
    private String mainImage;
    private String categoryName;
    private String brandName;
    private BigDecimal price;
    private Boolean isActive;
    private Integer currentStock;
    private Integer totalSuppliedQuantity;
    private LocalDateTime lastImportDate;
}
