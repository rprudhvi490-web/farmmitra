package com.weekendbasket.app.order;

import com.weekendbasket.app.exception.ResourceNotFoundException;
import com.weekendbasket.app.exception.WeekendBasketException;
import com.weekendbasket.app.repository.UserRepository;
import com.weekendbasket.app.repository.WeeklyCycleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Order(1)
@RequiredArgsConstructor
public class CycleOpenRule implements OrderPlacementRule {

    private final UserRepository userRepository;
    private final WeeklyCycleRepository cycleRepository;

    @Value("${order.close.buffer.minutes:5}")
    private int bufferMinutes;

    @Override
    public void validate(OrderPlacementContext context) {
        context.setUser(userRepository.findByPhoneNumber(context.getPhoneNumber())
                .orElseThrow(() -> new ResourceNotFoundException("User not found")));

        context.setCycle(cycleRepository.findTopByStatusOrderByOrderOpenAtDesc("OPEN")
                .orElseThrow(() -> new WeekendBasketException(
                        "Ordering is currently closed. Next window opens Monday.")));

        // Reject orders within the buffer window before close — scheduler fires after this window
        LocalDateTime cutoff = context.getCycle().getOrderCloseAt().minusMinutes(bufferMinutes);
        if (LocalDateTime.now().isAfter(cutoff)) {
            throw new WeekendBasketException(
                    "Ordering has closed. Please wait for the next cycle.");
        }
    }
}
