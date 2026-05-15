package com.weekendbasket.app.order;

import com.weekendbasket.app.model.User;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

// Default strategy — returns zero. Phase 7 ReferralDiscountStrategy overrides with @Primary.
@Component
public class NoDiscountStrategy implements DiscountStrategy {

    @Override
    public BigDecimal calculate(User user, BigDecimal orderTotal) {
        return BigDecimal.ZERO;
    }
}
