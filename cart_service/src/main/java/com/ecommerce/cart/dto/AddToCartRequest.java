package com.ecommerce.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddToCartRequest {
    @NotNull(message = "ID sản phẩm không được để trống")
    private UUID productId;

    @Min(value = 1, message = "Số lượng mua phải lớn hơn 0")
    private int quantity;
}
