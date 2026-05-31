package com.weekendbasket.app.service;

import com.weekendbasket.app.dto.CycleProductDto.*;
import com.weekendbasket.app.exception.ResourceNotFoundException;
import com.weekendbasket.app.exception.WeekendBasketException;
import com.weekendbasket.app.model.CycleProduct;
import com.weekendbasket.app.model.Product;
import com.weekendbasket.app.model.WeeklyCycle;
import com.weekendbasket.app.repository.CycleProductRepository;
import com.weekendbasket.app.repository.ProductRepository;
import com.weekendbasket.app.repository.WeeklyCycleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CycleProductService {

    private final CycleProductRepository cycleProductRepository;
    private final WeeklyCycleRepository cycleRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<CycleProductResponse> getForCycle(Long cycleId) {
        return cycleProductRepository.findByCycleId(cycleId).stream()
                .map(this::toResponse)
                .toList();
    }

    // Auto-suggest: returns all products with max_stock from the most recent previous cycle (null if none)
    @Transactional(readOnly = true)
    public List<StockSuggestion> getSuggestions() {
        List<Product> allProducts = productRepository.findAllWithCategory();

        // Find the most recent closed cycle that has cycle_product records
        Map<Long, BigDecimal> lastStockMap = cycleRepository
                .findAll().stream()
                .filter(c -> "CLOSED".equals(c.getStatus()))
                .max((a, b) -> a.getOrderOpenAt().compareTo(b.getOrderOpenAt()))
                .map(c -> cycleProductRepository.findByCycleId(c.getId()).stream()
                        .collect(Collectors.toMap(
                                cp -> cp.getProduct().getId(),
                                CycleProduct::getMaxStock)))
                .orElse(Map.of());

        return allProducts.stream()
                .map(p -> new StockSuggestion(
                        p.getId(), p.getName(), p.getUnit(),
                        lastStockMap.get(p.getId())))
                .toList();
    }

    @Transactional
    public List<CycleProductResponse> bulkSetStock(Long cycleId, BulkSetStockRequest request) {
        WeeklyCycle cycle = cycleRepository.findById(cycleId)
                .orElseThrow(() -> new ResourceNotFoundException("Cycle not found: " + cycleId));

        request.items().forEach(item -> {
            Product product = productRepository.findById(item.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + item.productId()));

            CycleProduct cp = cycleProductRepository
                    .findByCycleIdAndProductId(cycleId, item.productId())
                    .orElse(CycleProduct.builder().cycle(cycle).product(product).build());

            cp.setMaxStock(item.maxStock());
            // Reset sold_out if admin increases the limit
            if (cp.getOrderedQty() == null) cp.setOrderedQty(BigDecimal.ZERO);
            if (cp.getOrderedQty().compareTo(cp.getMaxStock()) < 0) {
                cp.setSoldOut(false);
            }
            cycleProductRepository.save(cp);
        });

        return getForCycle(cycleId);
    }

    // Called by ProductAvailableRule — increments ordered_qty, flips sold_out if limit reached
    @Transactional
    public void reserveStock(Long cycleId, Long productId, BigDecimal qty) {
        CycleProduct cp = cycleProductRepository
                .findByCycleIdAndProductIdForUpdate(cycleId, productId)
                .orElseThrow(() -> new WeekendBasketException(
                        "No stock limit configured for this product in the current cycle."));

        if (cp.getSoldOut()) {
            throw new WeekendBasketException("Product is sold out for this cycle.");
        }

        BigDecimal newQty = cp.getOrderedQty().add(qty);
        if (newQty.compareTo(cp.getMaxStock()) > 0) {
            throw new WeekendBasketException(
                    String.format("Only %.2f %s remaining for this product.",
                            cp.getMaxStock().subtract(cp.getOrderedQty()),
                            cp.getProduct().getUnit()));
        }

        cp.setOrderedQty(newQty);
        if (newQty.compareTo(cp.getMaxStock()) == 0) {
            cp.setSoldOut(true);
        }
        cycleProductRepository.save(cp);
    }

    // Called by OrderService.cancelOrder — releases stock back
    @Transactional
    public void releaseStock(Long cycleId, Long productId, BigDecimal qty) {
        cycleProductRepository.findByCycleIdAndProductId(cycleId, productId).ifPresent(cp -> {
            BigDecimal newQty = cp.getOrderedQty().subtract(qty).max(BigDecimal.ZERO);
            cp.setOrderedQty(newQty);
            cp.setSoldOut(false);
            cycleProductRepository.save(cp);
        });
    }

    private CycleProductResponse toResponse(CycleProduct cp) {
        BigDecimal remaining = cp.getMaxStock().subtract(cp.getOrderedQty()).max(BigDecimal.ZERO);
        return new CycleProductResponse(
                cp.getId(),
                cp.getProduct().getId(), cp.getProduct().getName(), cp.getProduct().getUnit(),
                cp.getMaxStock(), cp.getOrderedQty(), remaining, cp.getSoldOut());
    }
}
