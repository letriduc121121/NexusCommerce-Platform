package com.ecommerce.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Một dòng "sản phẩm + số lượng" - dùng chung cho reserve/release và khởi tạo kho. */
public record StockItem(
        @NotNull UUID productId,
        @NotNull @Min(0) Integer quantity
) {
}
