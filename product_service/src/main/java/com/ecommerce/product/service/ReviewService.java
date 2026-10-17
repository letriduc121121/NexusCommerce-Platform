package com.ecommerce.product.service;

import com.ecommerce.product.dto.CreateReviewRequest;
import com.ecommerce.product.dto.ReviewResponse;
import com.ecommerce.product.dto.UpdateReviewRequest;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface ReviewService {

    ReviewResponse createReview(UUID userId, CreateReviewRequest request);

    java.util.List<UUID> getMyReviewedOrderItemIds(UUID userId);

    ReviewResponse updateReview(UUID userId, UUID reviewId, UpdateReviewRequest request);

    Page<ReviewResponse> getReviewsByProduct(UUID productId, Integer rating, Boolean withImages, Boolean withComment, int page, int size);

    ReviewResponse toggleHideReview(UUID reviewId);

    ReviewResponse replyReview(UUID reviewId, String shopReply);
}
