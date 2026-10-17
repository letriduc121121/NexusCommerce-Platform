package com.ecommerce.product.kafka;

import com.ecommerce.product.entity.Product;
import com.ecommerce.product.entity.Review;
import com.ecommerce.product.event.OrderEventPayload;
import com.ecommerce.product.repository.ProductRepository;
import com.ecommerce.product.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    @KafkaListener(topics = "order-delivered-topic", groupId = "product-service-group")
    public void handleOrderDelivered(OrderEventPayload payload) {
        log.info("🚚 [PRODUCT KAFKA CONSUMER] Nhận sự kiện ORDER_DELIVERED cho đơn: {}", payload.getOrderId());
        if (payload.getItems() == null || payload.getItems().isEmpty()) return;

        for (OrderEventPayload.OrderItemPayload item : payload.getItems()) {
            try {
                UUID productId = UUID.fromString(item.getProductId());
                int quantity = item.getQuantity() != null ? item.getQuantity() : 1;

                Product product = productRepository.findById(productId).orElse(null);
                if (product != null) {
                    int currentSold = product.getTotalSold() != null ? product.getTotalSold() : 0;
                    product.setTotalSold(currentSold + quantity);
                    productRepository.save(product);

                    clearProductCache(productId);
                    log.info("✅ Đã cộng {} lượt bán cho sản phẩm {}", quantity, productId);
                }
            } catch (Exception e) {
                log.error("❌ Lỗi xử lý cộng total_sold cho item: {}", e.getMessage());
            }
        }
    }

    @KafkaListener(topics = "order-refunded-topic", groupId = "product-service-group")
    public void handleOrderRefunded(OrderEventPayload payload) {
        log.info("🔄 [PRODUCT KAFKA CONSUMER] Nhận sự kiện ORDER_REFUNDED cho đơn: {}", payload.getOrderId());
        if (payload.getItems() == null || payload.getItems().isEmpty()) return;

        for (OrderEventPayload.OrderItemPayload item : payload.getItems()) {
            try {
                UUID productId = UUID.fromString(item.getProductId());
                int quantity = item.getQuantity() != null ? item.getQuantity() : 1;

                Product product = productRepository.findById(productId).orElse(null);
                if (product != null) {
                    int currentSold = product.getTotalSold() != null ? product.getTotalSold() : 0;
                    product.setTotalSold(Math.max(0, currentSold - quantity));
                    productRepository.save(product);

                    // Ẩn bài đánh giá liên quan đến order_item_id nếu có
                    if (item.getOrderItemId() != null && !item.getOrderItemId().isBlank()) {
                        UUID orderItemId = UUID.fromString(item.getOrderItemId());
                        Optional<Review> reviewOpt = reviewRepository.findByOrderItemId(orderItemId);
                        if (reviewOpt.isPresent()) {
                            Review review = reviewOpt.get();
                            review.setIsHidden(true);
                            reviewRepository.save(review);
                            log.info("🙈 Đã ẩn bài đánh giá reviewId={} do đơn hàng bị Trả hàng", review.getId());
                        }
                    }

                    updateProductAvgRating(product);
                    clearProductCache(productId);
                    log.info("✅ Đã trừ {} lượt bán và làm mới dữ liệu cho sản phẩm {}", quantity, productId);
                }
            } catch (Exception e) {
                log.error("❌ Lỗi xử lý trừ total_sold / ẩn review cho item: {}", e.getMessage());
            }
        }
    }

    private void updateProductAvgRating(Product product) {
        Double avg = reviewRepository.calculateAvgRatingByProductId(product.getId());
        long count = reviewRepository.countValidReviewsByProductId(product.getId());

        double roundedAvg = avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0;
        product.setAvgRating(roundedAvg);
        product.setTotalReviews((int) count);

        productRepository.save(product);
    }

    private void clearProductCache(UUID productId) {
        try {
            redisTemplate.delete("product:" + productId);
            redisTemplate.delete("product_detail:" + productId);
        } catch (Exception e) {
            log.warn("⚠️ Lỗi xóa Redis Cache: {}", e.getMessage());
        }
    }
}
