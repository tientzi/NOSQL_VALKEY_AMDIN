package com.groceryshop.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoodsReceiptDTO {
    private Long id;
    private Long supplierId;
    private String supplierName;
    private Long brandId;
    private String brandName;
    private BigDecimal totalAmount;
    private String note;
    private String status;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<GoodsReceiptItemDTO> items;
}
