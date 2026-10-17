package com.ecommerce.product.repository;

import com.ecommerce.product.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    boolean existsByOrderItemId(UUID orderItemId);

    @Query("SELECT r.orderItemId FROM Review r WHERE r.userId = :userId")
    List<UUID> findReviewedOrderItemIdsByUserId(@Param("userId") UUID userId);

    Optional<Review> findByOrderItemId(UUID orderItemId);

    Page<Review> findByProductIdAndIsHiddenFalse(UUID productId, Pageable pageable);

    Page<Review> findByProductIdAndRatingAndIsHiddenFalse(UUID productId, Integer rating, Pageable pageable);

    @Query("SELECT DISTINCT r FROM Review r LEFT JOIN r.images img WHERE r.productId = :productId AND r.isHidden = false AND img IS NOT NULL")
    Page<Review> findByProductIdWithImagesAndIsHiddenFalse(@Param("productId") UUID productId, Pageable pageable);

    @Query("SELECT r FROM Review r WHERE r.productId = :productId AND r.isHidden = false AND r.comment IS NOT NULL AND LENGTH(TRIM(r.comment)) > 0")
    Page<Review> findByProductIdWithCommentAndIsHiddenFalse(@Param("productId") UUID productId, Pageable pageable);

    List<Review> findByProductIdAndIsHiddenFalse(UUID productId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.productId = :productId AND r.isHidden = false")
    long countValidReviewsByProductId(@Param("productId") UUID productId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.productId = :productId AND r.isHidden = false")
    Double calculateAvgRatingByProductId(@Param("productId") UUID productId);
}
