package com.ecommerce.auth.kafka;

import com.ecommerce.auth.entity.User;
import com.ecommerce.auth.event.CoinsRedeemedEvent;
import com.ecommerce.auth.event.CoinsRefundedEvent;
import com.ecommerce.auth.event.ReviewRewardGrantedEvent;
import com.ecommerce.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RewardEventListenerTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RewardEventListener rewardEventListener;

    private UUID userId;
    private User sampleUser;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        sampleUser = User.builder()
                .id(userId)
                .email("user@example.com")
                .rewardCoins(100L)
                .build();
    }

    @Test
    @DisplayName("TC-AS-01: Kafka REVIEW_REWARD_GRANTED -> Cộng dồn xu thưởng cho User")
    void handleRewardGranted_AddsCoinsToUser() {
        // Arrange
        ReviewRewardGrantedEvent event = ReviewRewardGrantedEvent.builder()
                .userId(userId.toString())
                .coins(200)
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));

        // Act
        rewardEventListener.handleRewardGranted(event);

        // Assert
        assertEquals(300L, sampleUser.getRewardCoins()); // 100 + 200 = 300
        verify(userRepository, times(1)).save(sampleUser);
    }

    @Test
    @DisplayName("TC-AS-02: Kafka COINS_REDEEMED -> Trừ xu ví khi User đặt hàng")
    void handleCoinsRedeemed_DeductsCoinsFromUser() {
        // Arrange
        CoinsRedeemedEvent event = CoinsRedeemedEvent.builder()
                .userId(userId.toString())
                .orderId(UUID.randomUUID().toString())
                .coinsUsed(50L)
                .redeemedAt(LocalDateTime.now())
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));

        // Act
        rewardEventListener.handleCoinsRedeemed(event);

        // Assert
        assertEquals(50L, sampleUser.getRewardCoins()); // 100 - 50 = 50
        verify(userRepository, times(1)).save(sampleUser);
    }

    @Test
    @DisplayName("TC-AS-03: Kafka COINS_REFUNDED -> Hoàn xu ví khi đơn bị hủy")
    void handleCoinsRefunded_RestoresCoinsToUser() {
        // Arrange
        CoinsRefundedEvent event = CoinsRefundedEvent.builder()
                .userId(userId.toString())
                .orderId(UUID.randomUUID().toString())
                .coinsRefunded(50L)
                .refundedAt(LocalDateTime.now())
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));

        // Act
        rewardEventListener.handleCoinsRefunded(event);

        // Assert
        assertEquals(150L, sampleUser.getRewardCoins()); // 100 + 50 = 150
        verify(userRepository, times(1)).save(sampleUser);
    }
}
