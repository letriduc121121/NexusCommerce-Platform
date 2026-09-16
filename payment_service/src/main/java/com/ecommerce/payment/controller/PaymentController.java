package com.ecommerce.payment.controller;

import com.ecommerce.payment.dto.PaymentUrlResponse;
import com.ecommerce.payment.entity.Transaction;
import com.ecommerce.payment.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/vnpay-payment-url/{orderId}")
    public ResponseEntity<PaymentUrlResponse> getVNPayPaymentUrl(
            @PathVariable UUID orderId,
            @RequestParam BigDecimal amount,
            HttpServletRequest request) {
        String url = paymentService.createVNPayPaymentUrl(orderId, amount, request);
        return ResponseEntity.ok(new PaymentUrlResponse(url));
    }

    @GetMapping("/vnpay-callback")
    public ResponseEntity<Transaction> handleVNPayCallback(@RequestParam Map<String, String> queryParams) {
        Transaction transaction = paymentService.processVNPayCallback(queryParams);
        return ResponseEntity.ok(transaction);
    }
}
