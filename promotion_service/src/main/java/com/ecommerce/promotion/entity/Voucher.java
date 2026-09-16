package com.ecommerce.promotion.entity;

import com.ecommerce.promotion.enums.DiscountType;
import com.ecommerce.promotion.enums.VoucherStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Chương trình voucher do Admin tạo.
 *
 * usedCount   : tổng lượt đã dùng thành công -> gate "hết voucher thì dừng".
 * issuedCount : tổng user đã nhận voucher vào ví -> chỉ để hiển thị.
 */
@Entity
@Table(name = "vouchers")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Voucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Mã khách nhập ở checkout, lưu chữ HOA. */
    @Column(nullable = false, unique = true, length = 40)
    private String code;

    /** Tên chương trình: "Giảm 50K cho đơn từ 300K". */
    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DiscountType discountType;

    /** % nếu PERCENTAGE, số tiền nếu FIXED_AMOUNT. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal discountValue;

    /** Trần giảm — chỉ dùng cho PERCENTAGE. null = không giới hạn. */
    @Column(precision = 12, scale = 2)
    private BigDecimal maxDiscountAmount;

    /** Giá trị đơn tối thiểu để áp mã. */
    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal minOrderAmount = BigDecimal.ZERO;

    /** null = không giới hạn tổng lượt dùng. */
    @Column
    private Integer totalQuantity;

    @Column(nullable = false)
    @Builder.Default
    private Integer usedCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer issuedCount = 0;

    @Column(nullable = false)
    private LocalDateTime usableFrom;

    @Column(nullable = false)
    private LocalDateTime usableTo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private VoucherStatus status = VoucherStatus.ACTIVE;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
