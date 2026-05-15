package com.weekendbasket.app.order;

import com.weekendbasket.app.exception.ResourceNotFoundException;
import com.weekendbasket.app.exception.WeekendBasketException;
import com.weekendbasket.app.repository.UserRepository;
import com.weekendbasket.app.repository.WeeklyCycleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
@RequiredArgsConstructor
public class CycleOpenRule implements OrderPlacementRule {

    private final UserRepository userRepository;
    private final WeeklyCycleRepository cycleRepository;

    @Override
    public void validate(OrderPlacementContext context) {
        context.setUser(userRepository.findByPhoneNumber(context.getPhoneNumber())
                .orElseThrow(() -> new ResourceNotFoundException("User not found")));

        context.setCycle(cycleRepository.findTopByStatusOrderByOrderOpenAtDesc("OPEN")
                .orElseThrow(() -> new WeekendBasketException(
                        "Ordering is currently closed. Next window opens Monday.")));
    }
}
