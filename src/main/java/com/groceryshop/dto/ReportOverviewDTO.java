package com.groceryshop.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportOverviewDTO {
    private BigDecimal totalRevenue;
    private Long totalOrders;
    private Long totalCustomers;
    private Long totalProductsSold;
    
    // Doanh thu theo 6 tháng (T10/2025 -> T03/2026)
    private List<Map<String, Object>> monthlyRevenues;

    // Cơ cấu trạng thái đơn
    private List<Map<String, Object>> orderStatusDistribution;

    // Tỷ trọng phương thức thanh toán
    private List<Map<String, Object>> paymentMethodDistribution;

    // Top 10 sản phẩm bán chạy
    private List<Map<String, Object>> topSellingProducts;

    // Hiệu suất của đội ngũ Shipper
    private List<Map<String, Object>> shipperPerformance;
}
