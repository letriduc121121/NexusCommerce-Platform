package com.ecommerce.promotion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/** order-service gọi khi tạo đơn. */
@Getter
@Setter
public class RedeemRequest {

    @NotBlank
    private String code;

    @NotNull
    private UUID userId;

    @NotNull
    private UUID orderId;

    @NotNull
    @PositiveOrZero
    private BigDecimal subtotal;
}
