package com.weekendbasket.app.order;

import com.weekendbasket.app.exception.WeekendBasketException;
import com.weekendbasket.app.repository.CustomerOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
@RequiredArgsConstructor
public class DuplicateOrderRule implements OrderPlacementRule {

    private final CustomerOrderRepository orderRepository;

    @Override
    public void validate(OrderPlacementContext context) {
        boolean exists = orderRepository.existsByUserIdAndCycleId(
                context.getUser().getId(),
                context.getCycle().getId());
        if (exists) {
            throw new WeekendBasketException("You have already placed an order for this week.");
        }
    }
}
