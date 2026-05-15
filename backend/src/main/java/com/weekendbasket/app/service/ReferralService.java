package com.weekendbasket.app.service;

import com.weekendbasket.app.dto.ReferralDto.*;
import com.weekendbasket.app.event.OrderDeliveredEvent;
import com.weekendbasket.app.exception.ResourceNotFoundException;
import com.weekendbasket.app.exception.WeekendBasketException;
import com.weekendbasket.app.model.Referral;
import com.weekendbasket.app.model.User;
import com.weekendbasket.app.repository.CustomerOrderRepository;
import com.weekendbasket.app.repository.ReferralRepository;
import com.weekendbasket.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReferralService {

    private static final Logger log = LogManager.getLogger(ReferralService.class);

    private final ReferralRepository referralRepository;
    private final UserRepository userRepository;
    private final CustomerOrderRepository orderRepository;

    @Transactional
    public ReferralResponse applyReferral(String phoneNumber, String referralCode) {
        User referred = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (referralRepository.existsByReferredId(referred.getId())) {
            throw new WeekendBasketException("You have already used a referral code.");
        }

        User referrer = userRepository.findByReferralCode(referralCode)
                .orElseThrow(() -> new WeekendBasketException("Invalid referral code."));

        if (referrer.getId().equals(referred.getId())) {
            throw new WeekendBasketException("You cannot use your own referral code.");
        }

        Referral referral = Referral.builder()
                .referrer(referrer)
                .referred(referred)
                .referralCode(referralCode)
                .build();
        referralRepository.save(referral);
        log.info("Referral applied: {} referred by {}", referred.getPhoneNumber(), referrer.getPhoneNumber());
        return toResponse(referral);
    }

    public String getMyReferralCode(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"))
                .getReferralCode();
    }

    public List<ReferralResponse> getMyReferrals(String phoneNumber) {
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return referralRepository.findByReferrerId(user.getId()).stream()
                .map(this::toResponse).toList();
    }

    // When referred user's order is DELIVERED → reward referrer
    @EventListener
    @Async("appTaskExecutor")
    @Transactional
    public void onOrderDelivered(OrderDeliveredEvent event) {
        // Check if this is the referred user's FIRST delivered order
        long deliveredCount = orderRepository.findByUserId(event.userId()).stream()
                .filter(o -> "DELIVERED".equals(o.getStatus())).count();

        if (deliveredCount == 1) {
            referralRepository.findByReferredIdAndStatus(event.userId(), "PENDING")
                    .ifPresent(referral -> {
                        referral.setStatus("REWARDED");
                        referralRepository.save(referral);
                        log.info("Referral rewarded: referrer={}", referral.getReferrer().getId());
                    });
        }
    }

    private ReferralResponse toResponse(Referral r) {
        return new ReferralResponse(
                r.getId(),
                r.getReferrer().getId(), r.getReferrer().getUsername(),
                r.getReferred().getId(), r.getReferred().getUsername(),
                r.getReferralCode(), r.getStatus());
    }
}
