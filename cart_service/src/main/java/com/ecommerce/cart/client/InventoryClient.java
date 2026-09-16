package com.ecommerce.cart.client;

import com.ecommerce.cart.dto.StockResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "inventory-client", url = "${services.inventory-service.url}")
public interface InventoryClient {

    @GetMapping("/api/v1/inventories/{productId}")
    StockResponse getStock(@PathVariable("productId") UUID productId);
}
