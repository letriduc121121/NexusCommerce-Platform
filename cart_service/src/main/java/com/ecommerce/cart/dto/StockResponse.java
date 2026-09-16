package com.ecommerce.cart.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Khớp đúng các trường Inventory Service trả về ở GET /api/v1/inventories/{productId}. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockResponse {
    private UUID productId;
    private String productName;
    private String imageUrl;
    private int quantity;
    private int reserved;
    private int available;
    private boolean active;
}
