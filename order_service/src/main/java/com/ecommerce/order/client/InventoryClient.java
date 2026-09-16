package com.ecommerce.order.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Gọi đồng bộ sang Inventory Service để giữ/nhả chỗ tồn kho trước khi đơn hàng được lưu.
 * Lỗi (hết hàng, không kết nối được...) được ném thẳng ra để Order Service dừng tạo đơn ngay.
 */
@Component
@RequiredArgsConstructor
public class InventoryClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${services.inventory-service.url}")
    private String inventoryUrl;

    public void reserve(UUID orderId, List<Map<String, Object>> items) {
        call("/reserve", orderId, items);
    }

    public void release(UUID orderId, List<Map<String, Object>> items) {
        call("/release", orderId, items);
    }

    private void call(String path, UUID orderId, List<Map<String, Object>> items) {
        try {
            restTemplate.postForEntity(inventoryUrl + "/api/v1/inventories" + path,
                    Map.of("orderId", orderId, "items", items), Void.class);
        } catch (HttpStatusCodeException e) {
            throw new RuntimeException(extractMessage(e));
        } catch (Exception e) {
            throw new RuntimeException("Không thể kết nối tới kho hàng, vui lòng thử lại!");
        }
    }

    @SuppressWarnings("unchecked")
    private String extractMessage(HttpStatusCodeException e) {
        try {
            return ((Map<String, Object>) objectMapper.readValue(e.getResponseBodyAsString(), Map.class))
                    .get("message").toString();
        } catch (Exception parseError) {
            return "Kho hàng từ chối yêu cầu!";
        }
    }
}
