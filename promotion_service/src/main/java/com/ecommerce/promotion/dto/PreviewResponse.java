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
public class PreviewResponse {
    private boolean valid;
    private String voucherCode;
    private String discountType;
    private BigDecimal discountAmount;
    private boolean freeShipping;
    private String message;
}
