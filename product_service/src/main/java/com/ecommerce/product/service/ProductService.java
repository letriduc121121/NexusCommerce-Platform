package com.ecommerce.product.service;

import com.ecommerce.product.entity.Product;
import com.ecommerce.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private static final String DISCONTINUED = "DISCONTINUED";

    private final ProductRepository productRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Cacheable(value = "products", key = "'all'")
    public List<Product> getAllProducts() {
        return productRepository.findByStatusNot(DISCONTINUED);
    }

    @Cacheable(value = "product", key = "#id")
    public Product getProductById(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + id));
    }

    public List<Product> getProductsByCategory(Integer categoryId) {
        return productRepository.findByCategoryId(categoryId);
    }

    public List<Product> searchProducts(String query) {
        return productRepository.findByNameContainingIgnoreCase(query);
    }

    @CacheEvict(value = {"products", "product"}, allEntries = true)
    public Product createProduct(Product product) {
        Product saved = productRepository.save(product);
        publishEvent("PRODUCT_CREATED", saved);
        return saved;
    }

    @CacheEvict(value = {"products", "product"}, allEntries = true)
    public Product updateProduct(UUID id, Product request) {
        Product existing = getProductById(id);

        setIfPresent(request.getName(), existing::setName);
        setIfPresent(request.getSku(), existing::setSku);
        setIfPresent(request.getDescription(), existing::setDescription);
        setIfPresent(request.getPrice(), existing::setPrice);
        setIfPresent(request.getCategory(), existing::setCategory);
        setIfPresent(request.getImageUrl(), existing::setImageUrl);
        setIfPresent(request.getStatus(), existing::setStatus);

        Product saved = productRepository.save(existing);
        publishEvent("PRODUCT_UPDATED", saved);
        return saved;
    }

    /** Xoá mềm: chỉ đổi trạng thái, không xoá bản ghi - đơn hàng cũ vẫn tra cứu được sản phẩm. */
    @CacheEvict(value = {"products", "product"}, allEntries = true)
    public void deleteProduct(UUID id) {
        Product product = getProductById(id);
        product.setStatus(DISCONTINUED);
        publishEvent("PRODUCT_DISCONTINUED", productRepository.save(product));
    }

    public List<Product> getProductsByIds(List<UUID> ids) {
        return productRepository.findAllById(ids);
    }

    /** Công bố lại toàn bộ catalog để Inventory Service tự tạo/cập nhật bản ghi kho còn thiếu. */
    public void republishCatalog() {
        List<Product> products = productRepository.findAll();

        for (Product p : products) {
            publishEvent("PRODUCT_CREATED", p);
            // Cùng productId nên cùng partition -> Kafka đảm bảo event này tới sau, kho bị khoá đúng.
            if (DISCONTINUED.equals(p.getStatus())) {
                publishEvent("PRODUCT_DISCONTINUED", p);
            }
        }
        log.info("Đã công bố lại {} sản phẩm để đồng bộ tồn kho.", products.size());
    }

    /** Có giá trị mới thì mới gán - giữ đúng kiểu "cập nhật từng phần" mà không cần lặp lại if != null. */
    private static <T> void setIfPresent(T value, Consumer<T> setter) {
        if (value != null) setter.accept(value);
    }

    private void publishEvent(String eventType, Product product) {
        // Dùng HashMap thay Map.of vì imageUrl có thể null (Map.of không nhận null).
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", eventType);
        event.put("productId", product.getId());
        event.put("name", product.getName());
        event.put("imageUrl", product.getImageUrl());

        try {
            kafkaTemplate.send("product-topic", product.getId().toString(), event);
        } catch (Exception e) {
            log.error("⚠️ [PRODUCT SERVICE] Lỗi phát sự kiện Kafka {}: {}", eventType, e.getMessage());
        }
    }
}
