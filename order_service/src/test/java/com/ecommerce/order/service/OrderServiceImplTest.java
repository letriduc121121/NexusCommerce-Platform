package com.ecommerce.order.service;

import com.ecommerce.order.client.InventoryClient;
import com.ecommerce.order.client.PromotionClient;
import com.ecommerce.order.dto.OrderItemRequest;
import com.ecommerce.order.dto.OrderRequest;
import com.ecommerce.order.dto.VerifyPurchaseResponse;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderItem;
import com.ecommerce.order.event.CoinsRedeemedEvent;
import com.ecommerce.order.event.CoinsRefundedEvent;
import com.ecommerce.order.repository.OrderItemRepository;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.order.service.impl.OrderServiceImpl;
import com.ecommerce.order.websocket.OrderNotifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private InventoryClient inventoryClient;

    @Mock
    private PromotionClient promotionClient;

    @Mock
    private OrderNotifier orderNotifier;

    @InjectMocks
    private OrderServiceImpl orderService;

    private UUID userId;
    private UUID orderId;
    private UUID orderItemId;
    private UUID productId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        orderItemId = UUID.randomUUID();
        productId = UUID.randomUUID();
    }

    @Test
    @DisplayName("TC-OS-01: Verify Purchase -> Hợp lệ (status=DELIVERED, trong 30 ngày)")
    void verifyPurchase_Eligible() {
        // Arrange
        Order order = Order.builder()
                .id(orderId)
                .userId(userId)
                .status("DELIVERED")
                .updatedAt(LocalDateTime.now().minusDays(5))
                .build();

        OrderItem item = OrderItem.builder()
                .id(orderItemId)
                .productId(productId)
                .order(order)
                .build();

        when(orderItemRepository.findByIdAndOrderUserId(orderItemId, userId)).thenReturn(Optional.of(item));

        // Act
        VerifyPurchaseResponse response = orderService.verifyPurchase(orderItemId, userId);

        // Assert
        assertNotNull(response);
        assertTrue(response.isEligible());
        assertEquals(orderItemId.toString(), response.getOrderItemId());
        assertEquals(productId.toString(), response.getProductId());
    }

    @Test
    @DisplayName("TC-OS-02: Verify Purchase -> Không hợp lệ vì đơn chưa giao")
    void verifyPurchase_NotDelivered_Ineligible() {
        // Arrange
        Order order = Order.builder()
                .id(orderId)
                .userId(userId)
                .status("SHIPPING")
                .build();

        OrderItem item = OrderItem.builder()
                .id(orderItemId)
                .productId(productId)
                .order(order)
                .build();

        when(orderItemRepository.findByIdAndOrderUserId(orderItemId, userId)).thenReturn(Optional.of(item));

        // Act
        VerifyPurchaseResponse response = orderService.verifyPurchase(orderItemId, userId);

        // Assert
        assertNotNull(response);
        assertFalse(response.isEligible());
        assertTrue(response.getMessage().contains("chưa được giao thành công"));
    }

    @Test
    @DisplayName("TC-OS-03: Verify Purchase -> Không hợp lệ vì quá thời hạn 30 ngày")
    void verifyPurchase_Over30Days_Ineligible() {
        // Arrange
        Order order = Order.builder()
                .id(orderId)
                .userId(userId)
                .status("DELIVERED")
                .updatedAt(LocalDateTime.now().minusDays(35))
                .build();

        OrderItem item = OrderItem.builder()
                .id(orderItemId)
                .productId(productId)
                .order(order)
                .build();

        when(orderItemRepository.findByIdAndOrderUserId(orderItemId, userId)).thenReturn(Optional.of(item));

        // Act
        VerifyPurchaseResponse response = orderService.verifyPurchase(orderItemId, userId);

        // Assert
        assertNotNull(response);
        assertFalse(response.isEligible());
        assertTrue(response.getMessage().contains("thời hạn 30 ngày"));
    }

    @Test
    @DisplayName("TC-OS-04: Tạo đơn dùng Xu giảm giá -> Trừ tổng tiền và phát sự kiện CoinsRedeemedEvent")
    void createOrder_WithCoins_ReducesTotalAndPublishesEvent() {
        // Arrange
        OrderItemRequest itemReq = new OrderItemRequest();
        itemReq.setProductId(productId);
        itemReq.setProductName("Sản phẩm 1");
        itemReq.setPrice(BigDecimal.valueOf(200000));
        itemReq.setQuantity(1);

        OrderRequest request = new OrderRequest();
        request.setCustomerName("Nguyễn Văn A");
        request.setPhone("0987654321");
        request.setShippingAddress("Hà Nội");
        request.setPaymentMethod("COD");
        request.setUseCoins(true);
        request.setCoinsToUse(50000L); // Dung 50k Xu = 50k VND
        request.setItems(List.of(itemReq));

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Order createdOrder = orderService.createOrder(userId, request);

        // Assert
        assertNotNull(createdOrder);
        assertEquals(50000L, createdOrder.getCoinsUsed());
        assertEquals(BigDecimal.valueOf(150000), createdOrder.getTotalAmount()); // 200k - 50k = 150k

        verify(kafkaTemplate, times(1)).send(eq("coins-redeemed-topic"), eq(userId.toString()), any(CoinsRedeemedEvent.class));
    }

    @Test
    @DisplayName("TC-OS-05: Admin refund đơn hàng (DELIVERED -> REFUNDED) -> Bắn Kafka ORDER_REFUNDED")
    void refundOrder_Success() {
        // Arrange
        Order order = Order.builder()
                .id(orderId)
                .userId(userId)
                .status("DELIVERED")
                .build();

        when(orderRepository.findWithItemsById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Order refundedOrder = orderService.refundOrder(orderId);

        // Assert
        assertNotNull(refundedOrder);
        assertEquals("REFUNDED", refundedOrder.getStatus());
        verify(kafkaTemplate).send(eq("order-topic"), eq(orderId.toString()), any());
        verify(kafkaTemplate).send(eq("order-refunded-topic"), eq(orderId.toString()), any());
    }

    @Test
    @DisplayName("TC-OS-06: Khách hủy đơn có dùng Xu -> Phát Kafka CoinsRefundedEvent để hoàn Xu")
    void cancelOrder_WithCoins_PublishesCoinsRefundedEvent() {
        // Arrange
        Order order = Order.builder()
                .id(orderId)
                .userId(userId)
                .status("PENDING")
                .coinsUsed(20000L)
                .build();

        when(orderRepository.findWithItemsById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Order cancelledOrder = orderService.cancelOrder(userId, orderId);

        // Assert
        assertNotNull(cancelledOrder);
        assertEquals("CANCELLED", cancelledOrder.getStatus());
        verify(kafkaTemplate, times(1)).send(eq("coins-refunded-topic"), eq(userId.toString()), any(CoinsRefundedEvent.class));
    }
}
