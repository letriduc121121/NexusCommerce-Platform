package com.ecommerce.product.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Bắt toàn bộ lỗi logic nghiệp vụ và Runtime
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        log.error("❌ Lỗi nghiệp vụ: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
    }

    // Bắt lỗi upload ảnh quá lớn
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        log.warn("⚠️ Kích thước file upload vượt giới hạn: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(Map.of("message", "File ảnh quá lớn! Kích thước tối đa là 20MB."));
    }

    // Bắt lỗi phân quyền
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(org.springframework.security.access.AccessDeniedException ex) {
        log.warn("🚫 Bị từ chối truy cập: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "Bạn không có quyền truy cập chức năng này!"));
    }

    // Bắt lỗi hệ thống chưa xác định khác
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneralException(Exception ex) {
        log.error("💥 Lỗi hệ thống: ", ex);
        return ResponseEntity.internalServerError().body(Map.of("message", "Lỗi hệ thống: " + ex.getMessage()));
    }
}
