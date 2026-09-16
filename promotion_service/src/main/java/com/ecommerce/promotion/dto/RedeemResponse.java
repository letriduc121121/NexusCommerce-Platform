package com.ecommerce.promotion.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RedeemResponse {
    private String voucherCode;
    private BigDecimal discountAmount;
    private boolean freeShipping;
}
