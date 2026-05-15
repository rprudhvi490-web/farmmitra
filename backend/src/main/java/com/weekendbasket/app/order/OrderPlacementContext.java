package com.weekendbasket.app.order;

import com.weekendbasket.app.dto.OrderDto.PlaceOrderRequest;
import com.weekendbasket.app.model.Product;
import com.weekendbasket.app.model.User;
import com.weekendbasket.app.model.WeeklyCycle;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class OrderPlacementContext {

    // Input — set before chain starts
    private final String phoneNumber;
    private final PlaceOrderRequest request;

    // Enriched by rules — each rule sets what it resolves
    private User user;                          // set by CycleOpenRule (fetches user too)
    private WeeklyCycle cycle;                  // set by CycleOpenRule
    private Map<Long, Product> resolvedProducts; // set by ProductAvailableRule, reused by MinQtyRule

    public OrderPlacementContext(String phoneNumber, PlaceOrderRequest request) {
        this.phoneNumber = phoneNumber;
        this.request = request;
        this.resolvedProducts = new HashMap<>();
    }
}
