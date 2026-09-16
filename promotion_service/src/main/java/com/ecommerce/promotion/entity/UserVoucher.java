package com.ecommerce.promotion.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Ví voucher của 1 user + bản ghi sử dụng.
 *
 * source : "CLAIM" | "ASSIGN" | "AUTO"
 * status : "AVAILABLE" (chưa dùng) -> "USED" (đã áp vào đơn). Đơn huỷ -> về lại "AVAILABLE".
 */
@Entity
@Table(name = "user_vouchers",
        uniqueConstraints = @UniqueConstraint(columnNames = {"voucher_id", "user_id"}))
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class UserVoucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "voucher_id", nullable = false)
    private Long voucherId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 20)
    private String source;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "AVAILABLE";

    @Column(name = "claimed_at", nullable = false)
    private LocalDateTime claimedAt;

    @Column(name = "order_id", unique = true)
    private UUID orderId;

    @Column(name = "discount_amount", precision = 12, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @PrePersist
    void onCreate() {
        if (claimedAt == null) claimedAt = LocalDateTime.now();
    }
}
