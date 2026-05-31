package com.weekendbasket.app.order;

import com.weekendbasket.app.exception.ResourceNotFoundException;
import com.weekendbasket.app.exception.WeekendBasketException;
import com.weekendbasket.app.model.Product;
import com.weekendbasket.app.repository.ProductRepository;
import com.weekendbasket.app.service.CycleProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(3)
@RequiredArgsConstructor
public class ProductAvailableRule implements OrderPlacementRule {

    private final ProductRepository productRepository;
    private final CycleProductService cycleProductService;

    @Override
    public void validate(OrderPlacementContext context) {
        Long cycleId = context.getCycle().getId();

        context.getRequest().items().forEach(item -> {
            Product product = productRepository.findById(item.productId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Product not found: " + item.productId()));
            if (!product.getAvailable()) {
                throw new WeekendBasketException("Product not available: " + product.getName());
            }
            // Reserve stock — acquires row-level lock, checks limit, increments ordered_qty
            cycleProductService.reserveStock(cycleId, product.getId(), item.quantity());

            context.getResolvedProducts().put(product.getId(), product);
        });
    }
}
