package com.ecommerce.product.repository;

import com.ecommerce.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {
    Optional<Product> findBySku(String sku);
    List<Product> findByNameContainingIgnoreCase(String name);
    List<Product> findByCategoryId(Integer categoryId);
    List<Product> findByStatusNot(String status);
}
