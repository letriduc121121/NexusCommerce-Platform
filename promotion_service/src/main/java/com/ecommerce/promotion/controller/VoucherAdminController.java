package com.ecommerce.promotion.controller;

import com.ecommerce.promotion.dto.VoucherRequest;
import com.ecommerce.promotion.entity.Voucher;
import com.ecommerce.promotion.enums.VoucherStatus;
import com.ecommerce.promotion.service.VoucherAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/vouchers")
@RequiredArgsConstructor
public class VoucherAdminController {

    private final VoucherAdminService voucherAdminService;

    /** Admin tạo voucher mới. */
    @PostMapping
    public ResponseEntity<Voucher> create(@Valid @RequestBody VoucherRequest request) {
        Voucher voucher = voucherAdminService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(voucher);
    }

    /** Danh sách toàn bộ voucher. */
    @GetMapping
    public ResponseEntity<List<Voucher>> findAll() {
        return ResponseEntity.ok(voucherAdminService.findAll());
    }

    /** Chi tiết 1 voucher. */
    @GetMapping("/{id}")
    public ResponseEntity<Voucher> findById(@PathVariable Long id) {
        return ResponseEntity.ok(voucherAdminService.findById(id));
    }

    /** Cập nhật voucher. */
    @PutMapping("/{id}")
    public ResponseEntity<Voucher> update(@PathVariable Long id, @Valid @RequestBody VoucherRequest request) {
        return ResponseEntity.ok(voucherAdminService.update(id, request));
    }

    /** Tạm dừng / kích hoạt voucher: PATCH /{id}/status?status=ACTIVE|PAUSED */
    @PatchMapping("/{id}/status")
    public ResponseEntity<Voucher> setStatus(@PathVariable Long id, @RequestParam VoucherStatus status) {
        return ResponseEntity.ok(voucherAdminService.setStatus(id, status));
    }

    /** Xoá voucher (đã có lượt dùng -> tự chuyển sang ENDED thay vì xoá). */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        voucherAdminService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
