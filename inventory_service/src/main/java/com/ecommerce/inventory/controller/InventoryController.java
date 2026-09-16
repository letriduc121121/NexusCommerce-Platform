package com.ecommerce.inventory.controller;

import com.ecommerce.inventory.dto.ReserveRequest;
import com.ecommerce.inventory.dto.StockResponse;
import com.ecommerce.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventories")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    public ResponseEntity<List<StockResponse>> getAll() {
        return ResponseEntity.ok(inventoryService.findAll());
    }

    @GetMapping("/{productId}")
    public ResponseEntity<StockResponse> getStock(@PathVariable UUID productId) {
        return ResponseEntity.ok(inventoryService.getStock(productId));
    }

    @PutMapping("/{productId}")
    public ResponseEntity<StockResponse> setQuantity(@PathVariable UUID productId, @RequestParam int quantity) {
        return ResponseEntity.ok(StockResponse.from(inventoryService.setQuantity(productId, quantity)));
    }

    /** Gọi bởi Order Service trước khi lưu đơn hàng - đồng bộ, phải trả lời ngay. */
    @PostMapping("/reserve")
    public ResponseEntity<Void> reserve(@Valid @RequestBody ReserveRequest request) {
        inventoryService.reserve(request.orderId(), request.items());
        return ResponseEntity.ok().build();
    }

    /** Gọi bởi Order Service khi tạo đơn thất bại sau khi đã giữ chỗ thành công. */
    @PostMapping("/release")
    public ResponseEntity<Void> release(@Valid @RequestBody ReserveRequest request) {
        inventoryService.release(request.orderId(), request.items());
        return ResponseEntity.ok().build();
    }
}
