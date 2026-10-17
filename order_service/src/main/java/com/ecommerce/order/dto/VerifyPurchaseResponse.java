package com.ecommerce.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyPurchaseResponse {
    private boolean eligible;
    private String orderItemId;
    private String orderId;
    private String productId;
    private String userId;
    private String variantInfo;
    private String message;
}
