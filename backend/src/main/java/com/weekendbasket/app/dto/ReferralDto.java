package com.weekendbasket.app.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public class ReferralDto {

    public record ApplyReferralRequest(
            @NotBlank(message = "Referral code is required")
            String referralCode
    ) {}

    public record ReferralResponse(
            Long id,
            Long referrerId,
            String referrerName,
            Long referredId,
            String referredName,
            String referralCode,
            String status
    ) {}
}
