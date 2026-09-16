package com.ecommerce.cart.service;

import com.ecommerce.cart.client.InventoryClient;
import com.ecommerce.cart.client.ProductClient;
import com.ecommerce.cart.dto.AddToCartRequest;
import com.ecommerce.cart.dto.CartItem;
import com.ecommerce.cart.dto.CartResponse;
import com.ecommerce.cart.dto.ProductResponse;
import com.ecommerce.cart.dto.StockResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Giỏ hàng lưu trong Redis (HASH "cart:{userId}"), không có database riêng.
 * <p>
 * Kiểm tra tồn kho qua REST (Feign) sang Inventory Service, vì đây là câu hỏi cần
 * đáp án ngay để quyết định thêm được hay không - Kafka không làm được việc này
 * (bắn đi là xong, không có đường nhận câu trả lời).
 * <p>
 * Feign ném lỗi thẳng ra khi Product/Inventory từ chối (hết hàng, không tồn tại...),
 * GlobalExceptionHandler bắt ở một chỗ duy nhất và trả về đúng câu tiếng Việt gốc.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    private static final Duration TTL = Duration.ofDays(30);

    private final RedisTemplate<String, Object> redisTemplate;
    private final ProductClient productClient;
    private final InventoryClient inventoryClient;

    private String cartKey(String userId) {
        return "cart:" + userId;
    }

    public CartResponse getCart(String userId) {
        Map<Object, Object> raw = redisTemplate.opsForHash().entries(cartKey(userId));

        List<CartItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (Object obj : raw.values()) {
            CartItem item = (CartItem) obj;
            applyStock(item);
            item.setSubtotal(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            total = total.add(item.getSubtotal());
            items.add(item);
        }

        return CartResponse.builder().userId(userId).items(items).totalAmount(total).build();
    }

    public CartResponse addToCart(String userId, AddToCartRequest request) {
        addOrIncrement(cartKey(userId), request.getProductId(), request.getQuantity());
        return getCart(userId);
    }

    /**
     * Gộp giỏ hàng khách vãng lai (gửi từ localStorage sau khi đăng nhập) vào giỏ Redis của user.
     * Cộng dồn số lượng vào sản phẩm đã có. Sản phẩm hết hàng / không tồn tại thì bỏ qua,
     * không làm hỏng cả lượt gộp.
     */
    public CartResponse merge(String userId, List<AddToCartRequest> guestItems) {
        if (guestItems == null || guestItems.isEmpty()) {
            return getCart(userId);
        }

        String key = cartKey(userId);
        int merged = 0;
        int skipped = 0;

        for (AddToCartRequest item : guestItems) {
            try {
                addOrIncrement(key, item.getProductId(), item.getQuantity());
                merged++;
            } catch (Exception e) {
                skipped++;
                log.warn("⚠️ [MERGE] Bỏ qua sản phẩm {}: {}", item.getProductId(), e.getMessage());
            }
        }

        log.info("🔀 [MERGE] User {}: gộp {} sản phẩm, bỏ {}", userId, merged, skipped);
        return getCart(userId);
    }

    public CartResponse updateQuantity(String userId, UUID productId, int quantity) {
        if (quantity <= 0) return removeItem(userId, productId);

        String key = cartKey(userId);
        String field = productId.toString();
        CartItem item = getItem(key, field);
        if (item == null) {
            throw new RuntimeException("Sản phẩm không có trong giỏ hàng!");
        }

        ensureEnough(inventoryClient.getStock(productId), quantity);

        item.setQuantity(quantity);
        saveItem(key, field, item);

        return getCart(userId);
    }

    public CartResponse removeItem(String userId, UUID productId) {
        redisTemplate.opsForHash().delete(cartKey(userId), productId.toString());
        return getCart(userId);
    }

    public void clearCart(String userId) {
        redisTemplate.delete(cartKey(userId));
        log.info("🗑️ [CART] Đã xoá giỏ hàng của user {}", userId);
    }

    // ==================== HỖ TRỢ ====================

    /** Thêm / cộng dồn 1 sản phẩm vào giỏ. Ném lỗi nếu hết hàng hoặc sản phẩm không tồn tại. */
    private void addOrIncrement(String key, UUID productId, int addQuantity) {
        String field = productId.toString();

        CartItem existing = getItem(key, field);
        int newQuantity = (existing != null ? existing.getQuantity() : 0) + addQuantity;

        // Kiểm tra tồn kho trước - đủ mới gọi tiếp sang Product để lấy tên/giá/ảnh.
        ensureEnough(inventoryClient.getStock(productId), newQuantity);
        ProductResponse product = productClient.getProductById(productId);

        CartItem item = CartItem.builder()
                .productId(productId)
                .productName(product.getName())
                .imageUrl(product.getImageUrl())
                .price(product.getPrice())
                .quantity(newQuantity)
                .build();

        saveItem(key, field, item);
        log.info("🛒 [CART] {} x{} trong {}", product.getName(), newQuantity, key);
    }

    private CartItem getItem(String key, String field) {
        return (CartItem) redisTemplate.opsForHash().get(key, field);
    }

    private void saveItem(String key, String field, CartItem item) {
        redisTemplate.opsForHash().put(key, field, item);
        redisTemplate.expire(key, TTL);
    }

    /** Thêm mới / sửa số lượng: hết hàng hoặc ngừng bán thì chặn thẳng, không cho lưu. */
    private void ensureEnough(StockResponse stock, int quantity) {
        if (!stock.isActive()) {
            throw new RuntimeException("Sản phẩm \"" + stock.getProductName() + "\" đã ngừng kinh doanh!");
        }
        if (stock.getAvailable() < quantity) {
            throw new RuntimeException("Sản phẩm \"" + stock.getProductName()
                    + "\" chỉ còn " + stock.getAvailable() + " sản phẩm!");
        }
    }

    /** Xem giỏ: một sản phẩm tra cứu lỗi không được làm vỡ cả trang - coi như hết hàng. */
    private void applyStock(CartItem item) {
        try {
            StockResponse stock = inventoryClient.getStock(item.getProductId());
            item.setAvailable(stock.getAvailable());
            item.setEnough(stock.isActive() && stock.getAvailable() >= item.getQuantity());
        } catch (Exception e) {
            item.setAvailable(0);
            item.setEnough(false);
        }
    }
}
