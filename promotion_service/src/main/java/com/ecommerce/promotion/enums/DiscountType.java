package com.ecommerce.promotion.enums;

public enum DiscountType {
    PERCENTAGE,     // giảm theo %, chặn trần bằng maxDiscountAmount
    FIXED_AMOUNT,   // giảm số tiền cố định
    FREE_SHIPPING   // miễn phí ship (discountValue bỏ qua)
}
