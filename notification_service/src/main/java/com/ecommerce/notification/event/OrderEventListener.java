package com.ecommerce.notification.event;

import com.ecommerce.notification.service.EmailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Gửi email cho khách theo vòng đời đơn hàng.
 * <p>
 * group-id riêng -> Kafka giao một bản sao mọi sự kiện cho service này, không tranh với Inventory.
 * Cố ý không dùng retry topic / DLQ: gửi mail hỏng thì khách vẫn tra cứu được đơn trên web,
 * khác với trừ kho - sai là hỏng dữ liệu thật.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final EmailService emailService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "order-topic", groupId = "notification-service-group")
    public void handle(String message) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> event = objectMapper.readValue(message, Map.class);
            String eventType = String.valueOf(event.get("eventType"));

            switch (eventType) {
                case "ORDER_CREATED" -> emailService.sendOrderCreated(event);
                case "ORDER_DELIVERED" -> emailService.sendOrderDelivered(event);
                case "ORDER_CANCELLED" -> emailService.sendOrderCancelled(event);
                default -> log.debug("Bỏ qua sự kiện không cần gửi mail: {}", eventType);
            }
        } catch (Exception e) {
            // Nuốt lỗi có chủ đích: ném ra ngoài sẽ kích hoạt DefaultErrorHandler thử lại 10 lần, treo consumer.
            log.error("❌ [NOTIFICATION] Lỗi xử lý sự kiện đơn hàng: {}", e.getMessage(), e);
        }
    }
}
