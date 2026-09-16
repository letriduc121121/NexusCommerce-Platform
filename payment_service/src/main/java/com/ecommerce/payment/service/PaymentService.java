package com.ecommerce.payment.service;

import com.ecommerce.payment.config.VNPayConfig;
import com.ecommerce.payment.config.VNPayUtil;
import com.ecommerce.payment.entity.Transaction;
import com.ecommerce.payment.repository.TransactionRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final VNPayConfig vnPayConfig;
    private final TransactionRepository transactionRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate; // Đổi sang Object để tự động serialize JSON sạch

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    @Transactional
    public String createVNPayPaymentUrl(UUID orderId, BigDecimal totalAmount, HttpServletRequest request) {
        log.info("💸 [PAYMENT SERVICE] Tạo link VNPay cho Order ID: {}, Số tiền: {}", orderId, totalAmount);

        long amount = totalAmount.multiply(BigDecimal.valueOf(100)).longValue();
        String createDate = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());

        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        params.put("vnp_Amount", String.valueOf(amount));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", orderId.toString());
        params.put("vnp_OrderInfo", "Thanh toan don hang " + orderId);
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
        params.put("vnp_IpAddr", VNPayUtil.getIpAddress(request));
        params.put("vnp_CreateDate", createDate);

        // hashData và query VNPay trước nay giống hệt nhau (key=value nối bằng '&') nên chỉ cần dựng một chuỗi.
        String hashData = params.entrySet().stream()
                .filter(e -> e.getValue() != null && !e.getValue().isEmpty())
                .map(e -> encode(e.getKey()) + "=" + encode(e.getValue()))
                .collect(Collectors.joining("&"));

        String secureHash = VNPayUtil.hmacSHA512(vnPayConfig.getHashSecret(), hashData);
        String paymentUrl = vnPayConfig.getVnpayUrl() + "?" + hashData + "&vnp_SecureHash=" + secureHash;

        // Lưu lịch sử giao dịch ban đầu (PENDING)
        Transaction transaction = transactionRepository.findByOrderId(orderId)
                .orElseGet(() -> Transaction.builder().orderId(orderId).build());
        transaction.setAmount(totalAmount);
        transaction.setPaymentMethod("VNPAY");
        transaction.setStatus("PENDING");
        transactionRepository.save(transaction);

        return paymentUrl;
    }

    @Transactional
    public Transaction processVNPayCallback(Map<String, String> params) {
        String responseCode = params.get("vnp_ResponseCode");
        String orderIdStr = params.get("vnp_TxnRef");
        String transactionNo = params.get("vnp_TransactionNo");
        String secureHash = params.get("vnp_SecureHash");

        if (orderIdStr == null) {
            throw new RuntimeException("Mã giao dịch vnp_TxnRef không hợp lệ!");
        }

        UUID orderId = UUID.fromString(orderIdStr);
        Transaction transaction = transactionRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giao dịch cho đơn hàng: " + orderId));

        // Kiểm tra chữ ký bảo mật VNPay: lọc field vnp_*, bỏ hash cũ, sắp theo key rồi nối '&'.
        String signData = params.entrySet().stream()
                .filter(e -> e.getKey().startsWith("vnp_") && !e.getKey().equals("vnp_SecureHash") && !e.getKey().equals("vnp_SecureHashType"))
                .filter(e -> e.getValue() != null && !e.getValue().isEmpty())
                .sorted(Map.Entry.comparingByKey())
                .map(e -> encode(e.getKey()) + "=" + encode(e.getValue()))
                .collect(Collectors.joining("&"));

        String calculatedHash = VNPayUtil.hmacSHA512(vnPayConfig.getHashSecret(), signData);
        boolean isSignatureValid = calculatedHash.equalsIgnoreCase(secureHash);

        if (!isSignatureValid) {
            log.error("⚠️ [PAYMENT SERVICE] Chữ ký đối soát VNPay không chính xác!");
            transaction.setStatus("FAILED");
            return transactionRepository.save(transaction);
        }

        boolean isSuccess = "00".equals(responseCode);
        transaction.setStatus(isSuccess ? "SUCCESS" : "FAILED");
        transaction.setTransactionNo(transactionNo);
        Transaction savedTx = transactionRepository.save(transaction);

        log.info("{} [PAYMENT SERVICE] Thanh toán VNPay {} cho đơn hàng: {}", 
                isSuccess ? "✅" : "❌", isSuccess ? "Thành công" : "Thất bại", orderId);

        // Bắn sự kiện Map trực tiếp lên Kafka (Spring Kafka sẽ tự động convert sang JSON thô sạch)
        try {
            Map<String, Object> paymentEvent = new HashMap<>();
            paymentEvent.put("orderId", orderId);
            paymentEvent.put("paymentStatus", isSuccess ? "PAID" : "FAILED");

            kafkaTemplate.send("payment-topic", orderId.toString(), paymentEvent);
            log.info("📢 [PAYMENT SERVICE -> KAFKA] Đã phát sự kiện lên topic 'payment-topic': {}", paymentEvent);
        } catch (Exception e) {
            log.error("⚠️ [PAYMENT SERVICE -> KAFKA] Lỗi phát sự kiện Kafka: {}", e.getMessage());
        }

        return savedTx;
    }
}
