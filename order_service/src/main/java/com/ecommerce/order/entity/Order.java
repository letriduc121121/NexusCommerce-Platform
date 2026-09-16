package com.ecommerce.order.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    /** order-service tự sinh (UUID.randomUUID) trước khi lưu, để dùng chung với giữ kho + trừ voucher. */
    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";

    /** Tổng tiền hàng, trước khi trừ giảm giá. */
    @Column(name = "subtotal", precision = 12, scale = 2)
    private BigDecimal subtotal;

    /** Số tiền được voucher giảm. Luôn được set (mặc định 0) trong onCreate / builder. */
    @Column(name = "discount_amount", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    /** Mã voucher đã áp. null nếu không dùng. */
    @Column(name = "voucher_code", length = 40)
    private String voucherCode;

    /** Số tiền khách thực trả = subtotal - discountAmount + shippingFee. */
    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "customer_name", nullable = false, length = 100)
    private String customerName;

    @Column(nullable = false, length = 15)
    private String phone;

    @Column(name = "customer_email", length = 100)
    private String customerEmail;

    @Column(name = "shipping_address", nullable = false, columnDefinition = "TEXT")
    private String shippingAddress;

    @Column(length = 500)
    private String note;

    @Column(name = "shipping_fee", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal shippingFee = BigDecimal.ZERO;

    @Column(name = "payment_method", nullable = false, length = 20)
    @Builder.Default
    private String paymentMethod = "COD";

    @Column(name = "payment_status", nullable = false, length = 20)
    @Builder.Default
    private String paymentStatus = "PENDING";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<OrderItem> orderItems = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = "PENDING";
        if (paymentMethod == null) paymentMethod = "COD";
        if (paymentStatus == null) paymentStatus = "PENDING";
        if (shippingFee == null) shippingFee = BigDecimal.ZERO;
        if (discountAmount == null) discountAmount = BigDecimal.ZERO;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
