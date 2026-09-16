package com.ecommerce.inventory.dto;

import com.ecommerce.inventory.entity.Inventory;

import java.util.UUID;

public record StockResponse(
        UUID productId,
        String productName,
        String imageUrl,
        int quantity,
        int reserved,
        int available,
        boolean active
) {
    public static StockResponse from(Inventory i) {
        return new StockResponse(
                i.getProductId(),
                i.getProductName(),
                i.getImageUrl(),
                i.getQuantity(),
                i.getReservedQuantity(),
                i.getAvailable(),
                i.getActive()
        );
    }
}
