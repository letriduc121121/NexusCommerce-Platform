package com.ecommerce.order.event;

import com.ecommerce.order.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "payment-topic", groupId = "order-service-group")
    public void handlePaymentEvent(String message) {
        log.info("📩 [ORDER] Nhận sự kiện thanh toán: {}", message);
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> event = objectMapper.readValue(message, Map.class);
            UUID orderId = UUID.fromString(event.get("orderId").toString());
            String paymentStatus = event.get("paymentStatus").toString();

            // VNPay thanh toán thành công -> giữ PENDING chờ Admin duyệt, chỉ cập nhật paymentStatus
            // VNPay thanh toán thất bại -> hủy đơn
            if ("PAID".equalsIgnoreCase(paymentStatus)) {
                orderService.updateOrderStatus(orderId, "PENDING", "PAID");
            } else {
                orderService.updateOrderStatus(orderId, "CANCELLED", paymentStatus);
            }

            log.info("✅ [ORDER] Cập nhật đơn hàng {} -> Thanh toán: {}", orderId, paymentStatus);
        } catch (Exception e) {
            log.error("❌ [ORDER] Lỗi xử lý sự kiện thanh toán: {}", e.getMessage());
        }
    }
}
