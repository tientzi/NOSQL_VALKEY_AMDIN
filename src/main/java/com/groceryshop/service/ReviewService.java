package com.groceryshop.service;

import com.groceryshop.dto.ReviewDTO;
import com.groceryshop.entity.Product;
import com.groceryshop.entity.Review;
import com.groceryshop.entity.User;
import com.groceryshop.exception.ResourceNotFoundException;
import com.groceryshop.mapper.EntityMapper;
import com.groceryshop.repository.ProductRepository;
import com.groceryshop.repository.ReviewRepository;
import com.groceryshop.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    public List<ReviewDTO> getApprovedReviewsByProduct(Long productId) {
        return reviewRepository.findByProductIdAndIsApprovedTrueOrderByCreatedAtDesc(productId).stream()
                .map(EntityMapper::toReviewDTO)
                .collect(Collectors.toList());
    }

    public List<ReviewDTO> getPendingReviews() {
        return reviewRepository.findByIsApprovedFalseOrderByCreatedAtDesc().stream()
                .map(EntityMapper::toReviewDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public ReviewDTO createReview(Long userId, ReviewDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với id: " + userId));

        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với id: " + dto.getProductId()));

        Review review = Review.builder()
                .user(user)
                .product(product)
                .rating(dto.getRating())
                .comment(dto.getComment())
                .isApproved(false) // Đăng ký review mặc định chờ duyệt
                .build();

        Review saved = reviewRepository.save(review);
        return EntityMapper.toReviewDTO(saved);
    }

    @Transactional
    public void approveReview(Long id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy review với id: " + id));
        review.setIsApproved(true);
        reviewRepository.save(review);
    }

    @Transactional
    public void deleteReview(Long id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy review với id: " + id));
        reviewRepository.delete(review);
    }
}
