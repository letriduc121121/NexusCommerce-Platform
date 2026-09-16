package com.ecommerce.order.client;

import java.math.BigDecimal;

/** Kết quả promotion-service trả về khi trừ lượt voucher. */
public record DiscountResult(String voucherCode, BigDecimal discountAmount, boolean freeShipping) {
}
