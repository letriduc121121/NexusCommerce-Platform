package com.ecommerce.promotion.service;

import com.ecommerce.promotion.dto.PreviewRequest;
import com.ecommerce.promotion.dto.PreviewResponse;
import com.ecommerce.promotion.dto.RedeemRequest;
import com.ecommerce.promotion.dto.RedeemResponse;
import com.ecommerce.promotion.entity.UserVoucher;
import com.ecommerce.promotion.entity.Voucher;
import com.ecommerce.promotion.enums.VoucherStatus;
import com.ecommerce.promotion.exception.PromotionException;
import com.ecommerce.promotion.repository.UserVoucherRepository;
import com.ecommerce.promotion.repository.VoucherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoucherRedeemService {

    private final VoucherRepository voucherRepository;
    private final UserVoucherRepository userVoucherRepository;
    private final DiscountCalculator calculator;

    // ==================== PREVIEW (chỉ đọc — trang checkout) ====================

    public PreviewResponse preview(PreviewRequest req) {
        Voucher v = loadAndValidate(req.getCode(), req.getUserId(), req.getSubtotal());
        BigDecimal discount = calculator.calculate(v, req.getSubtotal());

        return PreviewResponse.builder()
                .valid(true)
                .voucherCode(v.getCode())
                .discountType(v.getDiscountType().name())
                .discountAmount(discount)
                .freeShipping(calculator.isFreeShipping(v))
                .message("Áp dụng mã thành công")
                .build();
    }

    // ==================== REDEEM (order-service gọi khi tạo đơn) ====================

    @Transactional
    public RedeemResponse redeem(RedeemRequest req) {
        UUID userId = req.getUserId();
        UUID orderId = req.getOrderId();

        // 1) Idempotent: đơn này redeem rồi -> trả kết quả cũ, không làm lại
        Optional<UserVoucher> byOrder = userVoucherRepository.findByOrderId(orderId);
        if (byOrder.isPresent()) {
            UserVoucher uv = byOrder.get();
            Voucher v = voucherRepository.findById(uv.getVoucherId())
                    .orElseThrow(() -> PromotionException.notFound("Voucher không tồn tại"));
            return toResponse(v, uv.getDiscountAmount());
        }

        // 2) Kiểm tra hợp lệ (giống preview)
        Voucher voucher = loadAndValidate(req.getCode(), userId, req.getSubtotal());
        BigDecimal discount = calculator.calculate(voucher, req.getSubtotal());

        // 3) Trừ 1 lượt — NGUYÊN TỬ. 0 = hết lượt
        if (voucherRepository.consume(voucher.getId()) == 0) {
            throw PromotionException.conflict("Mã đã hết lượt hoặc hết hạn");
        }

        // 4) Lấy dòng ví (nếu user đã "lưu" trước) hoặc tạo mới
        UserVoucher uv = userVoucherRepository.findByVoucherIdAndUserId(voucher.getId(), userId)
                .orElseGet(() -> UserVoucher.builder()
                        .voucherId(voucher.getId())
                        .userId(userId)
                        .source("AUTO")
                        .status("AVAILABLE")
                        .claimedAt(LocalDateTime.now())
                        .build());

        // 5) Guard "1 user / 1 voucher / 1 lần"
        if ("USED".equals(uv.getStatus())) {
            // ném lỗi -> @Transactional rollback -> consume() ở bước 3 tự hoàn
            throw PromotionException.conflict("Bạn đã sử dụng mã này rồi");
        }

        uv.setStatus("USED");
        uv.setOrderId(orderId);
        uv.setDiscountAmount(discount);
        uv.setUsedAt(LocalDateTime.now());

        try {
            userVoucherRepository.save(uv);
        } catch (DataIntegrityViolationException e) {
            // 2 request song song cùng user -> dính UNIQUE(voucher_id, user_id)
            throw PromotionException.conflict("Bạn đã sử dụng mã này rồi");
        }

        log.info("🎟️ redeem {} cho đơn {} - giảm {}", voucher.getCode(), orderId, discount);
        return toResponse(voucher, discount);
    }

    // ==================== RELEASE (đơn bị huỷ) ====================

    @Transactional
    public void release(UUID orderId) {
        UserVoucher uv = userVoucherRepository.findByOrderId(orderId).orElse(null);
        if (uv == null || !"USED".equals(uv.getStatus())) {
            return; // không có gì để hoàn - idempotent
        }

        voucherRepository.refund(uv.getVoucherId());

        uv.setStatus("AVAILABLE");
        uv.setOrderId(null);
        uv.setDiscountAmount(null);
        uv.setUsedAt(null);
        userVoucherRepository.save(uv);

        log.info("↩️ Hoàn voucher của đơn {}", orderId);
    }

    // ==================== HỖ TRỢ ====================

    /** Kiểm tra mọi điều kiện dùng mã. Sai -> ném PromotionException. Đúng -> trả Voucher. */
    private Voucher loadAndValidate(String code, UUID userId, BigDecimal subtotal) {
        Voucher v = voucherRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> PromotionException.notFound("Mã không tồn tại"));

        if (v.getStatus() != VoucherStatus.ACTIVE) {
            throw PromotionException.conflict("Mã đã ngừng áp dụng");
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(v.getUsableFrom()) || now.isAfter(v.getUsableTo())) {
            throw PromotionException.conflict("Mã đã hết hạn sử dụng");
        }

        if (v.getTotalQuantity() != null && v.getUsedCount() >= v.getTotalQuantity()) {
            throw PromotionException.conflict("Mã đã hết lượt sử dụng");
        }

        if (subtotal.compareTo(v.getMinOrderAmount()) < 0) {
            throw PromotionException.badRequest(
                    "Đơn tối thiểu " + v.getMinOrderAmount().toBigInteger() + "đ để dùng mã này");
        }

        // "1 user / 1 voucher / 1 lần"
        userVoucherRepository.findByVoucherIdAndUserId(v.getId(), userId).ifPresent(uv -> {
            if ("USED".equals(uv.getStatus())) {
                throw PromotionException.conflict("Bạn đã sử dụng mã này rồi");
            }
        });

        return v;
    }

    private RedeemResponse toResponse(Voucher v, BigDecimal discount) {
        return RedeemResponse.builder()
                .voucherCode(v.getCode())
                .discountAmount(discount)
                .freeShipping(calculator.isFreeShipping(v))
                .build();
    }
}
