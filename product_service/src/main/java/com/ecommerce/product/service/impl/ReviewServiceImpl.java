package com.ecommerce.product.service.impl;

import com.ecommerce.product.client.OrderClient;
import com.ecommerce.product.dto.CreateReviewRequest;
import com.ecommerce.product.dto.ReviewResponse;
import com.ecommerce.product.dto.UpdateReviewRequest;
import com.ecommerce.product.dto.VerifyPurchaseResponse;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.entity.Review;
import com.ecommerce.product.entity.ReviewImage;
import com.ecommerce.product.event.ReviewRewardGrantedEvent;
import com.ecommerce.product.repository.ProductRepository;
import com.ecommerce.product.repository.ReviewRepository;
import com.ecommerce.product.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final OrderClient orderClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    @Transactional
    public ReviewResponse createReview(UUID userId, CreateReviewRequest request) {
        log.info("⭐ [REVIEW SERVICE] Tạo đánh giá mới: userId={}, orderItemId={}", userId, request.getOrderItemId());

        // 1. Kiểm tra xem món hàng này đã được đánh giá chưa
        if (reviewRepository.existsByOrderItemId(request.getOrderItemId())) {
            throw new IllegalStateException("Sản phẩm này trong đơn hàng của bạn đã được đánh giá rồi!");
        }

        // 2. Xác minh điều kiện mua hàng từ Order Service
        VerifyPurchaseResponse verify = orderClient.verifyPurchase(request.getOrderItemId(), userId);
        if (verify == null || !verify.isEligible()) {
            String msg = verify != null ? verify.getMessage() : "Không thể xác minh đơn hàng!";
            throw new IllegalStateException("Không đủ điều kiện đánh giá: " + msg);
        }

        UUID productId = UUID.fromString(verify.getProductId());
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm id: " + productId));

        // 3. Tính toán thưởng Xu (>= 50 ký tự = 100 Xu, +ảnh = 200 Xu)
        int charCount = request.getComment() != null ? request.getComment().trim().length() : 0;
        boolean hasPhotos = request.getPhotoUrls() != null && !request.getPhotoUrls().isEmpty();
        int coins = 0;

        if (charCount >= 50 && hasPhotos) {
            coins = 200;
        } else if (charCount >= 50) {
            coins = 100;
        }

        // 4. Lưu Review & ReviewImage
        Review review = Review.builder()
                .orderItemId(request.getOrderItemId())
                .productId(productId)
                .userId(userId)
                .userFullName("Khách hàng")
                .rating(request.getRating())
                .comment(request.getComment())
                .variantInfo(request.getVariantInfo() != null ? request.getVariantInfo() : verify.getVariantInfo())
                .coinsAwarded(coins)
                .isHidden(false)
                .isEdited(false)
                .build();

        if (hasPhotos) {
            for (String photoUrl : request.getPhotoUrls()) {
                if (photoUrl != null && !photoUrl.isBlank()) {
                    review.addImage(ReviewImage.builder().imageUrl(photoUrl.trim()).build());
                }
            }
        }

        Review savedReview = reviewRepository.save(review);

        // 5. Tính toán lại avgRating & totalReviews cho Product
        updateProductStats(productId);

        // 6. Phát sự kiện Kafka tặng Xu nếu coins > 0
        if (coins > 0) {
            try {
                ReviewRewardGrantedEvent event = ReviewRewardGrantedEvent.builder()
                        .reviewId(savedReview.getId().toString())
                        .userId(userId.toString())
                        .coins(coins)
                        .createdAt(LocalDateTime.now())
                        .build();
                kafkaTemplate.send("reward-topic", userId.toString(), event);
                log.info("🎁 Đã phát sự kiện REVIEW_REWARD_GRANTED qua Kafka: userId={}, coins={}", userId, coins);
            } catch (Exception e) {
                log.error("❌ Lỗi phát sự kiện thưởng Xu: {}", e.getMessage());
            }
        }

        return mapToResponse(savedReview);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> getMyReviewedOrderItemIds(UUID userId) {
        return reviewRepository.findReviewedOrderItemIdsByUserId(userId);
    }

    @Override
    @Transactional
    public ReviewResponse updateReview(UUID userId, UUID reviewId, UpdateReviewRequest request) {
        log.info("📝 [REVIEW SERVICE] Chỉnh sửa đánh giá reviewId={}, userId={}", reviewId, userId);
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đánh giá id: " + reviewId));

        if (!review.getUserId().equals(userId)) {
            throw new IllegalStateException("Bạn không có quyền chỉnh sửa đánh giá này!");
        }

        if (Boolean.TRUE.equals(review.getIsEdited())) {
            throw new IllegalStateException("Mỗi bài đánh giá chỉ được phép chỉnh sửa 1 lần duy nhất!");
        }

        if (review.getCreatedAt() != null && review.getCreatedAt().isBefore(LocalDateTime.now().minusDays(7))) {
            throw new IllegalStateException("Đã quá thời hạn 7 ngày kể từ khi đánh giá, không thể chỉnh sửa!");
        }

        if (request.getRating() != null) review.setRating(request.getRating());
        if (request.getComment() != null) review.setComment(request.getComment());
        review.setIsEdited(true);

        if (request.getPhotoUrls() != null) {
            review.getImages().clear();
            for (String url : request.getPhotoUrls()) {
                if (url != null && !url.isBlank()) {
                    review.addImage(ReviewImage.builder().imageUrl(url.trim()).build());
                }
            }
        }

        Review updated = reviewRepository.save(review);
        updateProductStats(review.getProductId());

        return mapToResponse(updated);
    }

    @Override
    public Page<ReviewResponse> getReviewsByProduct(UUID productId, Integer rating, Boolean withImages, Boolean withComment, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Review> reviewPage;

        if (rating != null && rating >= 1 && rating <= 5) {
            reviewPage = reviewRepository.findByProductIdAndRatingAndIsHiddenFalse(productId, rating, pageable);
        } else if (Boolean.TRUE.equals(withImages)) {
            reviewPage = reviewRepository.findByProductIdWithImagesAndIsHiddenFalse(productId, pageable);
        } else if (Boolean.TRUE.equals(withComment)) {
            reviewPage = reviewRepository.findByProductIdWithCommentAndIsHiddenFalse(productId, pageable);
        } else {
            reviewPage = reviewRepository.findByProductIdAndIsHiddenFalse(productId, pageable);
        }

        return reviewPage.map(this::mapToResponse);
    }

    @Override
    @Transactional
    public ReviewResponse toggleHideReview(UUID reviewId) {
        log.info("👁️ [REVIEW SERVICE - ADMIN] Ẩn/Hiện bài đánh giá reviewId={}", reviewId);
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đánh giá id: " + reviewId));

        review.setIsHidden(!Boolean.TRUE.equals(review.getIsHidden()));
        Review updated = reviewRepository.save(review);

        updateProductStats(review.getProductId());
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public ReviewResponse replyReview(UUID reviewId, String shopReply) {
        log.info("💬 [REVIEW SERVICE - ADMIN] Trả lời đánh giá reviewId={}", reviewId);
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đánh giá id: " + reviewId));

        review.setShopReply(shopReply);
        review.setShopRepliedAt(LocalDateTime.now());

        Review updated = reviewRepository.save(review);
        return mapToResponse(updated);
    }

    private void updateProductStats(UUID productId) {
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) return;

        Double avg = reviewRepository.calculateAvgRatingByProductId(productId);
        long count = reviewRepository.countValidReviewsByProductId(productId);

        double roundedAvg = avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0;
        product.setAvgRating(roundedAvg);
        product.setTotalReviews((int) count);

        productRepository.save(product);

        // Xóa cache Redis của sản phẩm
        try {
            redisTemplate.delete("product:" + productId);
            redisTemplate.delete("product_detail:" + productId);
            log.info("🧹 Đã làm mới Redis Cache cho sản phẩm: {}", productId);
        } catch (Exception e) {
            log.warn("⚠️ Lỗi xóa Redis Cache sản phẩm: {}", e.getMessage());
        }
    }

    private ReviewResponse mapToResponse(Review review) {
        List<String> imageUrls = review.getImages() != null
                ? review.getImages().stream().map(ReviewImage::getImageUrl).toList()
                : List.of();

        return ReviewResponse.builder()
                .id(review.getId())
                .orderItemId(review.getOrderItemId())
                .productId(review.getProductId())
                .userId(review.getUserId())
                .userFullName(maskName(review.getUserFullName()))
                .rating(review.getRating())
                .comment(review.getComment())
                .variantInfo(review.getVariantInfo())
                .coinsAwarded(review.getCoinsAwarded())
                .isHidden(review.getIsHidden())
                .isEdited(review.getIsEdited())
                .shopReply(review.getShopReply())
                .shopRepliedAt(review.getShopRepliedAt())
                .images(imageUrls)
                .createdAt(review.getCreatedAt())
                .build();
    }

    private String maskName(String name) {
        if (name == null || name.isBlank()) return "nguoidung***";
        if (name.length() <= 3) return name.charAt(0) + "***";
        return name.substring(0, 3) + "***";
    }
}
