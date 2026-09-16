package com.ecommerce.promotion.repository;

import com.ecommerce.promotion.entity.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VoucherRepository extends JpaRepository<Voucher, Long> {
    // tìm kiếm id vocuher
    Optional<Voucher> findByCodeIgnoreCase(String code);

    //check voucher exist
    boolean existsByCodeIgnoreCase(String code);

    /** Voucher đang phát: đang chạy, còn hạn, còn lượt. Dùng cho danh sách mã ở trang chủ. */
    @Query("""
        SELECT v FROM Voucher v
        WHERE v.status = com.ecommerce.promotion.enums.VoucherStatus.ACTIVE
          AND CURRENT_TIMESTAMP BETWEEN v.usableFrom AND v.usableTo
          AND (v.totalQuantity IS NULL OR v.usedCount < v.totalQuantity)
        ORDER BY v.usableTo ASC
        """)
    List<Voucher> findAvailable();

    //
    @Modifying
    @Query("""
        UPDATE Voucher v SET v.usedCount = v.usedCount + 1
        WHERE v.id = :id
          AND v.status = com.ecommerce.promotion.enums.VoucherStatus.ACTIVE
          AND CURRENT_TIMESTAMP BETWEEN v.usableFrom AND v.usableTo
          AND (v.totalQuantity IS NULL OR v.usedCount < v.totalQuantity)
        """)
    int consume(@Param("id") Long id);

    /** Hoàn 1 lượt khi đơn bị huỷ. */
    @Modifying
    @Query("UPDATE Voucher v SET v.usedCount = v.usedCount - 1 WHERE v.id = :id AND v.usedCount > 0")
    int refund(@Param("id") Long id);
}
