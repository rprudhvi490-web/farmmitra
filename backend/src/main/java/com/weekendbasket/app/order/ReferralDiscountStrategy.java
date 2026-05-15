package com.weekendbasket.app.order;

import com.weekendbasket.app.model.User;
import com.weekendbasket.app.repository.ReferralRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Primary
@RequiredArgsConstructor
public class ReferralDiscountStrategy implements DiscountStrategy {

    private static final BigDecimal REFERRAL_REWARD = new BigDecimal("100.00");

    private final ReferralRepository referralRepository;

    @Override
    public BigDecimal calculate(User user, BigDecimal orderTotal) {
        boolean hasReward = referralRepository
                .findByReferredIdAndStatus(user.getId(), "REWARDED")
                .isPresent();
        return hasReward ? REFERRAL_REWARD : BigDecimal.ZERO;
    }
}
