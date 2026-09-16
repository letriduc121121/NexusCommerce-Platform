package com.ecommerce.inventory.exception;

import org.springframework.http.HttpStatus;

/** Lỗi nghiệp vụ của Inventory: 404 khi chưa có bản ghi kho, 409 khi không đủ hàng. */
public class InventoryException extends RuntimeException {

    private final HttpStatus status;

    public InventoryException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public static InventoryException notFound(String message) {
        return new InventoryException(HttpStatus.NOT_FOUND, message);
    }

    public static InventoryException outOfStock(String message) {
        return new InventoryException(HttpStatus.CONFLICT, message);
    }

    public HttpStatus getStatus() {
        return status;
    }
}
