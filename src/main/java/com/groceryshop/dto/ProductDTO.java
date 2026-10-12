package com.groceryshop.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDTO {
    private Long id;

    @NotBlank(message = "Tên sản phẩm không được để trống")
    private String name;

    private String description;

    @NotNull(message = "Giá sản phẩm không được để trống")
    @DecimalMin(value = "0.0", message = "Giá sản phẩm không được âm")
    private BigDecimal price;

    @DecimalMin(value = "0.0", message = "Giá khuyến mãi không được âm")
    private BigDecimal salePrice;

    @NotNull(message = "Danh mục sản phẩm không được để trống")
    private Long categoryId;
    private String categoryName;

    @NotNull(message = "Thương hiệu sản phẩm không được để trống")
    private Long brandId;
    private String brandName;

    private String mainImage;
    private Boolean isActive;

    private String sku;
    private String barcode;
    private String unit;
    private Integer weightG;

    private List<String> images;

    @Min(value = 0, message = "Số lượng tồn kho không được âm")
    private Integer currentStock;
    
    private Integer minimumStock;
    private String location;
}
