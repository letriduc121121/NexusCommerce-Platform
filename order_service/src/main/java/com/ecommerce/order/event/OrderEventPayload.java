package com.ecommerce.order.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEventPayload {
  private String eventType;
  private String orderId;
  private String userId;
  private String customerName;
  private String customerEmail;
  private String phone;
  private String shippingAddress;
  private BigDecimal totalAmount;
  private String paymentMethod;
  private String status;
  private LocalDateTime createdAt;
  private List<OrderItemPayload> items;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class OrderItemPayload {
    private String orderItemId;
    private String productId;
    private String productName;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal subtotal;
  }
}
