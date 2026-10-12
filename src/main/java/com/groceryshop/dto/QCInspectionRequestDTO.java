package com.groceryshop.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QCInspectionRequestDTO {
    private String generalNote;
    private List<ItemQCResult> items;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ItemQCResult {
        private Long itemId;
        private Integer passedQuantity;
        private Integer rejectedQuantity;
        private String rejectReason;
        private String qcNote;
    }
}
