package com.ecommerce.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/** Giữ chỗ / nhả chỗ cho toàn bộ một đơn hàng: được hết hoặc không được gì. */
public record ReserveRequest(
        @NotNull UUID orderId,
        @NotEmpty @Valid List<StockItem> items
) {
}
