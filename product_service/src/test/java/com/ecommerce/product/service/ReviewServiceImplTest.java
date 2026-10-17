package com.ecommerce.product.service;

import com.ecommerce.product.client.OrderClient;
import com.ecommerce.product.dto.CreateReviewRequest;
import com.ecommerce.product.dto.ReviewResponse;
import com.ecommerce.product.dto.UpdateReviewRequest;
import com.ecommerce.product.dto.VerifyPurchaseResponse;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.entity.Review;
import com.ecommerce.product.repository.ProductRepository;
import com.ecommerce.product.repository.ReviewRepository;
import com.ecommerce.product.service.impl.ReviewServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderClient orderClient;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private UUID userId;
    private UUID orderItemId;
    private UUID productId;
    private UUID reviewId;
    private Product sampleProduct;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        orderItemId = UUID.randomUUID();
        productId = UUID.randomUUID();
        reviewId = UUID.randomUUID();

        sampleProduct = Product.builder()
                .id(productId)
                .name("Áo Thun Nam Premium")
                .sku("ATN-01")
                .price(BigDecimal.valueOf(150000))
                .avgRating(0.0)
                .totalReviews(0)
                .totalSold(0)
                .build();
    }

    @Test
    @DisplayName("TC-PS-01: Tạo đánh giá >= 50 ký tự + 0 ảnh -> Thưởng 100 Xu")
    void createReview_50CharsNoPhoto_Awards100Coins() {
        // Arrange
        String comment = "Sản phẩm chất lượng cao, mặc rất vừa vặn thoải mái, giao hàng cực nhanh chóng!";
        CreateReviewRequest request = CreateReviewRequest.builder()
                .orderItemId(orderItemId)
                .rating(5)
                .comment(comment)
                .build();

        VerifyPurchaseResponse verifyResp = VerifyPurchaseResponse.builder()
                .eligible(true)
                .productId(productId.toString())
                .variantInfo("Màu Đen, Size L")
                .build();

        when(reviewRepository.existsByOrderItemId(orderItemId)).thenReturn(false);
        when(orderClient.verifyPurchase(orderItemId, userId)).thenReturn(verifyResp);
        when(productRepository.findById(productId)).thenReturn(Optional.of(sampleProduct));

        Review savedReview = Review.builder()
                .id(reviewId)
                .orderItemId(orderItemId)
                .productId(productId)
                .userId(userId)
                .userFullName("Khách hàng")
                .rating(5)
                .comment(comment)
                .variantInfo("Màu Đen, Size L")
                .coinsAwarded(100)
                .isHidden(false)
                .isEdited(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);
        when(reviewRepository.calculateAvgRatingByProductId(productId)).thenReturn(5.0);
        when(reviewRepository.countValidReviewsByProductId(productId)).thenReturn(1L);

        // Act
        ReviewResponse response = reviewService.createReview(userId, request);

        // Assert
        assertNotNull(response);
        assertEquals(100, response.getCoinsAwarded());
        assertEquals(5, response.getRating());

        verify(kafkaTemplate, times(1)).send(eq("reward-topic"), eq(userId.toString()), any());
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    @DisplayName("TC-PS-02: Tạo đánh giá >= 50 ký tự + Có ảnh -> Thưởng 200 Xu")
    void createReview_50CharsWithPhoto_Awards200Coins() {
        // Arrange
        String comment = "Sản phẩm quá tuyệt vời, đính kèm ảnh chụp thực tế cho mọi người xem nét từng đường kim mũi chỉ!";
        CreateReviewRequest request = CreateReviewRequest.builder()
                .orderItemId(orderItemId)
                .rating(5)
                .comment(comment)
                .photoUrls(List.of("https://cloudinary.com/img1.jpg"))
                .build();

        VerifyPurchaseResponse verifyResp = VerifyPurchaseResponse.builder()
                .eligible(true)
                .productId(productId.toString())
                .variantInfo("Màu Trắng, Size M")
                .build();

        when(reviewRepository.existsByOrderItemId(orderItemId)).thenReturn(false);
        when(orderClient.verifyPurchase(orderItemId, userId)).thenReturn(verifyResp);
        when(productRepository.findById(productId)).thenReturn(Optional.of(sampleProduct));

        Review savedReview = Review.builder()
                .id(reviewId)
                .orderItemId(orderItemId)
                .productId(productId)
                .userId(userId)
                .rating(5)
                .comment(comment)
                .coinsAwarded(200)
                .isHidden(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);

        // Act
        ReviewResponse response = reviewService.createReview(userId, request);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getCoinsAwarded());

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(captor.capture());
        assertEquals(200, captor.getValue().getCoinsAwarded());
        assertEquals(1, captor.getValue().getImages().size());
    }

    @Test
    @DisplayName("TC-PS-03: Tạo đánh giá < 50 ký tự -> Không thưởng Xu (0 Xu)")
    void createReview_Under50Chars_Awards0Coins() {
        // Arrange
        CreateReviewRequest request = CreateReviewRequest.builder()
                .orderItemId(orderItemId)
                .rating(4)
                .comment("Hàng dùng tạm được.")
                .build();

        VerifyPurchaseResponse verifyResp = VerifyPurchaseResponse.builder()
                .eligible(true)
                .productId(productId.toString())
                .build();

        when(reviewRepository.existsByOrderItemId(orderItemId)).thenReturn(false);
        when(orderClient.verifyPurchase(orderItemId, userId)).thenReturn(verifyResp);
        when(productRepository.findById(productId)).thenReturn(Optional.of(sampleProduct));

        Review savedReview = Review.builder()
                .id(reviewId)
                .orderItemId(orderItemId)
                .productId(productId)
                .userId(userId)
                .rating(4)
                .comment("Hàng dùng tạm được.")
                .coinsAwarded(0)
                .build();

        when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);

        // Act
        ReviewResponse response = reviewService.createReview(userId, request);

        // Assert
        assertNotNull(response);
        assertEquals(0, response.getCoinsAwarded());
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("TC-PS-04: Từ chối tạo đánh giá nếu món hàng đã đánh giá trước đó")
    void createReview_AlreadyReviewed_ThrowsException() {
        // Arrange
        CreateReviewRequest request = CreateReviewRequest.builder()
                .orderItemId(orderItemId)
                .rating(5)
                .build();

        when(reviewRepository.existsByOrderItemId(orderItemId)).thenReturn(true);

        // Act & Assert
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> reviewService.createReview(userId, request));
        assertTrue(ex.getMessage().contains("đã được đánh giá rồi"));
    }

    @Test
    @DisplayName("TC-PS-05: Từ chối tạo đánh giá nếu Order Service xác minh không đủ điều kiện")
    void createReview_NotEligible_ThrowsException() {
        // Arrange
        CreateReviewRequest request = CreateReviewRequest.builder()
                .orderItemId(orderItemId)
                .rating(5)
                .build();

        VerifyPurchaseResponse verifyResp = VerifyPurchaseResponse.builder()
                .eligible(false)
                .message("Đã quá thời hạn 30 ngày kể từ khi nhận hàng")
                .build();

        when(reviewRepository.existsByOrderItemId(orderItemId)).thenReturn(false);
        when(orderClient.verifyPurchase(orderItemId, userId)).thenReturn(verifyResp);

        // Act & Assert
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> reviewService.createReview(userId, request));
        assertTrue(ex.getMessage().contains("Không đủ điều kiện đánh giá"));
    }

    @Test
    @DisplayName("TC-PS-06: Chỉnh sửa bài đánh giá lần đầu trong vòng 7 ngày -> Thành công")
    void updateReview_FirstTimeWithin7Days_Success() {
        // Arrange
        Review existingReview = Review.builder()
                .id(reviewId)
                .userId(userId)
                .productId(productId)
                .rating(4)
                .comment("Nội dung cũ")
                .isEdited(false)
                .createdAt(LocalDateTime.now().minusDays(2))
                .build();

        UpdateReviewRequest updateRequest = UpdateReviewRequest.builder()
                .rating(5)
                .comment("Nội dung đã được cập nhật xịn hơn")
                .build();

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(existingReview));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ReviewResponse response = reviewService.updateReview(userId, reviewId, updateRequest);

        // Assert
        assertNotNull(response);
        assertTrue(existingReview.getIsEdited());
        assertEquals(5, existingReview.getRating());
        assertEquals("Nội dung đã được cập nhật xịn hơn", existingReview.getComment());
    }

    @Test
    @DisplayName("TC-PS-07: Từ chối chỉnh sửa lần thứ 2")
    void updateReview_AlreadyEdited_ThrowsException() {
        // Arrange
        Review existingReview = Review.builder()
                .id(reviewId)
                .userId(userId)
                .isEdited(true)
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();

        UpdateReviewRequest updateRequest = UpdateReviewRequest.builder().rating(5).build();
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(existingReview));

        // Act & Assert
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> reviewService.updateReview(userId, reviewId, updateRequest));
        assertTrue(ex.getMessage().contains("chỉ được phép chỉnh sửa 1 lần duy nhất"));
    }

    @Test
    @DisplayName("TC-PS-08: Từ chối chỉnh sửa sau 7 ngày")
    void updateReview_After7Days_ThrowsException() {
        // Arrange
        Review existingReview = Review.builder()
                .id(reviewId)
                .userId(userId)
                .isEdited(false)
                .createdAt(LocalDateTime.now().minusDays(8))
                .build();

        UpdateReviewRequest updateRequest = UpdateReviewRequest.builder().rating(5).build();
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(existingReview));

        // Act & Assert
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> reviewService.updateReview(userId, reviewId, updateRequest));
        assertTrue(ex.getMessage().contains("Đã quá thời hạn 7 ngày"));
    }

    @Test
    @DisplayName("TC-PS-09: Admin toggle hide review -> Ẩn bài đánh giá")
    void toggleHideReview_Success() {
        // Arrange
        Review existingReview = Review.builder()
                .id(reviewId)
                .productId(productId)
                .isHidden(false)
                .build();

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(existingReview));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ReviewResponse response = reviewService.toggleHideReview(reviewId);

        // Assert
        assertNotNull(response);
        assertTrue(existingReview.getIsHidden());
    }

    @Test
    @DisplayName("TC-PS-10: Admin reply review -> Thêm câu trả lời của Shop")
    void replyReview_Success() {
        // Arrange
        Review existingReview = Review.builder()
                .id(reviewId)
                .build();

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(existingReview));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ReviewResponse response = reviewService.replyReview(reviewId, "Cảm ơn bạn đã tin tưởng shop!");

        // Assert
        assertNotNull(response);
        assertEquals("Cảm ơn bạn đã tin tưởng shop!", existingReview.getShopReply());
        assertNotNull(existingReview.getShopRepliedAt());
    }
}
