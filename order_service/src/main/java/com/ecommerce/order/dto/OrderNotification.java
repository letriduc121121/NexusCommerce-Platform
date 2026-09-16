package com.ecommerce.order.dto;

import com.ecommerce.order.entity.Order;

import java.math.BigDecimal;

/**
 * Gói tin đẩy qua WebSocket cho màn hình Admin (chuông thông báo + bảng đơn hàng).
 * Chỉ mang đủ dữ liệu để hiển thị, không mang cả entity Order.
 */
public record OrderNotification(
        String type,
        String orderId,
        String customerName,
        String phone,
        BigDecimal totalAmount,
        String status,
        String paymentStatus,
        String paymentMethod,
        int itemCount,
        String createdAt) {

    /** Đơn mới vừa đặt - đây là loại làm chuông admin kêu. */
    public static final String NEW_ORDER = "NEW_ORDER";

    /** Đơn đã có đổi trạng thái (admin duyệt, VNPay báo thanh toán...) - chỉ đồng bộ lại bảng. */
    public static final String ORDER_UPDATED = "ORDER_UPDATED";

    public static OrderNotification of(String type, Order order) {
        return new OrderNotification(
                type,
                order.getId().toString(),
                order.getCustomerName(),
                order.getPhone(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getPaymentStatus(),
                order.getPaymentMethod(),
                order.getOrderItems().size(),
                order.getCreatedAt().toString());
    }
}
