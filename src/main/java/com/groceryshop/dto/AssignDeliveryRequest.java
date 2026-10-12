package com.groceryshop.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignDeliveryRequest {
    private List<Long> orderIds;
    private Long shipperId;
    private String note;
}
