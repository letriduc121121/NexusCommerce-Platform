package com.ecommerce.cart.listener;

import com.ecommerce.cart.service.CartService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Đơn hàng tạo xong thì xoá giỏ. Chỉ nghe, không bắn lại gì cả - giỏ hàng đến/đi
 * đều là chuyện nội bộ của Cart Service.
 * <p>
 * group-id riêng để Kafka giao một bản sao sự kiện cho Cart, không tranh với
 * Inventory/Notification/Payment đang cùng nghe order-topic.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final CartService cartService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "order-topic", groupId = "cart-service-group")
    public void handle(String message) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> event = objectMapper.readValue(message, Map.class);

            if ("ORDER_CREATED".equals(event.get("eventType"))) {
                String userId = String.valueOf(event.get("userId"));
                cartService.clearCart(userId);
                log.info("🗑️ [CART] Đơn hàng tạo xong -> xoá giỏ hàng của {}", userId);
            }
        } catch (Exception e) {
            // Nuốt lỗi có chủ đích: xoá giỏ hỏng không nên làm treo consumer hay ném ra ngoài.
            log.error("❌ [CART] Lỗi xử lý sự kiện đơn hàng: {}", e.getMessage());
        }
    }
}
