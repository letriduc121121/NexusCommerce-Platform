package com.ecommerce.order.service.impl;

import com.ecommerce.order.client.DiscountResult;
import com.ecommerce.order.client.InventoryClient;
import com.ecommerce.order.client.PromotionClient;
import com.ecommerce.order.dto.BulkApprovalResult;
import com.ecommerce.order.dto.OrderItemRequest;
import com.ecommerce.order.dto.OrderRequest;
import com.ecommerce.order.dto.VerifyPurchaseResponse;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderItem;
import com.ecommerce.order.event.CoinsRedeemedEvent;
import com.ecommerce.order.event.CoinsRefundedEvent;
import com.ecommerce.order.event.OrderEventPayload;
import com.ecommerce.order.repository.OrderItemRepository;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.order.service.OrderService;
import com.ecommerce.order.websocket.OrderNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Vòng đời một đơn hàng:
 * <p>
 * 1. Giữ chỗ tồn kho (REST đồng bộ) -> 2. Lưu đơn PENDING -> 3. Bắn Kafka (Inventory chốt sổ,
 * Notification gửi mail) -> 4. Đẩy WebSocket cho Admin duyệt.
 * <p>
 * Đơn luôn dừng ở PENDING chờ Admin duyệt, kể cả khi đã thanh toán VNPay thành công.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final String COD = "COD";

    /** Máy trạng thái đơn hàng: từ trạng thái hiện tại chỉ được đi tới các trạng thái liệt kê. */
    private static final Map<String, Set<String>> ALLOWED_TRANSITIONS = Map.of(
            "PENDING", Set.of("CONFIRMED", "CANCELLED"),
            "CONFIRMED", Set.of("SHIPPING", "CANCELLED"),
            "SHIPPING", Set.of("DELIVERED"),
            "DELIVERED", Set.of("REFUNDED"),
            "REFUNDED", Set.of(),
            "CANCELLED", Set.of());

    /** Ngoài đời khách chỉ tự huỷ được khi đơn chưa được giao cho đơn vị vận chuyển. */
    private static final Set<String> CUSTOMER_CANCELLABLE = Set.of("PENDING", "CONFIRMED");

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final InventoryClient inventoryClient;
    private final PromotionClient promotionClient;
    private final OrderNotifier orderNotifier;

    // ==================== TẠO ĐƠN ====================

    /**
     * Vòng đời: sinh mã đơn -> giữ kho -> trừ voucher -> trừ Xu tích lũy -> lưu đơn PENDING -> bắn Kafka.
     */
    @Override
    @Transactional
    public Order createOrder(UUID userId, OrderRequest request) {
        UUID orderId = UUID.randomUUID();
        List<Map<String, Object>> stockItems = toStockItems(request.getItems());
        BigDecimal subtotal = calcSubtotal(request.getItems());

        // 1. Giữ chỗ tồn kho. Hết hàng -> ném lỗi
        inventoryClient.reserve(orderId, stockItems);

        // 2. Trừ lượt voucher (nếu có mã)
        DiscountResult discount = applyVoucher(request.getVoucherCode(), userId, orderId, subtotal, stockItems);

        // 3. Lưu đơn + phát sự kiện
        try {
            Order order = orderRepository.save(buildOrder(orderId, userId, request, subtotal, discount));
            log.info("✅ Đã tạo đơn hàng {} - chờ Admin duyệt", order.getId());

            // Nếu người dùng chọn dùng Xu -> phát sự kiện tiêu Xu
            if (order.getCoinsUsed() != null && order.getCoinsUsed() > 0) {
                publishCoinsRedeemedEvent(order);
            }

            publishEvent("ORDER_CREATED", order);
            orderNotifier.newOrder(order);
            return order;
        } catch (Exception e) {
            if (discount != null) promotionClient.release(orderId);
            inventoryClient.release(orderId, stockItems);
            throw e;
        }
    }

    private DiscountResult applyVoucher(String voucherCode, UUID userId, UUID orderId,
                                       BigDecimal subtotal, List<Map<String, Object>> stockItems) {
        if (voucherCode == null || voucherCode.isBlank()) {
            return null;
        }
        try {
            return promotionClient.redeem(voucherCode.trim(), userId, orderId, subtotal);
        } catch (Exception e) {
            inventoryClient.release(orderId, stockItems);
            throw e;
        }
    }

    private BigDecimal calcSubtotal(List<OrderItemRequest> items) {
        return items.stream()
                .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ==================== ĐỌC ====================

    @Override
    public List<Order> getOrderHistory(UUID userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public Order getOrderDetails(UUID userId, UUID orderId, String userRole) {
        Order order = findOrThrow(orderId);

        if (!isAdmin(userRole) && !order.getUserId().equals(userId)) {
            throw new AccessDeniedException("Bạn không có quyền xem đơn hàng này!");
        }
        return order;
    }

    @Override
    public List<Order> getAllOrders(String status) {
        return status == null || status.isBlank()
                ? orderRepository.findAllByOrderByCreatedAtDesc()
                : orderRepository.findByStatusOrderByCreatedAtDesc(status.toUpperCase());
    }

    // ==================== XÁC MINH MUA HÀNG (DÙNG CHO REVIEW) ====================

    @Override
    @Transactional(readOnly = true)
    public VerifyPurchaseResponse verifyPurchase(UUID orderItemId, UUID userId) {
        log.info("🔍 [ORDER SERVICE] Xác minh mua hàng: orderItemId={}, userId={}", orderItemId, userId);
        Optional<OrderItem> itemOpt = orderItemRepository.findByIdAndOrderUserId(orderItemId, userId);
        if (itemOpt.isEmpty()) {
            return VerifyPurchaseResponse.builder()
                    .eligible(false)
                    .message("Không tìm thấy thông tin sản phẩm trong đơn hàng của người dùng!")
                    .build();
        }
        OrderItem item = itemOpt.get();
        Order order = item.getOrder();

        if (!"DELIVERED".equalsIgnoreCase(order.getStatus())) {
            return VerifyPurchaseResponse.builder()
                    .eligible(false)
                    .message("Đơn hàng chưa được giao thành công!")
                    .build();
        }

        if (order.getUpdatedAt() != null && order.getUpdatedAt().isBefore(LocalDateTime.now().minusDays(30))) {
            return VerifyPurchaseResponse.builder()
                    .eligible(false)
                    .message("Đã quá thời hạn 30 ngày kể từ khi nhận hàng, không thể đánh giá!")
                    .build();
        }

        return VerifyPurchaseResponse.builder()
                .eligible(true)
                .orderItemId(item.getId().toString())
                .orderId(order.getId().toString())
                .productId(item.getProductId().toString())
                .userId(userId.toString())
                .message("Đủ điều kiện đánh giá!")
                .build();
    }

    // ==================== TRẢ HÀNG / HOÀN TIỀN (ADMIN) ====================

    @Override
    @Transactional
    public Order refundOrder(UUID orderId) {
        log.info("🔄 [ORDER SERVICE - ADMIN] Xác nhận Trả hàng / Hoàn tiền cho đơn: {}", orderId);
        Order order = findOrThrow(orderId);
        if (!"DELIVERED".equals(order.getStatus())) {
            throw new IllegalStateException("Chỉ đơn hàng đã giao (DELIVERED) mới có thể chuyển sang Trả hàng / Hoàn tiền!");
        }
        order.setStatus("REFUNDED");
        Order updated = orderRepository.save(order);

        publishEvent("ORDER_REFUNDED", updated);
        orderNotifier.orderUpdated(updated);
        return updated;
    }

    // ==================== HUỶ ĐƠN (KHÁCH TỰ THAO TÁC) ====================

    @Override
    @Transactional
    public Order cancelOrder(UUID userId, UUID orderId) {
        Order order = findOrThrow(orderId);

        if (!order.getUserId().equals(userId)) {
            throw new AccessDeniedException("Bạn không có quyền huỷ đơn hàng này!");
        }
        if (!CUSTOMER_CANCELLABLE.contains(order.getStatus())) {
            throw new IllegalStateException(customerCancelRejectReason(order.getStatus()));
        }

        order.setStatus("CANCELLED");
        Order updated = orderRepository.save(order);

        if (order.getCoinsUsed() != null && order.getCoinsUsed() > 0) {
            publishCoinsRefundedEvent(order);
        }

        publishEvent("ORDER_CANCELLED", updated);
        orderNotifier.orderUpdated(updated);
        return updated;
    }

    private String customerCancelRejectReason(String status) {
        return switch (status) {
            case "SHIPPING" -> "Đơn hàng đang được vận chuyển, không thể tự huỷ. Vui lòng liên hệ CSKH!";
            case "DELIVERED" -> "Đơn hàng đã giao thành công, không thể huỷ!";
            case "CANCELLED" -> "Đơn hàng này đã được huỷ trước đó!";
            default -> "Đơn hàng không thể huỷ ở trạng thái hiện tại!";
        };
    }

    // ==================== CẬP NHẬT TRẠNG THÁI (ADMIN THAO TÁC) ====================

    @Override
    @Transactional
    public Order updateOrderStatus(UUID orderId, String status, String paymentStatus) {
        Order order = findOrThrow(orderId);
        String currentStatus = order.getStatus();
        String newStatus = status == null || status.isBlank() ? currentStatus : status.toUpperCase();

        if (paymentStatus != null && !paymentStatus.isBlank()) {
            order.setPaymentStatus(paymentStatus.toUpperCase());
        }

        boolean statusChanged = !newStatus.equals(currentStatus);
        if (statusChanged) {
            validateTransition(order, currentStatus, newStatus);
            order.setStatus(newStatus);
        }

        Order updated = orderRepository.save(order);

        if (statusChanged) {
            if ("CANCELLED".equals(newStatus)) {
                if (order.getCoinsUsed() != null && order.getCoinsUsed() > 0) {
                    publishCoinsRefundedEvent(order);
                }
                publishEvent("ORDER_CANCELLED", updated);
            } else if ("DELIVERED".equals(newStatus)) {
                publishEvent("ORDER_DELIVERED", updated);
            } else if ("REFUNDED".equals(newStatus)) {
                publishEvent("ORDER_REFUNDED", updated);
            }
        }
        orderNotifier.orderUpdated(updated);

        return updated;
    }

    private void validateTransition(Order order, String currentStatus, String newStatus) {
        if (!ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Set.of()).contains(newStatus)) {
            throw new IllegalStateException(
                    "Không thể chuyển đơn hàng từ %s sang %s!".formatted(currentStatus, newStatus));
        }
        if ("CONFIRMED".equals(newStatus) && isAwaitingPayment(order)) {
            throw new IllegalStateException("Đơn thanh toán online chưa nhận được tiền, chưa thể duyệt!");
        }
    }

    private boolean isAwaitingPayment(Order order) {
        return !COD.equalsIgnoreCase(order.getPaymentMethod()) && !"PAID".equals(order.getPaymentStatus());
    }

    // ==================== DUYỆT HÀNG LOẠT (ADMIN THAO TÁC NHANH) ====================

    @Override
    @Transactional
    public BulkApprovalResult approveAllPending() {
        List<Order> pending = orderRepository.findByStatusOrderByCreatedAtDesc("PENDING");

        int approved = 0;
        int skipped = 0;
        for (Order order : pending) {
            if (isAwaitingPayment(order)) {
                skipped++;
                continue;
            }
            order.setStatus("CONFIRMED");
            Order updated = orderRepository.save(order);
            orderNotifier.orderUpdated(updated);
            approved++;
        }

        log.info("✅ Duyệt hàng loạt: {} đơn được duyệt, {} đơn bị bỏ qua (chờ thanh toán)", approved, skipped);
        return new BulkApprovalResult(approved, skipped);
    }

    // ==================== HỖ TRỢ ====================

    private Order findOrThrow(UUID orderId) {
        return orderRepository.findWithItemsById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng: " + orderId));
    }

    private boolean isAdmin(String userRole) {
        return userRole != null && userRole.toUpperCase().endsWith("ADMIN");
    }

    private Order buildOrder(UUID orderId, UUID userId, OrderRequest request,
                             BigDecimal subtotal, DiscountResult discount) {
        List<OrderItem> items = new ArrayList<>();
        for (OrderItemRequest item : request.getItems()) {
            BigDecimal lineTotal = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            items.add(OrderItem.builder()
                    .productId(item.getProductId())
                    .productName(item.getProductName())
                    .imageUrl(item.getImageUrl())
                    .price(item.getPrice())
                    .quantity(item.getQuantity())
                    .subtotal(lineTotal)
                    .build());
        }

        BigDecimal discountAmount = discount != null ? discount.discountAmount() : BigDecimal.ZERO;
        String voucherCode = discount != null ? discount.voucherCode() : null;

        // Tính khấu trừ Xu (1 Xu = 1 VNĐ)
        Long coinsUsed = 0L;
        BigDecimal coinsDiscountAmount = BigDecimal.ZERO;

        if (Boolean.TRUE.equals(request.getUseCoins())) {
            coinsUsed = request.getCoinsToUse() != null ? Math.max(0L, request.getCoinsToUse()) : 0L;
            coinsDiscountAmount = BigDecimal.valueOf(coinsUsed);
        }

        BigDecimal total = subtotal.subtract(discountAmount).subtract(coinsDiscountAmount).max(BigDecimal.ZERO);

        Order order = Order.builder()
                .id(orderId)
                .userId(userId)
                .customerName(request.getCustomerName())
                .phone(request.getPhone())
                .customerEmail(request.getCustomerEmail())
                .shippingAddress(request.getShippingAddress())
                .note(request.getNote())
                .paymentMethod(request.getPaymentMethod())
                .subtotal(subtotal)
                .discountAmount(discountAmount)
                .voucherCode(voucherCode)
                .coinsUsed(coinsUsed)
                .coinsDiscountAmount(coinsDiscountAmount)
                .totalAmount(total)
                .status("PENDING")
                .paymentStatus("PENDING")
                .build();

        items.forEach(item -> item.setOrder(order));
        order.setOrderItems(items);
        return order;
    }

    private List<Map<String, Object>> toStockItems(List<OrderItemRequest> items) {
        return items.stream()
                .map(i -> Map.<String, Object>of("productId", i.getProductId(), "quantity", i.getQuantity()))
                .toList();
    }

    private void publishEvent(String eventType, Order order) {
        try {
            OrderEventPayload event = OrderEventPayload.builder()
                    .eventType(eventType)
                    .orderId(order.getId().toString())
                    .userId(order.getUserId().toString())
                    .customerName(order.getCustomerName())
                    .customerEmail(order.getCustomerEmail())
                    .phone(order.getPhone())
                    .shippingAddress(order.getShippingAddress())
                    .totalAmount(order.getTotalAmount())
                    .paymentMethod(order.getPaymentMethod())
                    .status(order.getStatus())
                    .createdAt(order.getCreatedAt())
                    .items(order.getOrderItems().stream()
                            .map(i -> OrderEventPayload.OrderItemPayload.builder()
                                    .orderItemId(i.getId() != null ? i.getId().toString() : null)
                                    .productId(i.getProductId().toString())
                                    .productName(i.getProductName())
                                    .price(i.getPrice())
                                    .quantity(i.getQuantity())
                                    .subtotal(i.getSubtotal())
                                    .build())
                            .toList())
                    .build();

            kafkaTemplate.send("order-topic", order.getId().toString(), event);

            if ("DELIVERED".equalsIgnoreCase(eventType) || "ORDER_DELIVERED".equalsIgnoreCase(eventType)) {
                kafkaTemplate.send("order-delivered-topic", order.getId().toString(), event);
            } else if ("REFUNDED".equalsIgnoreCase(eventType) || "ORDER_REFUNDED".equalsIgnoreCase(eventType)) {
                kafkaTemplate.send("order-refunded-topic", order.getId().toString(), event);
            }

            log.info("📤 Đã phát sự kiện {} cho đơn {}", eventType, order.getId());
        } catch (Exception e) {
            log.error("❌ Không phát được sự kiện {}: {}", eventType, e.getMessage());
        }
    }

    private void publishCoinsRedeemedEvent(Order order) {
        try {
            CoinsRedeemedEvent event = CoinsRedeemedEvent.builder()
                    .orderId(order.getId().toString())
                    .userId(order.getUserId().toString())
                    .coinsUsed(order.getCoinsUsed())
                    .redeemedAt(LocalDateTime.now())
                    .build();
            kafkaTemplate.send("coins-redeemed-topic", order.getUserId().toString(), event);
            log.info("🪙 Đã phát sự kiện CoinsRedeemedEvent cho đơn {}: dùng {} Xu", order.getId(), order.getCoinsUsed());
        } catch (Exception e) {
            log.error("❌ Lỗi phát sự kiện CoinsRedeemedEvent: {}", e.getMessage());
        }
    }

    private void publishCoinsRefundedEvent(Order order) {
        try {
            CoinsRefundedEvent event = CoinsRefundedEvent.builder()
                    .orderId(order.getId().toString())
                    .userId(order.getUserId().toString())
                    .coinsRefunded(order.getCoinsUsed())
                    .refundedAt(LocalDateTime.now())
                    .build();
            kafkaTemplate.send("coins-refunded-topic", order.getUserId().toString(), event);
            log.info("🪙 Đã phát sự kiện CoinsRefundedEvent cho đơn {}: hoàn {} Xu", order.getId(), order.getCoinsUsed());
        } catch (Exception e) {
            log.error("❌ Lỗi phát sự kiện CoinsRefundedEvent: {}", e.getMessage());
        }
    }
}

