package com.ecommerce.inventory.service;

import com.ecommerce.inventory.dto.StockItem;
import com.ecommerce.inventory.dto.StockResponse;
import com.ecommerce.inventory.entity.Inventory;
import com.ecommerce.inventory.exception.InventoryException;
import com.ecommerce.inventory.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private static final String LOCK_PREFIX = "lock:inventory:";
    private static final Duration LOCK_TTL = Duration.ofSeconds(5);

    private final InventoryRepository repository;
    private final StringRedisTemplate redis;

    // ==================== ĐỌC ====================

    public List<StockResponse> findAll() {
        return repository.findAll().stream().map(StockResponse::from).toList();
    }

    public StockResponse getStock(UUID productId) {
        return StockResponse.from(findOrThrow(productId));
    }

    // ==================== QUẢN TRỊ ====================

    /**
     * Đồng bộ thông tin sản phẩm từ product-topic: chưa có thì tạo mới với tồn kho 0,
     * có rồi thì cập nhật tên/ảnh. Dùng chung cho PRODUCT_CREATED và PRODUCT_UPDATED,
     * nên gọi lại nhiều lần cũng không sinh bản ghi trùng.
     */
    @Transactional
    public void syncProduct(UUID productId, String productName, String imageUrl) {
        Inventory inventory = repository.findByProductId(productId)
                .orElseGet(() -> Inventory.builder().productId(productId).build());

        inventory.setProductName(productName);
        inventory.setImageUrl(imageUrl);
        repository.save(inventory);
        log.info("📦 Đồng bộ sản phẩm {} ({}) vào kho", productName, productId);
    }

    @Transactional
    public Inventory setQuantity(UUID productId, int quantity) {
        Inventory inventory = findOrThrow(productId);
        inventory.setQuantity(quantity);
        return repository.save(inventory);
    }

    /** Ngừng kinh doanh: không cho giữ chỗ nữa nhưng vẫn giữ bản ghi để tra cứu lịch sử. */
    @Transactional
    public void deactivate(UUID productId) {
        repository.findByProductId(productId).ifPresent(inventory -> {
            inventory.setActive(false);
            repository.save(inventory);
        });
    }

    // ==================== GIỮ CHỖ (đồng bộ, Order Service gọi trước khi lưu đơn) ====================

    /** Được hết hoặc không được gì: một sản phẩm thiếu hàng là toàn bộ transaction rollback. */
    @Transactional
    public void reserve(UUID orderId, List<StockItem> items) {
        List<String> locked = new ArrayList<>();
        try {
            for (StockItem item : sortByProductId(items)) {
                String key = LOCK_PREFIX + item.productId();
                if (!Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key, "1", LOCK_TTL))) {
                    throw InventoryException.outOfStock("Sản phẩm đang có người khác đặt, vui lòng thử lại!");
                }
                locked.add(key);

                if (repository.reserve(item.productId(), item.quantity()) == 0) {
                    throw outOfStock(item);
                }
            }
            log.info("🔒 Giữ chỗ thành công cho đơn hàng {}", orderId);
        } finally {
            locked.forEach(redis::delete);
        }
    }

    /** Nhả chỗ đã giữ khi Order Service không tạo được đơn. */
    @Transactional
    public void release(UUID orderId, List<StockItem> items) {
        items.forEach(item -> repository.release(item.productId(), item.quantity()));
        log.info("🔓 Đã nhả chỗ giữ của đơn hàng {}", orderId);
    }

    // ==================== CHỐT SỔ / HOÀN KHO (bất đồng bộ qua Kafka) ====================

    /** Trừ kho thật sau khi đơn đã tạo thành công. Đã reserve trước nên hầu như luôn thành công. */
    @Transactional
    public void commit(String orderId, List<StockItem> items) {
        for (StockItem item : items) {
            if (repository.commit(item.productId(), item.quantity()) == 0) throw outOfStock(item);
        }
        log.info("✅ Chốt sổ trừ kho cho đơn hàng {}", orderId);
    }

    /** Hoàn kho khi đơn đã chốt sổ bị hủy. */
    @Transactional
    public void restock(String orderId, List<StockItem> items) {
        for (StockItem item : items) {
            if (repository.restock(item.productId(), item.quantity()) == 0) {
                throw InventoryException.notFound("Sản phẩm chưa có bản ghi kho: " + item.productId());
            }
        }
        log.info("↩️ Hoàn kho cho đơn hàng {}", orderId);
    }

    // ==================== HỖ TRỢ ====================

    private Inventory findOrThrow(UUID productId) {
        return repository.findByProductId(productId)
                .orElseThrow(() -> InventoryException.notFound("Sản phẩm chưa được khởi tạo kho: " + productId));
    }

    private InventoryException outOfStock(StockItem item) {
        Inventory inventory = findOrThrow(item.productId());
        // Tên có thể chưa kịp đồng bộ từ product-topic -> lùi về productId cho khỏi hiện "null".
        String name = inventory.getProductName() != null ? inventory.getProductName() : item.productId().toString();

        return InventoryException.outOfStock(inventory.getActive()
                ? "Sản phẩm \"%s\" không đủ hàng!".formatted(name)
                : "Sản phẩm \"%s\" đã ngừng kinh doanh!".formatted(name));
    }

    private List<StockItem> sortByProductId(List<StockItem> items) {
        return items.stream().sorted(Comparator.comparing(StockItem::productId)).toList();
    }
}
