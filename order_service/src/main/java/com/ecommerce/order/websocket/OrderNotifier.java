package com.ecommerce.order.websocket;

import com.ecommerce.order.dto.OrderNotification;
import com.ecommerce.order.entity.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Đẩy biến động đơn hàng tới các Admin đang mở dashboard, ngay trong tiến trình xử lý.
 * <p>
 * Cố ý không đi vòng qua Kafka: Kafka chia tải theo group-id nên chỉ MỘT instance nhận được
 * message, trong khi phiên WebSocket lại nằm rải ở mọi instance - đẩy thẳng thì admin nào
 * cũng nhận được ngay. Kafka vẫn giữ nguyên vai trò cho Inventory / Notification Service.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderNotifier {

    public static final String ADMIN_TOPIC = "/topic/admin/orders";

    private final SimpMessagingTemplate messagingTemplate;

    public void newOrder(Order order) {
        send(OrderNotification.NEW_ORDER, order);
    }

    public void orderUpdated(Order order) {
        send(OrderNotification.ORDER_UPDATED, order);
    }

    /** Lỗi đẩy thông báo không được phép làm hỏng việc đặt hàng -> nuốt lỗi, chỉ ghi log. */
    private void send(String type, Order order) {
        try {
            messagingTemplate.convertAndSend(ADMIN_TOPIC, OrderNotification.of(type, order));
            log.info("🔔 [WS] {} -> đơn {}", type, order.getId());
        } catch (Exception e) {
            log.error("❌ [WS] Không đẩy được thông báo {}: {}", type, e.getMessage());
        }
    }
}
