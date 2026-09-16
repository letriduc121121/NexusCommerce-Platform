package com.ecommerce.promotion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/** Frontend gửi lên ở trang checkout để xem thử tiền giảm. */
@Getter
@Setter
public class PreviewRequest {

    @NotBlank
    private String code;

    /** Thực tế nên lấy từ JWT; để trong body cho dễ test Swagger. */
    @NotNull
    private UUID userId;

    /** Tổng tiền hàng (chưa trừ giảm giá, chưa cộng ship). */
    @NotNull
    @PositiveOrZero
    private BigDecimal subtotal;
}
