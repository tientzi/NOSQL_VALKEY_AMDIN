package com.groceryshop.controller;

import com.groceryshop.dto.ReviewDTO;
import com.groceryshop.security.UserPrincipal;
import com.groceryshop.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @GetMapping("/api/public/products/{productId}/reviews")
    public ResponseEntity<List<ReviewDTO>> getApprovedReviews(@PathVariable Long productId) {
        return ResponseEntity.ok(reviewService.getApprovedReviewsByProduct(productId));
    }

    @PostMapping("/api/reviews")
    public ResponseEntity<ReviewDTO> createReview(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody ReviewDTO dto) {
        return ResponseEntity.ok(reviewService.createReview(userPrincipal.getId(), dto));
    }

    @GetMapping("/api/admin/reviews/pending")
    public ResponseEntity<List<ReviewDTO>> getPendingReviews() {
        return ResponseEntity.ok(reviewService.getPendingReviews());
    }

    @PutMapping("/api/admin/reviews/{id}/approve")
    public ResponseEntity<?> approveReview(@PathVariable Long id) {
        reviewService.approveReview(id);
        return ResponseEntity.ok().body("{\"message\": \"Duyệt đánh giá thành công\"}");
    }

    @DeleteMapping("/api/admin/reviews/{id}")
    public ResponseEntity<?> deleteReview(@PathVariable Long id) {
        reviewService.deleteReview(id);
        return ResponseEntity.ok().body("{\"message\": \"Xóa đánh giá thành công\"}");
    }
}
