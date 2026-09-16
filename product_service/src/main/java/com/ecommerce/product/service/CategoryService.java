package com.ecommerce.product.service;

import com.ecommerce.product.entity.Category;
import com.ecommerce.product.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Cacheable(value = "categories", key = "'all'")
    public List<Category> getAllCategories() {
        log.info("🏷️ [CATEGORY SERVICE] Lấy danh sách tất cả danh mục");
        return categoryRepository.findAll();
    }

    @Cacheable(value = "category", key = "#id")
    public Category getCategoryById(Integer id) {
        log.info("🔍 [CATEGORY SERVICE] Truy vấn danh mục ID: {}", id);
        return categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục với ID: " + id));
    }

    @CacheEvict(value = {"categories", "category"}, allEntries = true)
    public Category createCategory(Category category) {
        log.info("✨ [CATEGORY SERVICE] Tạo danh mục mới: {}", category.getName());
        return categoryRepository.save(category);
    }

    @CacheEvict(value = {"categories", "category"}, allEntries = true)
    public Category updateCategory(Integer id, Category request) {
        log.info("📝 [CATEGORY SERVICE] Cập nhật danh mục ID: {}", id);
        Category existingCategory = getCategoryById(id);
        if (request.getName() != null) existingCategory.setName(request.getName());
        if (request.getDescription() != null) existingCategory.setDescription(request.getDescription());
        return categoryRepository.save(existingCategory);
    }

    @CacheEvict(value = {"categories", "category"}, allEntries = true)
    public void deleteCategory(Integer id) {
        log.info("🗑️ [CATEGORY SERVICE] Xóa danh mục ID: {}", id);
        Category category = getCategoryById(id);
        categoryRepository.delete(category);
    }
}
