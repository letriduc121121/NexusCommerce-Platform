package com.ecommerce.order.repository;

import com.ecommerce.order.entity.Order;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Mọi truy vấn đọc đều kèm @EntityGraph để nạp sẵn orderItems trong một lượt.
 * Không có nó thì mỗi đơn sinh thêm một câu SELECT lúc Jackson serialize (N+1).
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    @EntityGraph(attributePaths = "orderItems")
    Optional<Order> findWithItemsById(UUID id);

    @EntityGraph(attributePaths = "orderItems")
    List<Order> findByUserIdOrderByCreatedAtDesc(UUID userId);

    @EntityGraph(attributePaths = "orderItems")
    List<Order> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = "orderItems")
    List<Order> findByStatusOrderByCreatedAtDesc(String status);
}
