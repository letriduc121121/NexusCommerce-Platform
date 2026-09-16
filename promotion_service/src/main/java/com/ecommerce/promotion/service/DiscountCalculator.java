package com.ecommerce.promotion.service;

import com.ecommerce.promotion.entity.Voucher;
import com.ecommerce.promotion.enums.DiscountType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Tính số tiền giảm cho 1 voucher với subtotal cho trước. Thuần tính toán, không đụng DB. */
@Component
public class DiscountCalculator {

    public BigDecimal calculate(Voucher v, BigDecimal subtotal) {
        BigDecimal raw = switch (v.getDiscountType()) {
            case FIXED_AMOUNT -> v.getDiscountValue();
            case FREE_SHIPPING -> BigDecimal.ZERO;
            case PERCENTAGE -> {
                BigDecimal d = subtotal.multiply(v.getDiscountValue())
                        .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN);
                if (v.getMaxDiscountAmount() != null) {
                    d = d.min(v.getMaxDiscountAmount());
                }
                yield d;
            }
        };

        return raw.setScale(0, RoundingMode.DOWN)   // VND làm tròn xuống đồng
                .min(subtotal)                       // giảm không vượt tiền hàng
                .max(BigDecimal.ZERO);               // không âm
    }

    public boolean isFreeShipping(Voucher v) {
        return v.getDiscountType() == DiscountType.FREE_SHIPPING;
    }
}
