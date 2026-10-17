package com.ecommerce.product.client;

import com.ecommerce.product.dto.VerifyPurchaseResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderClient {

    private final RestTemplate restTemplate;

    @Value("${order-service.url:http://localhost:8082}")
    private String orderServiceUrl;

    public VerifyPurchaseResponse verifyPurchase(UUID orderItemId, UUID userId) {
        String url = String.format("%s/api/v1/orders/verify-purchase?orderItemId=%s&userId=%s",
                orderServiceUrl, orderItemId, userId);
        log.info("📞 [PRODUCT SERVICE] Gọi REST API xác minh mua hàng sang Order Service: {}", url);
        try {
            return restTemplate.getForObject(url, VerifyPurchaseResponse.class);
        } catch (Exception e) {
            log.error("❌ Lỗi gọi verifyPurchase từ order_service: {}", e.getMessage());
            return VerifyPurchaseResponse.builder()
                    .eligible(false)
                    .message("Không thể kết nối đến Order Service để xác minh mua hàng!")
                    .build();
        }
    }
}
