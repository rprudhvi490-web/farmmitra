package com.weekendbasket.app.service;

import com.weekendbasket.app.dto.CategoryDto.*;
import com.weekendbasket.app.exception.ResourceNotFoundException;
import com.weekendbasket.app.exception.WeekendBasketException;
import com.weekendbasket.app.model.Category;
import com.weekendbasket.app.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<CategoryResponse> getActiveCategories() {
        return categoryRepository.findByActiveTrueOrderByDisplayOrderAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    public List<CategoryResponse> getAll() {
        return categoryRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public CategoryResponse getById(Long id) {
        return toResponse(find(id));
    }

    @Transactional
    public CategoryResponse create(CreateCategoryRequest request) {
        if (categoryRepository.existsByName(request.name())) {
            throw new WeekendBasketException("Category already exists: " + request.name());
        }
        Category category = Category.builder()
                .name(request.name())
                .imageUrl(request.imageUrl())
                .displayOrder(request.displayOrder())
                .build();
        categoryRepository.save(category);
        return toResponse(category);
    }

    @Transactional
    public CategoryResponse update(Long id, CreateCategoryRequest request) {
        Category category = find(id);
        category.setName(request.name());
        category.setImageUrl(request.imageUrl());
        category.setDisplayOrder(request.displayOrder());
        categoryRepository.save(category);
        return toResponse(category);
    }

    @Transactional
    public void deactivate(Long id) {
        Category category = find(id);
        category.setActive(false);
        categoryRepository.save(category);
    }

    private Category find(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
    }

    private CategoryResponse toResponse(Category c) {
        return new CategoryResponse(c.getId(), c.getName(), c.getImageUrl(), c.getDisplayOrder(), c.getActive());
    }
}
