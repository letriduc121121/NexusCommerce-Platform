package com.ecommerce.cart.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {
    private UUID productId;
    private String productName;
    private String imageUrl;
    private BigDecimal price;
    private int quantity;
    private BigDecimal subtotal;

    /**
     * available/enough KHÔNG có ý nghĩa lâu dài khi lưu trong Redis - luôn được
     * CartService tính lại mới nhất mỗi lần gọi getCart(), vì tồn kho đổi liên tục.
     */
    private int available;
    private boolean enough;
}
