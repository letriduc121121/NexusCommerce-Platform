package com.ecommerce.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class OrderItemRequest {

    @NotNull(message = "Product ID không được để trống")
    private UUID productId;

    @NotBlank(message = "Tên sản phẩm không được để trống")
    private String productName;

    private String imageUrl;

    @NotNull(message = "Giá sản phẩm không được để trống")
    @Min(value = 0, message = "Giá sản phẩm không được nhỏ hơn 0")
    private BigDecimal price;

    @NotNull(message = "Số lượng mua không được để trống")
    @Min(value = 1, message = "Số lượng mua phải lớn hơn 0")
    private Integer quantity;
}
