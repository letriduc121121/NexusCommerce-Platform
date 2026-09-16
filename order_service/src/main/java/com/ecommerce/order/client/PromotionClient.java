package com.ecommerce.order.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Gọi đồng bộ sang Promotion Service để trừ / hoàn lượt voucher.
 * <p>
 * redeem  : lỗi (hết lượt, đã dùng, sai điều kiện) được ném thẳng ra để Order Service dừng tạo đơn.
 * release : chỉ để dọn dẹp khi tạo đơn hỏng -> nuốt lỗi, chỉ ghi log.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PromotionClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${services.promotion-service.url}")
    private String promotionUrl;

    public DiscountResult redeem(String code, UUID userId, UUID orderId, BigDecimal subtotal) {
        Map<String, Object> body = Map.of(
                "code", code,
                "userId", userId.toString(),
                "orderId", orderId.toString(),
                "subtotal", subtotal);

        try {
            Map<?, ?> res = restTemplate.postForObject(
                    promotionUrl + "/api/v1/vouchers/redeem", body, Map.class);

            return new DiscountResult(
                    String.valueOf(res.get("voucherCode")),
                    new BigDecimal(String.valueOf(res.get("discountAmount"))),
                    Boolean.parseBoolean(String.valueOf(res.get("freeShipping"))));

        } catch (HttpStatusCodeException e) {
            throw new RuntimeException(extractMessage(e));
        } catch (Exception e) {
            throw new RuntimeException("Không thể kết nối tới dịch vụ khuyến mãi, vui lòng thử lại!");
        }
    }

    public void release(UUID orderId) {
        try {
            restTemplate.postForEntity(
                    promotionUrl + "/api/v1/vouchers/release?orderId=" + orderId, null, Void.class);
        } catch (Exception e) {
            log.error("❌ Không hoàn được voucher cho đơn {}: {}", orderId, e.getMessage());
        }
    }

    private String extractMessage(HttpStatusCodeException e) {
        try {
            Object message = objectMapper.readValue(e.getResponseBodyAsString(), Map.class).get("message");
            return message != null ? message.toString() : "Áp dụng voucher thất bại!";
        } catch (Exception ignored) {
            return "Áp dụng voucher thất bại!";
        }
    }
}
