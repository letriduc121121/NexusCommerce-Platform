package com.ecommerce.cart.controller;

import com.ecommerce.cart.dto.AddToCartRequest;
import com.ecommerce.cart.dto.CartResponse;
import com.ecommerce.cart.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("🔐 Authentication: {}", auth);
        if (auth == null || auth.getPrincipal() == null) {
            log.error("❌ No authentication found");
            throw new RuntimeException("Bạn chưa đăng nhập!");
        }
        String userId = auth.getName();
        log.info("✅ User ID: {}", userId);
        return userId;
    }

    @GetMapping
    public ResponseEntity<CartResponse> getCart() {
        return ResponseEntity.ok(cartService.getCart(getCurrentUserId()));
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponse> addToCart(@Valid @RequestBody AddToCartRequest request) {
        log.info("📝 Add to cart request: {}", request);
        return ResponseEntity.ok(cartService.addToCart(getCurrentUserId(), request));
    }

    /** Gộp giỏ hàng khách vãng lai vào giỏ tài khoản sau khi đăng nhập. */
    @PostMapping("/merge")
    public ResponseEntity<CartResponse> merge(@RequestBody List<AddToCartRequest> items) {
        return ResponseEntity.ok(cartService.merge(getCurrentUserId(), items));
    }

    @PutMapping("/items/{productId}")
    public ResponseEntity<CartResponse> updateQuantity(@PathVariable UUID productId, @RequestParam int quantity) {
        return ResponseEntity.ok(cartService.updateQuantity(getCurrentUserId(), productId, quantity));
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<CartResponse> removeItem(@PathVariable UUID productId) {
        return ResponseEntity.ok(cartService.removeItem(getCurrentUserId(), productId));
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart() {
        cartService.clearCart(getCurrentUserId());
        return ResponseEntity.ok().build();
    }
}
