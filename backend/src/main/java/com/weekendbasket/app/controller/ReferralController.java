package com.weekendbasket.app.controller;

import com.weekendbasket.app.dto.ReferralDto.*;
import com.weekendbasket.app.service.ReferralService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/referrals")
@RequiredArgsConstructor
public class ReferralController {

    private final ReferralService referralService;

    @PostMapping("/apply")
    public ResponseEntity<ReferralResponse> applyReferral(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ApplyReferralRequest request) {
        return ResponseEntity.ok(referralService.applyReferral(
                userDetails.getUsername(), request.referralCode()));
    }

    @GetMapping("/my")
    public ResponseEntity<List<ReferralResponse>> getMyReferrals(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(referralService.getMyReferrals(userDetails.getUsername()));
    }

    @GetMapping("/my-code")
    public ResponseEntity<String> getMyReferralCode(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(referralService.getMyReferralCode(userDetails.getUsername()));
    }
}
