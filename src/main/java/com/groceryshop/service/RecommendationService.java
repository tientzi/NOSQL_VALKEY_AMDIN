package com.groceryshop.service;

import com.groceryshop.dto.ProductDTO;
import com.groceryshop.dto.ProductRecommendationDTO;
import com.groceryshop.entity.OrderItem;
import com.groceryshop.entity.Product;
import com.groceryshop.exception.ResourceNotFoundException;
import com.groceryshop.mapper.EntityMapper;
import com.groceryshop.repository.OrderItemRepository;
import com.groceryshop.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ProductRepository productRepository;

    /**
     * Gợi ý sản phẩm dựa trên thuật toán Apriori (Confidence và Lift) từ lịch sử hóa đơn mua hàng
     *
     * @param targetProductId ID sản phẩm đang xem
     * @param limit           Số lượng sản phẩm cần gợi ý (mặc định 4)
     * @return Danh sách ProductRecommendationDTO kèm chỉ số Confidence, Lift và lý do gợi ý
     */
    public List<ProductRecommendationDTO> getRecommendations(Long targetProductId, int limit) {
        Product targetProduct = productRepository.findById(targetProductId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với id: " + targetProductId));

        // 1. Thu thập tất cả giao dịch (hóa đơn) hợp lệ không bị hủy
        List<OrderItem> allItems = orderItemRepository.findAll();
        Map<Long, Set<Long>> transactions = new HashMap<>();

        for (OrderItem item : allItems) {
            if (item.getOrder() == null || "HUY".equalsIgnoreCase(item.getOrder().getStatus())) {
                continue;
            }
            if (item.getProduct() == null) {
                continue;
            }
            Long orderId = item.getOrder().getId();
            Long prodId = item.getProduct().getId();
            transactions.computeIfAbsent(orderId, k -> new HashSet<>()).add(prodId);
        }

        int totalTransactions = transactions.size();
        List<ProductRecommendationDTO> recommendations = new ArrayList<>();
        Set<Long> addedProductIds = new HashSet<>();
        addedProductIds.add(targetProductId);

        if (totalTransactions > 0) {
            // Đếm tần số xuất hiện của target product A: freq(A)
            int freqA = 0;
            // Đếm tần số xuất hiện của tất cả các sản phẩm B: freq(B)
            Map<Long, Integer> freqMap = new HashMap<>();
            // Đếm số lần A và B cùng xuất hiện trong một đơn: freq(A ∩ B)
            Map<Long, Integer> coOccurrenceMap = new HashMap<>();

            for (Set<Long> basket : transactions.values()) {
                boolean containsTarget = basket.contains(targetProductId);
                if (containsTarget) {
                    freqA++;
                }

                for (Long prodId : basket) {
                    freqMap.put(prodId, freqMap.getOrDefault(prodId, 0) + 1);
                    if (containsTarget && !prodId.equals(targetProductId)) {
                        coOccurrenceMap.put(prodId, coOccurrenceMap.getOrDefault(prodId, 0) + 1);
                    }
                }
            }

            // 2. Tính toán Confidence và Lift theo thuật toán Apriori
            if (freqA > 0 && !coOccurrenceMap.isEmpty()) {
                class CandidateScore {
                    final Long productId;
                    final double confidence;
                    final double lift;
                    final int supportAB;

                    CandidateScore(Long productId, double confidence, double lift, int supportAB) {
                        this.productId = productId;
                        this.confidence = confidence;
                        this.lift = lift;
                        this.supportAB = supportAB;
                    }
                }

                List<CandidateScore> candidates = new ArrayList<>();
                for (Map.Entry<Long, Integer> entry : coOccurrenceMap.entrySet()) {
                    Long candidateId = entry.getKey();
                    int freqAB = entry.getValue();
                    int freqB = freqMap.getOrDefault(candidateId, 1);

                    // Confidence(A -> B) = freq(A ∩ B) / freq(A)
                    double confidence = (double) freqAB / freqA;

                    // Support(B) = freq(B) / N
                    // Lift(A -> B) = Confidence(A -> B) / Support(B) = (freqAB * N) / (freqA * freqB)
                    double lift = ((double) freqAB * totalTransactions) / ((double) freqA * freqB);

                    candidates.add(new CandidateScore(candidateId, confidence, lift, freqAB));
                }

                // Sắp xếp ưu tiên: Lift giảm dần -> Confidence giảm dần -> freqAB giảm dần
                candidates.sort((c1, c2) -> {
                    int liftCompare = Double.compare(c2.lift, c1.lift);
                    if (liftCompare != 0) return liftCompare;
                    int confCompare = Double.compare(c2.confidence, c1.confidence);
                    if (confCompare != 0) return confCompare;
                    return Integer.compare(c2.supportAB, c1.supportAB);
                });

                // Lấy các sản phẩm có Lift >= 1.0 hoặc Confidence cao
                for (CandidateScore score : candidates) {
                    if (recommendations.size() >= limit) break;

                    productRepository.findById(score.productId).ifPresent(p -> {
                        if (Boolean.TRUE.equals(p.getIsActive())) {
                            ProductDTO pDto = EntityMapper.toProductDTO(p);
                            recommendations.add(ProductRecommendationDTO.builder()
                                    .product(pDto)
                                    .confidence(Math.round(score.confidence * 100.0) / 100.0)
                                    .lift(Math.round(score.lift * 100.0) / 100.0)
                                    .reason("Thường mua cùng")
                                    .build());
                            addedProductIds.add(p.getId());
                        }
                    });
                }
            }
        }

        // 3. Fallback Cold-Start: Nếu chưa đủ số lượng limit, lấy thêm sản phẩm cùng danh mục
        if (recommendations.size() < limit && targetProduct.getCategory() != null) {
            Long categoryId = targetProduct.getCategory().getId();
            List<Product> sameCategoryProducts = productRepository.findAll().stream()
                    .filter(p -> Boolean.TRUE.equals(p.getIsActive()))
                    .filter(p -> p.getCategory() != null && p.getCategory().getId().equals(categoryId))
                    .filter(p -> !addedProductIds.contains(p.getId()))
                    .sorted((p1, p2) -> Long.compare(p2.getId(), p1.getId()))
                    .collect(Collectors.toList());

            for (Product p : sameCategoryProducts) {
                if (recommendations.size() >= limit) break;
                recommendations.add(ProductRecommendationDTO.builder()
                        .product(EntityMapper.toProductDTO(p))
                        .confidence(null)
                        .lift(null)
                        .reason("Cùng danh mục")
                        .build());
                addedProductIds.add(p.getId());
            }
        }

        return recommendations;
    }
}
