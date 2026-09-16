package com.ecommerce.promotion.exception;

import org.springframework.http.HttpStatus;

/** Lỗi nghiệp vụ của Promotion: 400 khi dữ liệu sai, 404 khi không tìm thấy, 409 khi xung đột (hết lượt, đã dùng...). */
public class PromotionException extends RuntimeException {

    private final HttpStatus status;

    public PromotionException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public static PromotionException badRequest(String message) {
        return new PromotionException(HttpStatus.BAD_REQUEST, message);
    }

    public static PromotionException notFound(String message) {
        return new PromotionException(HttpStatus.NOT_FOUND, message);
    }

    public static PromotionException conflict(String message) {
        return new PromotionException(HttpStatus.CONFLICT, message);
    }

    public HttpStatus getStatus() {
        return status;
    }
}
