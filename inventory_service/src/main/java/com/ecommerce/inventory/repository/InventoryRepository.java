package com.ecommerce.inventory.repository;

import com.ecommerce.inventory.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Mọi thao tác thay đổi số lượng đều là UPDATE có điều kiện và trả về số dòng bị ảnh hưởng.
 * Trả về 0 = không đủ điều kiện = thất bại. Đây là lớp chống oversell ở mức database.
 */
@Repository
public interface InventoryRepository extends JpaRepository<Inventory, UUID> {

    Optional<Inventory> findByProductId(UUID productId);

    /** Giữ chỗ: chỉ giữ được khi số bán được (quantity - reserved) còn đủ. */
    @Modifying
    @Query("""
            UPDATE Inventory i SET i.reservedQuantity = i.reservedQuantity + :qty
            WHERE i.productId = :productId
              AND i.active = true
              AND i.quantity - i.reservedQuantity >= :qty
            """)
    int reserve(@Param("productId") UUID productId, @Param("qty") int qty);

    /** Nhả chỗ đã giữ (khi Order tạo đơn thất bại). */
    @Modifying
    @Query("""
            UPDATE Inventory i SET i.reservedQuantity = i.reservedQuantity - :qty
            WHERE i.productId = :productId AND i.reservedQuantity >= :qty
            """)
    int release(@Param("productId") UUID productId, @Param("qty") int qty);

    /** Chốt sổ: trừ kho thật và nhả chỗ giữ, thực hiện trong cùng một câu lệnh. */
    @Modifying
    @Query("""
            UPDATE Inventory i SET i.quantity = i.quantity - :qty,
                                  i.reservedQuantity = i.reservedQuantity - :qty
            WHERE i.productId = :productId
              AND i.quantity >= :qty
              AND i.reservedQuantity >= :qty
            """)
    int commit(@Param("productId") UUID productId, @Param("qty") int qty);

    /** Hoàn kho khi đơn đã chốt sổ bị hủy. */
    @Modifying
    @Query("UPDATE Inventory i SET i.quantity = i.quantity + :qty WHERE i.productId = :productId")
    int restock(@Param("productId") UUID productId, @Param("qty") int qty);
}
