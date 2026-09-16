package com.ecommerce.order.dto;

/** Kết quả duyệt hàng loạt: đơn nào đợi thanh toán online thì bị bỏ qua, không tính vào approvedCount. */
public record BulkApprovalResult(int approvedCount, int skippedCount) {
}
