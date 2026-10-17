package com.ecommerce.product.kafka;

import com.ecommerce.product.entity.Product;
import com.ecommerce.product.entity.Review;
import com.ecommerce.product.event.OrderEventPayload;
import com.ecommerce.product.repository.ProductRepository;
import com.ecommerce.product.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderEventListenerTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @InjectMocks
    private OrderEventListener orderEventListener;

    private UUID productId;
    private UUID orderItemId;
    private Product sampleProduct;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        orderItemId = UUID.randomUUID();

        sampleProduct = Product.builder()
                .id(productId)
                .name("Sản phẩm Test")
                .totalSold(10)
                .avgRating(4.5)
                .totalReviews(2)
                .build();
    }

    @Test
    @DisplayName("TC-PS-11: Kafka ORDER_DELIVERED -> Cộng dồn lượt bán total_sold và làm mới Redis cache")
    void handleOrderDelivered_IncrementsTotalSold() {
        // Arrange
        OrderEventPayload payload = OrderEventPayload.builder()
                .orderId(UUID.randomUUID().toString())
                .items(List.of(OrderEventPayload.OrderItemPayload.builder()
                        .productId(productId.toString())
                        .quantity(3)
                        .build()))
                .build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(sampleProduct));

        // Act
        orderEventListener.handleOrderDelivered(payload);

        // Assert
        assertEquals(13, sampleProduct.getTotalSold());
        verify(productRepository, times(1)).save(sampleProduct);
        verify(redisTemplate, times(1)).delete("product:" + productId);
    }

    @Test
    @DisplayName("TC-PS-12: Kafka ORDER_REFUNDED -> Trừ total_sold, ẩn bài đánh giá và tính lại sao")
    void handleOrderRefunded_DecrementsTotalSoldAndHidesReview() {
        // Arrange
        OrderEventPayload payload = OrderEventPayload.builder()
                .orderId(UUID.randomUUID().toString())
                .items(List.of(OrderEventPayload.OrderItemPayload.builder()
                        .productId(productId.toString())
                        .orderItemId(orderItemId.toString())
                        .quantity(2)
                        .build()))
                .build();

        Review sampleReview = Review.builder()
                .id(UUID.randomUUID())
                .orderItemId(orderItemId)
                .isHidden(false)
                .build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(sampleProduct));
        when(reviewRepository.findByOrderItemId(orderItemId)).thenReturn(Optional.of(sampleReview));
        when(reviewRepository.calculateAvgRatingByProductId(productId)).thenReturn(4.0);
        when(reviewRepository.countValidReviewsByProductId(productId)).thenReturn(1L);

        // Act
        orderEventListener.handleOrderRefunded(payload);

        // Assert
        assertEquals(8, sampleProduct.getTotalSold());
        assertTrue(sampleReview.getIsHidden());
        verify(reviewRepository, times(1)).save(sampleReview);
        verify(productRepository, times(2)).save(sampleProduct); // 1 lần cho total_sold, 1 lần cho avgRating
        verify(redisTemplate, times(1)).delete("product:" + productId);
    }
}
