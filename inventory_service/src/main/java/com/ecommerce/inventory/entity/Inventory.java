package com.ecommerce.inventory.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Tồn kho của một sản phẩm.
 * <p>
 * quantity          : số lượng thực tế trong kho
 * reservedQuantity  : số lượng đang được giữ chỗ cho các đơn chưa chốt sổ
 * available         : quantity - reservedQuantity, đây mới là số bán được
 * <p>
 * productName/imageUrl là BẢN SAO từ Product Service (đồng bộ qua product-topic),
 * chỉ dùng để hiển thị. Nguồn gốc thật vẫn nằm ở Product Service.
 */
@Entity
@Table(name = "inventories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "product_id", nullable = false, unique = true)
    private UUID productId;

    /** Bản sao tên/ảnh từ Product Service, đồng bộ qua product-topic - chỉ dùng để hiển thị. */
    @Column(name = "product_name", length = 200)
    private String productName;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 0;

    @Column(name = "reserved_quantity", nullable = false)
    @Builder.Default
    private Integer reservedQuantity = 0;

    /** false khi sản phẩm đã ngừng kinh doanh - giữ lại bản ghi để tra cứu lịch sử */
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    public int getAvailable() {
        return quantity - reservedQuantity;
    }
}
