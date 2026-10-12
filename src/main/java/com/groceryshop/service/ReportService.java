package com.groceryshop.service;

import com.groceryshop.dto.ReportOverviewDTO;
import com.groceryshop.entity.Order;
import com.groceryshop.entity.OrderItem;
import com.groceryshop.entity.User;
import com.groceryshop.repository.OrderItemRepository;
import com.groceryshop.repository.OrderRepository;
import com.groceryshop.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ReportService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private UserRepository userRepository;

    public ReportOverviewDTO getOverviewReport() {
        return getOverviewReport("30days", null, null);
    }

    public ReportOverviewDTO getOverviewReport(String range, java.time.LocalDate startDate, java.time.LocalDate endDate) {
        List<Order> allOrders = orderRepository.findAll();

        // 1. Xác định khung thời gian lọc
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from = null;
        LocalDateTime to = now;

        if (range == null || range.isBlank()) {
            range = "30days";
        }

        switch (range) {
            case "today":
                from = java.time.LocalDate.now().atStartOfDay();
                to = java.time.LocalDate.now().atTime(23, 59, 59);
                break;
            case "7days":
                from = now.minusDays(6).withHour(0).withMinute(0).withSecond(0);
                to = java.time.LocalDate.now().atTime(23, 59, 59);
                break;
            case "30days":
                from = now.minusDays(29).withHour(0).withMinute(0).withSecond(0);
                to = java.time.LocalDate.now().atTime(23, 59, 59);
                break;
            case "thisMonth":
                from = YearMonth.now().atDay(1).atStartOfDay();
                to = YearMonth.now().atEndOfMonth().atTime(23, 59, 59);
                break;
            case "thisYear":
                from = java.time.LocalDate.of(now.getYear(), 1, 1).atStartOfDay();
                to = java.time.LocalDate.of(now.getYear(), 12, 31).atTime(23, 59, 59);
                break;
            case "custom":
                if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
                    java.time.LocalDate temp = startDate;
                    startDate = endDate;
                    endDate = temp;
                }
                if (startDate != null) from = startDate.atStartOfDay();
                if (endDate != null) to = endDate.atTime(23, 59, 59);
                break;
            case "all":
            default:
                from = null;
                to = null;
                break;
        }

        final LocalDateTime filterFrom = from;
        final LocalDateTime filterTo = to;

        List<Order> filteredOrders = allOrders.stream()
                .filter(o -> {
                    if (o.getCreatedAt() == null) return false;
                    if (filterFrom != null && o.getCreatedAt().isBefore(filterFrom)) return false;
                    if (filterTo != null && o.getCreatedAt().isAfter(filterTo)) return false;
                    return true;
                })
                .collect(Collectors.toList());

        // 2. Tổng quan KPI cơ bản trong kỳ
        long totalOrders = filteredOrders.size();

        // Tối ưu truy vấn người dùng: chỉ gọi userRepository.findAll() 1 lần
        List<User> allUsers = userRepository.findAll();
        long totalCustomers = allUsers.stream()
                .filter(u -> u.getRole() != null && "ROLE_USER".equals(u.getRole().getName()))
                .count();

        BigDecimal totalRevenue = filteredOrders.stream()
                .filter(o -> "HOAN_THANH".equals(o.getStatus()) || "DA_GIAO".equals(o.getStatus()))
                .map(Order::getFinalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalProductsSold = filteredOrders.stream()
                .filter(o -> "HOAN_THANH".equals(o.getStatus()) || "DA_GIAO".equals(o.getStatus()))
                .flatMap(o -> o.getItems() != null ? o.getItems().stream() : java.util.stream.Stream.empty())
                .filter(item -> item.getQuantity() != null)
                .mapToLong(OrderItem::getQuantity)
                .sum();

        // 3. Biểu đồ doanh thu (Revenue Trend) hoàn toàn động theo bộ lọc
        List<Map<String, Object>> revenueTrend = new ArrayList<>();
        DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("dd/MM");

        if ("today".equals(range)) {
            // Theo khung giờ trong ngày (4 tiếng một mốc: 00:00, 04:00, 08:00, 12:00, 16:00, 20:00)
            for (int h = 0; h < 24; h += 4) {
                String label = String.format("%02d:00", h);
                int startH = h;
                int endH = h + 4;
                List<Order> hOrders = filteredOrders.stream()
                        .filter(o -> o.getCreatedAt() != null && o.getCreatedAt().getHour() >= startH && o.getCreatedAt().getHour() < endH)
                        .collect(Collectors.toList());
                BigDecimal rev = hOrders.stream()
                        .filter(o -> "HOAN_THANH".equals(o.getStatus()) || "DA_GIAO".equals(o.getStatus()))
                        .map(Order::getFinalAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
                revenueTrend.add(Map.of("month", label, "revenue", rev, "orderCount", hOrders.size()));
            }
        } else if ("7days".equals(range)) {
            for (int i = 6; i >= 0; i--) {
                java.time.LocalDate d = java.time.LocalDate.now().minusDays(i);
                String label = d.format(dayFmt);
                List<Order> dOrders = filteredOrders.stream()
                        .filter(o -> o.getCreatedAt() != null && o.getCreatedAt().toLocalDate().equals(d))
                        .collect(Collectors.toList());
                BigDecimal rev = dOrders.stream()
                        .filter(o -> "HOAN_THANH".equals(o.getStatus()) || "DA_GIAO".equals(o.getStatus()))
                        .map(Order::getFinalAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
                revenueTrend.add(Map.of("month", label, "revenue", rev, "orderCount", dOrders.size()));
            }
        } else if ("30days".equals(range)) {
            for (int i = 27; i >= 0; i -= 3) {
                java.time.LocalDate d = java.time.LocalDate.now().minusDays(i);
                java.time.LocalDate endD = d.plusDays(2);
                String label = d.format(dayFmt);
                List<Order> intervalOrders = filteredOrders.stream()
                        .filter(o -> o.getCreatedAt() != null && !o.getCreatedAt().toLocalDate().isBefore(d) && !o.getCreatedAt().toLocalDate().isAfter(endD))
                        .collect(Collectors.toList());
                BigDecimal rev = intervalOrders.stream()
                        .filter(o -> "HOAN_THANH".equals(o.getStatus()) || "DA_GIAO".equals(o.getStatus()))
                        .map(Order::getFinalAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
                revenueTrend.add(Map.of("month", label, "revenue", rev, "orderCount", intervalOrders.size()));
            }
        } else if ("thisMonth".equals(range)) {
            // Từng ngày từ ngày 1 đến ngày hiện tại
            java.time.LocalDate today = java.time.LocalDate.now();
            java.time.LocalDate firstDay = today.withDayOfMonth(1);
            int currentDay = today.getDayOfMonth();
            for (int d = 1; d <= currentDay; d++) {
                java.time.LocalDate date = firstDay.withDayOfMonth(d);
                String label = date.format(dayFmt);
                List<Order> dOrders = filteredOrders.stream()
                        .filter(o -> o.getCreatedAt() != null && o.getCreatedAt().toLocalDate().equals(date))
                        .collect(Collectors.toList());
                BigDecimal rev = dOrders.stream()
                        .filter(o -> "HOAN_THANH".equals(o.getStatus()) || "DA_GIAO".equals(o.getStatus()))
                        .map(Order::getFinalAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
                revenueTrend.add(Map.of("month", label, "revenue", rev, "orderCount", dOrders.size()));
            }
        } else if ("thisYear".equals(range)) {
            // Hiển thị đủ 12 tháng của năm hiện tại
            int currentYear = java.time.LocalDate.now().getYear();
            for (int m = 1; m <= 12; m++) {
                YearMonth ym = YearMonth.of(currentYear, m);
                String label = "T" + String.format("%02d", m);
                List<Order> mOrders = filteredOrders.stream()
                        .filter(o -> o.getCreatedAt() != null && YearMonth.from(o.getCreatedAt()).equals(ym))
                        .collect(Collectors.toList());
                BigDecimal rev = mOrders.stream()
                        .filter(o -> "HOAN_THANH".equals(o.getStatus()) || "DA_GIAO".equals(o.getStatus()))
                        .map(Order::getFinalAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
                revenueTrend.add(Map.of("month", label, "revenue", rev, "orderCount", mOrders.size()));
            }
        } else if ("custom".equals(range) && startDate != null && endDate != null) {
            long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
            if (daysBetween <= 31) {
                // Hiển thị theo từng ngày
                for (java.time.LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
                    final java.time.LocalDate targetDate = d;
                    String label = targetDate.format(dayFmt);
                    List<Order> dOrders = filteredOrders.stream()
                            .filter(o -> o.getCreatedAt() != null && o.getCreatedAt().toLocalDate().equals(targetDate))
                            .collect(Collectors.toList());
                    BigDecimal rev = dOrders.stream()
                            .filter(o -> "HOAN_THANH".equals(o.getStatus()) || "DA_GIAO".equals(o.getStatus()))
                            .map(Order::getFinalAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
                    revenueTrend.add(Map.of("month", label, "revenue", rev, "orderCount", dOrders.size()));
                }
            } else {
                // Hiển thị theo từng tháng trong khoảng custom
                YearMonth startYm = YearMonth.from(startDate);
                YearMonth endYm = YearMonth.from(endDate);
                for (YearMonth ym = startYm; !ym.isAfter(endYm); ym = ym.plusMonths(1)) {
                    final YearMonth targetYm = ym;
                    String label = "T" + targetYm.getMonthValue() + "/" + targetYm.getYear();
                    List<Order> mOrders = filteredOrders.stream()
                            .filter(o -> o.getCreatedAt() != null && YearMonth.from(o.getCreatedAt()).equals(targetYm))
                            .collect(Collectors.toList());
                    BigDecimal rev = mOrders.stream()
                            .filter(o -> "HOAN_THANH".equals(o.getStatus()) || "DA_GIAO".equals(o.getStatus()))
                            .map(Order::getFinalAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
                    revenueTrend.add(Map.of("month", label, "revenue", rev, "orderCount", mOrders.size()));
                }
            }
        } else {
            // Cho "all" hoặc custom thiếu ngày: Lấy các tháng từ đơn hàng sớm nhất đến tháng hiện tại
            YearMonth nowYm = YearMonth.now();
            YearMonth earliestYm = filteredOrders.stream()
                    .map(o -> o.getCreatedAt() != null ? YearMonth.from(o.getCreatedAt()) : null)
                    .filter(Objects::nonNull)
                    .min(YearMonth::compareTo)
                    .orElse(nowYm.minusMonths(5));

            if (earliestYm.isBefore(nowYm.minusMonths(11))) {
                earliestYm = nowYm.minusMonths(11);
            }

            for (YearMonth ym = earliestYm; !ym.isAfter(nowYm); ym = ym.plusMonths(1)) {
                final YearMonth currentYm = ym;
                String label = "T" + currentYm.getMonthValue() + "/" + currentYm.getYear();
                List<Order> monthOrders = filteredOrders.stream()
                        .filter(o -> o.getCreatedAt() != null && YearMonth.from(o.getCreatedAt()).equals(currentYm))
                        .collect(Collectors.toList());

                BigDecimal revenue = monthOrders.stream()
                        .filter(o -> "HOAN_THANH".equals(o.getStatus()) || "DA_GIAO".equals(o.getStatus()))
                        .map(Order::getFinalAmount)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                Map<String, Object> map = new HashMap<>();
                map.put("month", label);
                map.put("revenue", revenue);
                map.put("orderCount", monthOrders.size());
                revenueTrend.add(map);
            }
        }

        // 4. Cơ cấu trạng thái đơn hàng trong kỳ (Null-safe)
        Map<String, Long> statusCounts = filteredOrders.stream()
                .collect(Collectors.groupingBy(o -> o.getStatus() != null ? o.getStatus() : "CHO_XAC_NHAN", Collectors.counting()));

        List<Map<String, Object>> statusDist = new ArrayList<>();
        statusDist.add(Map.of("name", "Hoàn thành", "count", statusCounts.getOrDefault("HOAN_THANH", 0L) + statusCounts.getOrDefault("DA_GIAO", 0L), "color", "#10b981"));
        statusDist.add(Map.of("name", "Đang giao hàng", "count", statusCounts.getOrDefault("DANG_GIAO", 0L) + statusCounts.getOrDefault("DA_NHAN_DON", 0L), "color", "#f59e0b"));
        statusDist.add(Map.of("name", "Chờ xác nhận", "count", statusCounts.getOrDefault("CHO_XAC_NHAN", 0L) + statusCounts.getOrDefault("DA_XAC_NHAN", 0L), "color", "#3b82f6"));
        statusDist.add(Map.of("name", "Đã hủy đơn", "count", statusCounts.getOrDefault("HUY", 0L), "color", "#64748b"));
        statusDist.add(Map.of("name", "Giao thất bại", "count", statusCounts.getOrDefault("GIAO_THAT_BAI", 0L), "color", "#ef4444"));

        // 5. Tỷ trọng phương thức thanh toán trong kỳ
        Map<String, Long> payCounts = filteredOrders.stream()
                .collect(Collectors.groupingBy(o -> o.getPaymentMethod() != null ? o.getPaymentMethod() : "COD", Collectors.counting()));

        List<Map<String, Object>> payDist = new ArrayList<>();
        payDist.add(Map.of("name", "Tiền mặt COD", "value", payCounts.getOrDefault("COD", 0L), "color", "#f97316"));
        payDist.add(Map.of("name", "Chuyển khoản QR", "value", payCounts.getOrDefault("BANK_TRANSFER", 0L), "color", "#0284c7"));
        payDist.add(Map.of("name", "Ví MoMo", "value", payCounts.getOrDefault("MOMO", 0L), "color", "#d946ef"));

        // 6. Top 10 sản phẩm bán chạy nhất trong kỳ (Null-safe quantity and price)
        Map<String, Long> productQuantities = new HashMap<>();
        Map<String, BigDecimal> productRevenues = new HashMap<>();

        for (Order o : filteredOrders) {
            if ("HOAN_THANH".equals(o.getStatus()) || "DA_GIAO".equals(o.getStatus())) {
                if (o.getItems() != null) {
                    for (OrderItem item : o.getItems()) {
                        String name = item.getProductName();
                        if (name != null) {
                            int qty = item.getQuantity() != null ? item.getQuantity() : 0;
                            productQuantities.put(name, productQuantities.getOrDefault(name, 0L) + qty);
                            BigDecimal price = item.getPrice() != null ? item.getPrice() : BigDecimal.ZERO;
                            BigDecimal itemTotal = price.multiply(BigDecimal.valueOf(qty));
                            productRevenues.put(name, productRevenues.getOrDefault(name, BigDecimal.ZERO).add(itemTotal));
                        }
                    }
                }
            }
        }

        List<Map<String, Object>> topProducts = productQuantities.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                .limit(10)
                .map(e -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("productName", e.getKey());
                    map.put("quantity", e.getValue());
                    map.put("revenue", productRevenues.getOrDefault(e.getKey(), BigDecimal.ZERO));
                    return map;
                })
                .collect(Collectors.toList());

        // 7. Hiệu suất của đội ngũ Shipper (3 quận TP.HCM)
        List<User> shippers = allUsers.stream()
                .filter(u -> u.getRole() != null && "ROLE_SHIPPER".equals(u.getRole().getName()))
                .collect(Collectors.toList());

        List<Map<String, Object>> shipperPerf = new ArrayList<>();
        for (User s : shippers) {
            List<Order> sOrders = filteredOrders.stream()
                    .filter(o -> o.getShipper() != null && o.getShipper().getId().equals(s.getId()))
                    .collect(Collectors.toList());

            long assigned = sOrders.size();
            long delivered = sOrders.stream().filter(o -> "HOAN_THANH".equals(o.getStatus()) || "DA_GIAO".equals(o.getStatus())).count();
            long failed = sOrders.stream().filter(o -> "GIAO_THAT_BAI".equals(o.getStatus())).count();
            BigDecimal codCollected = sOrders.stream()
                    .filter(o -> ("HOAN_THANH".equals(o.getStatus()) || "DA_GIAO".equals(o.getStatus())) && "COD".equals(o.getPaymentMethod()))
                    .map(Order::getFinalAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Xác định quận phụ trách dựa trên username (shipper1, shipper2, shipper3) hoặc fullName
            String district = "Chưa phân khu";
            String username = s.getUsername() != null ? s.getUsername().toLowerCase() : "";
            String fullName = s.getFullName() != null ? s.getFullName().toLowerCase() : "";

            if (username.contains("shipper1") || fullName.contains("tân phú") || fullName.contains("tan phu")) {
                district = "Quận Tân Phú";
            } else if (username.contains("shipper2") || fullName.contains("tân bình") || fullName.contains("tan binh")) {
                district = "Quận Tân Bình";
            } else if (username.contains("shipper3") || fullName.contains("quận 12") || fullName.contains("quan 12") || fullName.contains("q.12") || fullName.contains("q12")) {
                district = "Quận 12";
            }

            double successRate = 0.0;
            if ((delivered + failed) > 0) {
                successRate = ((double) delivered / (delivered + failed)) * 100.0;
            } else if (assigned == 0) {
                successRate = 100.0;
            } else {
                successRate = 0.0;
            }

            Map<String, Object> map = new HashMap<>();
            map.put("shipperName", s.getFullName() != null ? s.getFullName() : s.getUsername());
            map.put("district", district);
            map.put("phone", s.getPhone());
            map.put("assignedCount", assigned);
            shipperPerf.add(map);
        }

        return ReportOverviewDTO.builder()
                .totalRevenue(totalRevenue)
                .totalOrders(totalOrders)
                .totalCustomers(totalCustomers)
                .totalProductsSold(totalProductsSold)
                .monthlyRevenues(revenueTrend)
                .orderStatusDistribution(statusDist)
                .paymentMethodDistribution(payDist)
                .topSellingProducts(topProducts)
                .shipperPerformance(shipperPerf)
                .build();
    }

    @Autowired
    private com.groceryshop.repository.ProductBatchRepository batchRepository;

    public Map<String, Object> getExpiredBatchesReport() {
        java.time.LocalDate today = java.time.LocalDate.now();
        List<com.groceryshop.entity.ProductBatch> allBatches = batchRepository.findAll();

        List<com.groceryshop.dto.ProductBatchDTO> expiredList = new ArrayList<>();
        int totalExpiredQty = 0;
        BigDecimal totalLoss = BigDecimal.ZERO;

        for (com.groceryshop.entity.ProductBatch b : allBatches) {
            boolean isExp = b.getExpiryDate() != null && b.getExpiryDate().isBefore(today);
            boolean isDisposed = "DISPOSED".equalsIgnoreCase(b.getStatus());
            if (isExp || isDisposed) {
                com.groceryshop.dto.ProductBatchDTO dto = com.groceryshop.mapper.EntityMapper.toProductBatchDTO(b);
                expiredList.add(dto);

                int qty = b.getQuantity() != null ? b.getQuantity() : 0;
                totalExpiredQty += qty;
                BigDecimal price = b.getEffectiveImportPrice();
                if (price != null && qty > 0) {
                    totalLoss = totalLoss.add(price.multiply(BigDecimal.valueOf(qty)));
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalExpiredBatches", expiredList.size());
        result.put("totalExpiredQuantity", totalExpiredQty);
        result.put("totalLossAmount", totalLoss);
        result.put("batches", expiredList);
        return result;
    }
}
