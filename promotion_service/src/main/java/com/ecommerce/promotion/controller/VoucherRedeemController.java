package com.ecommerce.promotion.controller;

import com.ecommerce.promotion.dto.PreviewRequest;
import com.ecommerce.promotion.dto.PreviewResponse;
import com.ecommerce.promotion.dto.RedeemRequest;
import com.ecommerce.promotion.dto.RedeemResponse;
import com.ecommerce.promotion.entity.Voucher;
import com.ecommerce.promotion.service.VoucherAdminService;
import com.ecommerce.promotion.service.VoucherRedeemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/vouchers")
@RequiredArgsConstructor
public class VoucherRedeemController {

    private final VoucherRedeemService voucherRedeemService;
    private final VoucherAdminService voucherAdminService;

    /** Trang chủ: danh sách mã đang phát để khách xem & copy. */
    @GetMapping("/available")
    public ResponseEntity<List<Voucher>> available() {
        return ResponseEntity.ok(voucherAdminService.findAvailable());
    }

    /** Trang checkout: xem thử tiền giảm, KHÔNG trừ lượt. */
    @PostMapping("/preview")
    public ResponseEntity<PreviewResponse> preview(@Valid @RequestBody PreviewRequest request) {
        return ResponseEntity.ok(voucherRedeemService.preview(request));
    }

    /** order-service gọi khi tạo đơn: trừ lượt + khoá voucher cho user. */
    @PostMapping("/redeem")
    public ResponseEntity<RedeemResponse> redeem(@Valid @RequestBody RedeemRequest request) {
        return ResponseEntity.ok(voucherRedeemService.redeem(request));
    }

    /** order-service / Kafka gọi khi đơn bị huỷ: hoàn lượt. */
    @PostMapping("/release")
    public ResponseEntity<Void> release(@RequestParam UUID orderId) {
        voucherRedeemService.release(orderId);
        return ResponseEntity.noContent().build();
    }
}
