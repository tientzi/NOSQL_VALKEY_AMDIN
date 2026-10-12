package com.groceryshop.service;

import com.groceryshop.dto.DashboardStatsDTO;
import com.groceryshop.entity.Order;
import com.groceryshop.repository.OrderRepository;
import com.groceryshop.repository.ProductRepository;
import com.groceryshop.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class DashboardService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    public DashboardStatsDTO getStats() {
        List<Order> allOrders = orderRepository.findAll();

        BigDecimal totalRevenue = allOrders.stream()
                .filter(order -> "HOAN_THANH".equals(order.getStatus()) && order.getFinalAmount() != null)
                .map(Order::getFinalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalOrdersCount = allOrders.size();

        long totalCustomersCount = userRepository.findAll().stream()
                .filter(u -> u.getRole() != null && "ROLE_USER".equals(u.getRole().getName()))
                .count();

        long totalProductsCount = productRepository.count();

        Map<String, BigDecimal> monthlyRevenue = new TreeMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");

        allOrders.stream()
                .filter(order -> "HOAN_THANH".equals(order.getStatus()) && order.getCreatedAt() != null && order.getFinalAmount() != null)
                .forEach(order -> {
                    String month = order.getCreatedAt().format(formatter);
                    BigDecimal amount = order.getFinalAmount();
                    monthlyRevenue.put(month, monthlyRevenue.getOrDefault(month, BigDecimal.ZERO).add(amount));
                });

        return DashboardStatsDTO.builder()
                .totalRevenue(totalRevenue)
                .totalOrders(totalOrdersCount)
                .totalCustomers(totalCustomersCount)
                .totalProducts(totalProductsCount)
                .monthlyRevenue(monthlyRevenue)
                .build();
    }
}
