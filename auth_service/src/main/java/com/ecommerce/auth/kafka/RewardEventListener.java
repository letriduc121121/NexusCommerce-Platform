package com.ecommerce.auth.kafka;

import com.ecommerce.auth.entity.User;
import com.ecommerce.auth.event.CoinsRedeemedEvent;
import com.ecommerce.auth.event.CoinsRefundedEvent;
import com.ecommerce.auth.event.ReviewRewardGrantedEvent;
import com.ecommerce.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RewardEventListener {

    private final UserRepository userRepository;

    @Transactional
    @KafkaListener(topics = "reward-topic", groupId = "auth-service-group")
    public void handleRewardGranted(ReviewRewardGrantedEvent event) {
        log.info("🎁 [AUTH KAFKA CONSUMER] Nhận sự kiện REVIEW_REWARD_GRANTED cho userId={}, coins={}",
                event.getUserId(), event.getCoins());
        try {
            UUID userId = UUID.fromString(event.getUserId());
            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                long currentCoins = user.getRewardCoins() != null ? user.getRewardCoins() : 0L;
                int addedCoins = event.getCoins() != null ? event.getCoins() : 0;
                user.setRewardCoins(currentCoins + addedCoins);
                userRepository.save(user);
                log.info("💰 Đã cộng {} Xu thưởng cho user {}. Số dư mới: {} Xu", addedCoins, userId, user.getRewardCoins());
            } else {
                log.warn("⚠️ Không tìm thấy người dùng ID: {}", userId);
            }
        } catch (Exception e) {
            log.error("❌ Lỗi xử lý cộng Xu thưởng cho user: {}", e.getMessage());
        }
    }

    @Transactional
    @KafkaListener(topics = "coins-redeemed-topic", groupId = "auth-service-group")
    public void handleCoinsRedeemed(CoinsRedeemedEvent event) {
        log.info("🪙 [AUTH KAFKA CONSUMER] Nhận sự kiện COINS_REDEEMED cho đơn {}: userId={}, coinsUsed={}",
                event.getOrderId(), event.getUserId(), event.getCoinsUsed());
        try {
            UUID userId = UUID.fromString(event.getUserId());
            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                long currentCoins = user.getRewardCoins() != null ? user.getRewardCoins() : 0L;
                long coinsUsed = event.getCoinsUsed() != null ? event.getCoinsUsed() : 0L;
                user.setRewardCoins(Math.max(0L, currentCoins - coinsUsed));
                userRepository.save(user);
                log.info("💸 Đã khấu trừ {} Xu cho đơn {}. Số dư còn lại: {} Xu", coinsUsed, event.getOrderId(), user.getRewardCoins());
            }
        } catch (Exception e) {
            log.error("❌ Lỗi xử lý trừ Xu khi đặt hàng: {}", e.getMessage());
        }
    }

    @Transactional
    @KafkaListener(topics = "coins-refunded-topic", groupId = "auth-service-group")
    public void handleCoinsRefunded(CoinsRefundedEvent event) {
        log.info("🔄 [AUTH KAFKA CONSUMER] Nhận sự kiện COINS_REFUNDED cho đơn {}: userId={}, coinsRefunded={}",
                event.getOrderId(), event.getUserId(), event.getCoinsRefunded());
        try {
            UUID userId = UUID.fromString(event.getUserId());
            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                long currentCoins = user.getRewardCoins() != null ? user.getRewardCoins() : 0L;
                long coinsRefunded = event.getCoinsRefunded() != null ? event.getCoinsRefunded() : 0L;
                user.setRewardCoins(currentCoins + coinsRefunded);
                userRepository.save(user);
                log.info("↩️ Đã hoàn lại {} Xu cho đơn bị Hủy {}. Số dư mới: {} Xu", coinsRefunded, event.getOrderId(), user.getRewardCoins());
            }
        } catch (Exception e) {
            log.error("❌ Lỗi xử lý hoàn Xu khi hủy đơn: {}", e.getMessage());
        }
    }
}
