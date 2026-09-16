package com.ecommerce.promotion.event;

import com.ecommerce.promotion.service.VoucherRedeemService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Nghe sự kiện đơn hàng.
 * Đơn bị huỷ (ORDER_CANCELLED) -> hoàn lại lượt voucher đã dùng cho đơn đó.
 * Lỗi thì thử lại 4 lần (1s, 2s, 4s), giống inventory-service.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final VoucherRedeemService voucherRedeemService;
    private final ObjectMapper objectMapper;

    @RetryableTopic(attempts = "4", backoff = @Backoff(delay = 1000, multiplier = 2.0))
    @KafkaListener(topics = "order-topic", groupId = "promotion-service-group")
    public void handle(String message) throws Exception {
        Map<?, ?> event = objectMapper.readValue(message, Map.class);
        String eventType = String.valueOf(event.get("eventType"));

        if (!"ORDER_CANCELLED".equals(eventType)) {
            return; // các sự kiện khác không liên quan tới voucher
        }

        UUID orderId = UUID.fromString(String.valueOf(event.get("orderId")));
        voucherRedeemService.release(orderId);
        log.info("↩️ ORDER_CANCELLED -> đã hoàn voucher cho đơn {}", orderId);
    }
}
