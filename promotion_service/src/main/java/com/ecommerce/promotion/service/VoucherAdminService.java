package com.ecommerce.promotion.service;

import com.ecommerce.promotion.dto.VoucherRequest;
import com.ecommerce.promotion.entity.Voucher;
import com.ecommerce.promotion.enums.DiscountType;
import com.ecommerce.promotion.enums.VoucherStatus;
import com.ecommerce.promotion.exception.PromotionException;
import com.ecommerce.promotion.repository.VoucherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/** Nghiệp vụ quản trị voucher: CRUD. */
@Slf4j
@Service
@RequiredArgsConstructor
public class VoucherAdminService {

    private final VoucherRepository voucherRepository;

    // ==================== CREATE ====================

    @Transactional
    public Voucher create(VoucherRequest req) {
        String code = req.getCode().trim().toUpperCase();

        if (voucherRepository.existsByCodeIgnoreCase(code)) {
            throw PromotionException.conflict("Mã voucher \"" + code + "\" đã tồn tại");
        }
        validateRules(req);

        Voucher voucher = Voucher.builder()
                .code(code)
                .name(req.getName())
                .description(req.getDescription())
                .discountType(req.getDiscountType())
                .discountValue(req.getDiscountValue())
                .maxDiscountAmount(req.getMaxDiscountAmount())
                .minOrderAmount(req.getMinOrderAmount())
                .totalQuantity(req.getTotalQuantity())
                .usableFrom(req.getUsableFrom())
                .usableTo(req.getUsableTo())
                .build();

        Voucher saved = voucherRepository.save(voucher);
        log.info("✅ Đã tạo voucher {} (id={})", saved.getCode(), saved.getId());
        return saved;
    }

    // ==================== READ ====================

    public List<Voucher> findAll() {
        return voucherRepository.findAll();
    }

    /** Voucher công khai cho trang chủ: đang chạy, còn hạn, còn lượt. */
    public List<Voucher> findAvailable() {
        return voucherRepository.findAvailable();
    }

    public Voucher findById(Long id) {
        return voucherRepository.findById(id)
                .orElseThrow(() -> PromotionException.notFound("Không tìm thấy voucher: " + id));
    }

    // ==================== UPDATE ====================

    @Transactional
    public Voucher update(Long id, VoucherRequest req) {
        Voucher voucher = findById(id);
        String code = req.getCode().trim().toUpperCase();

        // Đổi mã -> phải không trùng với voucher KHÁC
        if (!voucher.getCode().equalsIgnoreCase(code) && voucherRepository.existsByCodeIgnoreCase(code)) {
            throw PromotionException.conflict("Mã voucher \"" + code + "\" đã tồn tại");
        }
        validateRules(req);

        voucher.setCode(code);
        voucher.setName(req.getName());
        voucher.setDescription(req.getDescription());
        voucher.setDiscountType(req.getDiscountType());
        voucher.setDiscountValue(req.getDiscountValue());
        voucher.setMaxDiscountAmount(req.getMaxDiscountAmount());
        voucher.setMinOrderAmount(req.getMinOrderAmount());
        voucher.setTotalQuantity(req.getTotalQuantity());
        voucher.setUsableFrom(req.getUsableFrom());
        voucher.setUsableTo(req.getUsableTo());

        Voucher saved = voucherRepository.save(voucher);
        log.info("✏️ Đã cập nhật voucher {} (id={})", saved.getCode(), saved.getId());
        return saved;
    }

    /** Tạm dừng / kích hoạt lại voucher. Không áp dụng cho voucher đã ENDED. */
    @Transactional
    public Voucher setStatus(Long id, VoucherStatus status) {
        if (status != VoucherStatus.ACTIVE && status != VoucherStatus.PAUSED) {
            throw PromotionException.badRequest("Chỉ chuyển được sang ACTIVE hoặc PAUSED");
        }
        Voucher voucher = findById(id);
        if (voucher.getStatus() == VoucherStatus.ENDED) {
            throw PromotionException.badRequest("Voucher đã kết thúc, không đổi được trạng thái");
        }
        voucher.setStatus(status);
        Voucher saved = voucherRepository.save(voucher);
        log.info("🔁 Voucher {} -> {}", saved.getCode(), status);
        return saved;
    }

    // ==================== DELETE ====================

    @Transactional
    public void delete(Long id) {
        Voucher voucher = findById(id);

        // Đã có người dùng -> KHÔNG xoá cứng (bảng user_vouchers còn tham chiếu), chỉ kết thúc.
        if (voucher.getUsedCount() != null && voucher.getUsedCount() > 0) {
            voucher.setStatus(VoucherStatus.ENDED);
            voucherRepository.save(voucher);
            log.info("🔚 Voucher {} đã có {} lượt dùng -> chuyển ENDED thay vì xoá", voucher.getCode(), voucher.getUsedCount());
        } else {
            voucherRepository.delete(voucher);
            log.info("🗑️ Đã xoá voucher {} (id={})", voucher.getCode(), id);
        }
    }

    // ==================== HỖ TRỢ ====================

    private void validateRules(VoucherRequest req) {
        if (req.getUsableTo().isBefore(req.getUsableFrom())) {
            throw PromotionException.badRequest("Thời gian kết thúc phải sau thời gian bắt đầu");
        }
        if (req.getDiscountType() == DiscountType.FREE_SHIPPING) {
            return; // FREE_SHIPPING: discountValue không dùng, để 0 cũng được
        }
        if (req.getDiscountValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw PromotionException.badRequest("Giá trị giảm phải lớn hơn 0");
        }
        if (req.getDiscountType() == DiscountType.PERCENTAGE
                && req.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw PromotionException.badRequest("Giảm theo % không được vượt quá 100");
        }
    }
}
