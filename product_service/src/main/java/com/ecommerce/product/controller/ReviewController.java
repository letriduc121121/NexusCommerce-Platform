package com.ecommerce.product.controller;

import com.ecommerce.product.dto.CreateReviewRequest;
import com.ecommerce.product.dto.ReviewResponse;
import com.ecommerce.product.dto.UpdateReviewRequest;
import com.ecommerce.product.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    // ==================== KHÁCH HÀNG ====================

    @PostMapping("/reviews")
    public ResponseEntity<ReviewResponse> createReview(
            Authentication auth,
            @Valid @RequestBody CreateReviewRequest request) {
        ReviewResponse response = reviewService.createReview(userId(auth), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/reviews/my-reviewed-order-item-ids")
    public ResponseEntity<java.util.List<UUID>> getMyReviewedOrderItemIds(Authentication auth) {
        return ResponseEntity.ok(reviewService.getMyReviewedOrderItemIds(userId(auth)));
    }

    @PutMapping("/reviews/{reviewId}")
    public ResponseEntity<ReviewResponse> updateReview(
            Authentication auth,
            @PathVariable UUID reviewId,
            @Valid @RequestBody UpdateReviewRequest request) {
        ReviewResponse response = reviewService.updateReview(userId(auth), reviewId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/products/{productId}/reviews")
    public ResponseEntity<Page<ReviewResponse>> getProductReviews(
            @PathVariable UUID productId,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) Boolean withImages,
            @RequestParam(required = false) Boolean withComment,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<ReviewResponse> reviews = reviewService.getReviewsByProduct(productId, rating, withImages, withComment, page, size);
        return ResponseEntity.ok(reviews);
    }

    // ==================== ADMIN ====================

    @PutMapping("/admin/reviews/{reviewId}/toggle-hide")
    public ResponseEntity<ReviewResponse> toggleHideReview(@PathVariable UUID reviewId) {
        return ResponseEntity.ok(reviewService.toggleHideReview(reviewId));
    }

    @PostMapping("/admin/reviews/{reviewId}/reply")
    public ResponseEntity<ReviewResponse> replyReview(
            @PathVariable UUID reviewId,
            @RequestBody String shopReply) {
        return ResponseEntity.ok(reviewService.replyReview(reviewId, shopReply));
    }

    // ==================== HỖ TRỢ ====================

    private UUID userId(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            throw new IllegalArgumentException("Bạn cần đăng nhập để thực hiện thao tác này!");
        }
        return UUID.fromString(auth.getPrincipal().toString());
    }
}
