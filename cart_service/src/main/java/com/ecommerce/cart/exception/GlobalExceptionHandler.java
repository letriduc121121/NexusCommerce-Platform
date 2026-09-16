package com.ecommerce.cart.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Feign ném lỗi thẳng ra khi Product/Inventory trả về 4xx/5xx. Bắt ở một chỗ duy nhất
 * và lấy đúng câu tiếng Việt gốc từ response của service kia, thay vì mỗi hàm trong
 * CartService phải tự try/catch.
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final ObjectMapper objectMapper;

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<Map<String, String>> handleFeign(FeignException ex) {
        String message = extractMessage(ex.contentUTF8());
        HttpStatus status = HttpStatus.resolve(ex.status());
        log.warn("⚠️ [CART] Dịch vụ liên quan từ chối yêu cầu: {}", message);
        return ResponseEntity.status(status != null ? status : HttpStatus.BAD_GATEWAY)
                .body(Map.of("message", message));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(e -> e.getDefaultMessage())
                .orElse("Dữ liệu gửi lên không hợp lệ");
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntime(RuntimeException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
    }

    private String extractMessage(String body) {
        try {
            Map<?, ?> parsed = objectMapper.readValue(body, Map.class);
            Object msg = parsed.get("message");
            return msg != null ? msg.toString() : "Yêu cầu bị từ chối";
        } catch (Exception e) {
            return "Không thể kết nối tới dịch vụ liên quan, vui lòng thử lại!";
        }
    }
}
