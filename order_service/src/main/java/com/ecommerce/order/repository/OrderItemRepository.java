package com.ecommerce.order.repository;

import com.ecommerce.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    @Query("SELECT oi FROM OrderItem oi JOIN FETCH oi.order WHERE oi.id = :id AND oi.order.userId = :userId")
    Optional<OrderItem> findByIdAndOrderUserId(@Param("id") UUID id, @Param("userId") UUID userId);
}
