package com.groceryshop.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryLedgerDTO {
    private Long id;
    private Long inventoryId;
    private String productName;
    private String changeType;
    private Integer quantityChange;
    private Integer previousStock;
    private Integer newStock;
    private Long referenceId;
    private String note;
    private String createdBy;
    private LocalDateTime createdAt;
}
