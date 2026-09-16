package com.ecommerce.order.controller;

import com.ecommerce.order.dto.BulkApprovalResult;
import com.ecommerce.order.dto.OrderRequest;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // ==================== KHÁCH HÀNG ====================

    @PostMapping
    public ResponseEntity<Order> createOrder(Authentication auth, @Valid @RequestBody OrderRequest request) {
        Order order = orderService.createOrder(userId(auth), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @GetMapping
    public ResponseEntity<List<Order>> getOrderHistory(Authentication auth) {
        return ResponseEntity.ok(orderService.getOrderHistory(userId(auth)));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrderDetails(Authentication auth, @PathVariable UUID orderId) {
        return ResponseEntity.ok(orderService.getOrderDetails(userId(auth), orderId, userRole(auth)));
    }

    /** Khách tự huỷ đơn của chính mình - chỉ khi đơn còn PENDING hoặc CONFIRMED. */
    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<Order> cancelOrder(Authentication auth, @PathVariable UUID orderId) {
        return ResponseEntity.ok(orderService.cancelOrder(userId(auth), orderId));
    }

    // ==================== ADMIN ====================

    /** Bỏ trống status = lấy tất cả. Truyền PENDING để lấy danh sách đơn đang chờ duyệt. */
    @GetMapping("/admin")
    public ResponseEntity<List<Order>> getAllOrders(@RequestParam(required = false) String status) {
        return ResponseEntity.ok(orderService.getAllOrders(status));
    }

    /** Duyệt một lượt mọi đơn PENDING đủ điều kiện - đơn thanh toán online chưa có tiền sẽ bị bỏ qua. */
    @PutMapping("/admin/approve-all")
    public ResponseEntity<BulkApprovalResult> approveAllPending() {
        return ResponseEntity.ok(orderService.approveAllPending());
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<Order> updateOrderStatus(
            @PathVariable UUID orderId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String paymentStatus) {
        return ResponseEntity.ok(orderService.updateOrderStatus(orderId, status, paymentStatus));
    }

    // ==================== HỖ TRỢ ====================

    private UUID userId(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            throw new IllegalArgumentException("Bạn cần đăng nhập để thực hiện thao tác này!");
        }
        return UUID.fromString(auth.getPrincipal().toString());
    }

    private String userRole(Authentication auth) {
        if (auth == null || auth.getAuthorities() == null) return "ROLE_USER";
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("ROLE_USER");
    }
}
