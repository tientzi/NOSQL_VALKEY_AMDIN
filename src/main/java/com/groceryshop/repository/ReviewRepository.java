package com.groceryshop.repository;

import com.groceryshop.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByProductIdAndIsApprovedTrueOrderByCreatedAtDesc(Long productId);
    List<Review> findByIsApprovedFalseOrderByCreatedAtDesc();
}
