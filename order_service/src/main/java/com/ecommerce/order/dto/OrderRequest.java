package com.ecommerce.order.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OrderRequest {

    @NotBlank(message = "Tên người nhận không được để trống")
    private String customerName;

    @NotBlank(message = "Số điện thoại nhận hàng không được để trống")
    private String phone;

    private String customerEmail;

    @NotBlank(message = "Địa chỉ nhận hàng không được để trống")
    private String shippingAddress;

    private String note;

    private String paymentMethod = "COD";

    /** Mã voucher khách áp (tuỳ chọn). */
    private String voucherCode;

    /** Tùy chọn bật sử dụng Xu tích lũy để giảm giá (1 Xu = 1 VNĐ). */
    private Boolean useCoins = false;

    /** Số xu khách hàng muốn áp dụng (nếu null sẽ áp dụng tối đa số xu hiện có). */
    private Long coinsToUse;

    @jakarta.validation.constraints.NotEmpty(message = "Danh sách sản phẩm mua không được để trống")
    private java.util.List<@jakarta.validation.Valid OrderItemRequest> items;
}
