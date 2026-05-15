package com.weekendbasket.app.order;

import com.weekendbasket.app.model.User;

import java.math.BigDecimal;

public interface DiscountStrategy {
    BigDecimal calculate(User user, BigDecimal orderTotal);
}
