package com.ecommerce.promotion.repository;

import com.ecommerce.promotion.entity.UserVoucher;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserVoucherRepository extends JpaRepository<UserVoucher, Long> {

    /** Voucher này đã có trong ví user chưa (dù AVAILABLE hay USED). */
    Optional<UserVoucher> findByVoucherIdAndUserId(Long voucherId, UUID userId);

    /** Tra bản ghi sử dụng theo đơn — dùng cho idempotent redeem + release khi huỷ đơn. */
    Optional<UserVoucher> findByOrderId(UUID orderId);

    /** Toàn bộ voucher trong ví 1 user — cho trang "Voucher của tôi". */
    List<UserVoucher> findByUserId(UUID userId);
}
