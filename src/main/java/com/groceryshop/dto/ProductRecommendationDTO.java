package com.groceryshop.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRecommendationDTO {
    private ProductDTO product;
    private Double confidence; // ví dụ 0.85 -> 85%
    private Double lift;       // ví dụ 2.4x
    private String reason;     // "Thường mua cùng" hoặc "Cùng danh mục"
}
