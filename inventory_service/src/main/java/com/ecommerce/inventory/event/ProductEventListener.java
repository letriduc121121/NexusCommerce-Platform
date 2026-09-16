package com.ecommerce.inventory.event;

import com.ecommerce.inventory.service.InventoryService;
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
 * Tạo/ngừng bản ghi tồn kho theo vòng đời sản phẩm bên Product Service.
 * Các event khác (PRODUCT_UPDATED, ...) bị bỏ qua vì Inventory không lưu tên/giá.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductEventListener {

    private final InventoryService inventoryService;
    private final ObjectMapper objectMapper;

    @RetryableTopic(attempts = "4", backoff = @Backoff(delay = 1000, multiplier = 2.0))
    @KafkaListener(topics = "product-topic", groupId = "inventory-service-group")
    @SuppressWarnings("unchecked")
    public void handle(String message) throws Exception {
        Map<String, Object> event = objectMapper.readValue(message, Map.class);
        UUID productId = UUID.fromString(String.valueOf(event.get("productId")));

        switch (String.valueOf(event.get("eventType"))) {
            case "PRODUCT_CREATED", "PRODUCT_UPDATED" -> inventoryService.syncProduct(
                    productId,
                    (String) event.get("name"),
                    (String) event.get("imageUrl"));
            case "PRODUCT_DISCONTINUED" -> inventoryService.deactivate(productId);
            default -> log.debug("Bỏ qua product event không liên quan: {}", event.get("eventType"));
        }
    }
}
