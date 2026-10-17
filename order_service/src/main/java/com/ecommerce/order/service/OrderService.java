package com.ecommerce.order.service;

import com.ecommerce.order.dto.BulkApprovalResult;
import com.ecommerce.order.dto.OrderRequest;
import com.ecommerce.order.entity.Order;

import java.util.List;
import java.util.UUID;

public interface OrderService {

    Order createOrder(UUID userId, OrderRequest request);

    List<Order> getOrderHistory(UUID userId);

    Order getOrderDetails(UUID userId, UUID orderId, String userRole);

    /** Khách tự huỷ đơn của chính mình - chỉ khi đơn chưa được giao cho đơn vị vận chuyển. */
    Order cancelOrder(UUID userId, UUID orderId);

    Order updateOrderStatus(UUID orderId, String status, String paymentStatus);

    /** Duyệt một lượt mọi đơn PENDING đủ điều kiện, để Admin không phải bấm từng đơn. */
    BulkApprovalResult approveAllPending();

    /** Danh sách cho Admin, lọc theo trạng thái nếu có (null / rỗng = lấy tất cả). */
    List<Order> getAllOrders(String status);

    /** API xác minh món hàng trong đơn có đủ điều kiện đánh giá không (Status = DELIVERED, trong 30 ngày). */
    com.ecommerce.order.dto.VerifyPurchaseResponse verifyPurchase(UUID orderItemId, UUID userId);

    /** Admin xác nhận trả hàng / hoàn tiền (DELIVERED -> REFUNDED). */
    Order refundOrder(UUID orderId);
}
