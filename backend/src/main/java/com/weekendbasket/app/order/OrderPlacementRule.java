package com.weekendbasket.app.order;

public interface OrderPlacementRule {
    void validate(OrderPlacementContext context);
}
