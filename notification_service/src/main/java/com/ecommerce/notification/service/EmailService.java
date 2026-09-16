package com.ecommerce.notification.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Dựng nội dung HTML từ template rồi gửi qua SMTP.
 * <p>
 * Hai loại mail dùng chung một template, chỉ khác tiêu đề - câu dẫn - màu nhấn.
 * Chưa khai báo MAIL_USERNAME thì in ra log thay vì gửi thật, để demo được ngay.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${spring.mail.username:}")
    private String from;


    @Value("${notification.mail.from-name:Ecommerce Shop}")
    private String fromName;

    // ==================== 2 LOẠI MAIL ====================

    public void sendOrderCreated(Map<String, Object> event) {
        send(event, "Đặt hàng thành công!",
                "Cảm ơn bạn đã đặt hàng. Đơn hàng đang được xử lý và sẽ sớm được giao đến bạn.",
                "#111827");
    }

    public void sendOrderDelivered(Map<String, Object> event) {
        send(event, "Giao hàng thành công!",
                "Đơn hàng đã được giao đến bạn. Cảm ơn bạn đã mua sắm tại cửa hàng!",
                "#059669");
    }

    public void sendOrderCancelled(Map<String, Object> event) {
        send(event, "Đơn hàng đã hủy",
                "Đơn hàng của bạn đã được hủy. Nếu bạn đã thanh toán, tiền sẽ được hoàn lại trong 3-5 ngày làm việc.",
                "#dc2626");
    }

    // ==================== 3 BƯỚC CHÍNH ====================

    /** Bước 1: kiểm tra, dựng nội dung, rồi quyết định gửi thật hay in ra log. */
    private void send(Map<String, Object> event, String heading, String message, String color) {
        String to = text(event.get("customerEmail"), "");
        if (to.isBlank()) {
            log.warn("⚠️ [NOTIFICATION] Đơn {} không có email khách hàng -> bỏ qua.", event.get("orderId"));
            return;
        }

        String subject = heading + " - Đơn hàng #" + shortId(event.get("orderId"));
        String html = buildHtml(event, heading, message, color);

        if (from == null || from.isBlank()) {
            log.info("✉️ [CHẾ ĐỘ THỬ] Chưa cấu hình MAIL_USERNAME nên không gửi thật."
                    + "\nNgười nhận: {}\nTiêu đề   : {}\n{}", to, subject, html);
            return;
        }

        deliver(to, subject, html);
    }

    /** Bước 2: đổ dữ liệu vào template order-email.html để lấy ra chuỗi HTML. */
    private String buildHtml(Map<String, Object> event, String heading, String message, String color) {
        Context ctx = new Context();
        ctx.setVariable("heading", heading);
        ctx.setVariable("message", message);
        ctx.setVariable("accentColor", color);
        ctx.setVariable("orderId", shortId(event.get("orderId")));
        ctx.setVariable("customerName", text(event.get("customerName"), "Quý khách"));
        ctx.setVariable("shippingAddress", text(event.get("shippingAddress"), "-"));
        ctx.setVariable("paymentMethod", paymentLabel(event.get("paymentMethod")));
        ctx.setVariable("totalAmount", money(event.get("totalAmount")));
        ctx.setVariable("items", toRows(event.get("items")));

        return templateEngine.process("order-email", ctx);
    }

    /** Bước 3: gửi qua SMTP. Hỏng thì ghi log, không ném lỗi ra ngoài. */
    private void deliver(String to, String subject, String html) {
        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8"); // UTF-8: giữ dấu tiếng Việt
            helper.setFrom(from, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true); // true = nội dung HTML
            mailSender.send(mime);

            log.info("✅ [NOTIFICATION] Đã gửi mail \"{}\" tới {}", subject, to);
        } catch (Exception e) {
            log.error("❌ [NOTIFICATION] Gửi mail tới {} thất bại: {}", to, e.getMessage());
        }
    }

    // ==================== HÀM PHỤ ====================

    /** Danh sách sản phẩm thô trong event -> dữ liệu đã định dạng sẵn cho template. */
    private List<Map<String, String>> toRows(Object rawItems) {
        List<Map<String, String>> rows = new ArrayList<>();
        if (!(rawItems instanceof List<?> list)) return rows;

        for (Object element : list) {
            if (!(element instanceof Map<?, ?> item)) continue;

            Map<String, String> row = new HashMap<>();
            row.put("productName", text(item.get("productName"), "Sản phẩm"));
            row.put("quantity", text(item.get("quantity"), "0"));
            row.put("price", money(item.get("price")));
            row.put("subtotal", money(item.get("subtotal")));
            rows.add(row);
        }
        return rows;
    }

    /** 250000 -> "250.000 ₫". Ép Locale.US để dấu phân nhóm luôn là dấu phẩy, rồi đổi thành dấu chấm. */
    private String money(Object value) {
        if (value == null) return "0 ₫";
        long amount = new BigDecimal(value.toString()).longValue();
        return String.format(Locale.US, "%,d", amount).replace(',', '.') + " ₫";
    }

    private String paymentLabel(Object method) {
        if ("VNPAY".equalsIgnoreCase(text(method, ""))) return "Thanh toán online qua VNPay";
        return "Thanh toán khi nhận hàng (COD)";
    }

    /** Rút gọn UUID cho dễ đọc: "3f2a9c81-...." -> "3F2A9C81". */
    private String shortId(Object orderId) {
        String id = text(orderId, "");
        return id.length() <= 8 ? id : id.substring(0, 8).toUpperCase();
    }

    /** Null hoặc rỗng thì trả về giá trị dự phòng, nhờ vậy chỗ khác không phải kiểm tra null. */
    private String text(Object value, String fallback) {
        if (value == null) return fallback;
        String result = value.toString();
        return result.isBlank() ? fallback : result;
    }
}
