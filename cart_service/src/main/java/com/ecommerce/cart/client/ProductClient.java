package com.ecommerce.cart.client;

import com.ecommerce.cart.dto.ProductResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "product-client", url = "${services.product-service.url}")
public interface ProductClient {

    @GetMapping("/api/v1/products/{id}")
    ProductResponse getProductById(@PathVariable("id") UUID id);
}
