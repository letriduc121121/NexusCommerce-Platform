package com.ecommerce.inventory.event;

import com.ecommerce.inventory.dto.StockItem;
import com.ecommerce.inventory.service.InventoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Chốt sổ / hoàn kho theo vòng đời đơn hàng.
 * <p>
 * Việc giữ chỗ đã xảy ra đồng bộ qua REST trước khi đơn được lưu (InventoryController#reserve),
 * nên ORDER_CREATED ở đây gần như luôn thành công - chỉ là chuyển reserved -> trừ hẳn.
 * Tổng cộng 4 lượt xử lý: lần đầu + 3 lần thử lại (chờ 1s, 2s, 4s).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final InventoryService inventoryService;
    private final ObjectMapper objectMapper;

    @RetryableTopic(attempts = "4", backoff = @Backoff(delay = 1000, multiplier = 2.0))
    @KafkaListener(topics = {"order-topic", "order-refunded-topic"}, groupId = "inventory-service-group")
    @SuppressWarnings("unchecked")
    public void handle(String message) throws Exception {
        Map<String, Object> event = objectMapper.readValue(message, Map.class);
        String orderId = String.valueOf(event.get("orderId"));
        List<StockItem> items = ((List<Map<String, Object>>) event.get("items")).stream()
                .map(row -> new StockItem(
                        UUID.fromString(String.valueOf(row.get("productId"))),
                        ((Number) row.get("quantity")).intValue()))
                .toList();

        String eventType = String.valueOf(event.get("eventType"));
        switch (eventType) {
            case "ORDER_CREATED" -> inventoryService.commit(orderId, items);
            case "ORDER_CANCELLED", "ORDER_REFUNDED" -> {
                inventoryService.restock(orderId, items);
                log.info("📦 [INVENTORY SERVICE] Đã hoàn tồn kho thành công cho đơn {}: event={}", orderId, eventType);
            }
            default -> log.debug("Bỏ qua order event không liên quan: {}", eventType);
        }
    }
}
