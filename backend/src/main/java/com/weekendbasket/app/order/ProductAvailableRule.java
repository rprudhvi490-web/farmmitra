package com.weekendbasket.app.order;

import com.weekendbasket.app.exception.ResourceNotFoundException;
import com.weekendbasket.app.exception.WeekendBasketException;
import com.weekendbasket.app.model.Product;
import com.weekendbasket.app.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(3)
@RequiredArgsConstructor
public class ProductAvailableRule implements OrderPlacementRule {

    private final ProductRepository productRepository;

    @Override
    public void validate(OrderPlacementContext context) {
        context.getRequest().items().forEach(item -> {
            Product product = productRepository.findById(item.productId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Product not found: " + item.productId()));
            if (!product.getAvailable()) {
                throw new WeekendBasketException("Product not available: " + product.getName());
            }
            // Store resolved product on context — Rule 4 reuses this, no re-fetch
            context.getResolvedProducts().put(product.getId(), product);
        });
    }
}
