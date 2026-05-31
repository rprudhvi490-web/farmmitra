package com.weekendbasket.app.service;

import com.weekendbasket.app.dto.ProductDto.*;
import com.weekendbasket.app.exception.ResourceNotFoundException;
import com.weekendbasket.app.model.Category;
import com.weekendbasket.app.model.Product;
import com.weekendbasket.app.repository.CategoryRepository;
import com.weekendbasket.app.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<ProductResponse> getAvailableProducts(Long categoryId, String search) {
        String searchPattern = (search != null && !search.isBlank())
                ? "%" + search + "%" : null;
        return productRepository.findAvailableFilteredEager(categoryId, search, searchPattern)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getAll() {
        return productRepository.findAllWithCategory()
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getByCategory(Long categoryId) {
        return productRepository.findByCategoryIdAndAvailableTrue(categoryId).stream()
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        return toResponse(find(id));
    }

    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.categoryId()));
        Product product = Product.builder()
                .name(request.name())
                .description(request.description())
                .category(category)
                .unit(request.unit())
                .pricePerUnit(request.pricePerUnit())
                .imageUrl(request.imageUrl())
                .minOrderQty(request.minOrderQty())
                .rating(request.rating() != null ? request.rating() : BigDecimal.ZERO)
                .build();
        productRepository.save(product);
        return toResponse(product);
    }

    @Transactional
    public ProductResponse update(Long id, CreateProductRequest request) {
        Product product = find(id);
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.categoryId()));
        product.setName(request.name());
        product.setDescription(request.description());
        product.setCategory(category);
        product.setUnit(request.unit());
        product.setPricePerUnit(request.pricePerUnit());
        product.setImageUrl(request.imageUrl());
        product.setMinOrderQty(request.minOrderQty());
        if (request.rating() != null) product.setRating(request.rating());
        productRepository.save(product);
        return toResponse(product);
    }

    @Transactional
    public void toggleAvailability(Long id) {
        Product product = find(id);
        product.setAvailable(!product.getAvailable());
        productRepository.save(product);
    }

    private Product find(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    private ProductResponse toResponse(Product p) {
        return new ProductResponse(
                p.getId(), p.getName(), p.getDescription(),
                p.getCategory().getId(), p.getCategory().getName(),
                p.getUnit(), p.getPricePerUnit(), p.getImageUrl(),
                p.getAvailable(), p.getMinOrderQty(), p.getRating()
        );
    }
}
