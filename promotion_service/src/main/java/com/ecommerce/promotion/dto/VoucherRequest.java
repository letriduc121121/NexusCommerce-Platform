package com.ecommerce.promotion.dto;

import com.ecommerce.promotion.enums.DiscountType;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Dữ liệu admin gửi lên khi tạo / sửa voucher. */
@Getter
@Setter
public class VoucherRequest {

    @NotBlank
    private String code;

    @NotBlank
    private String name;

    private String description;

    @NotNull
    private DiscountType discountType;

    /** > 0 với PERCENTAGE / FIXED_AMOUNT; = 0 với FREE_SHIPPING (service kiểm tra chi tiết). */
    @NotNull
    @PositiveOrZero
    private BigDecimal discountValue;

    /** Chỉ dùng cho PERCENTAGE, có thể để trống. */
    @Positive
    private BigDecimal maxDiscountAmount;

    @NotNull
    @PositiveOrZero
    private BigDecimal minOrderAmount;

    /** Tổng lượt dùng tối đa. Để trống = không giới hạn. */
    @Positive
    private Integer totalQuantity;

    @NotNull
    private LocalDateTime usableFrom;

    @NotNull
    private LocalDateTime usableTo;
}
