package com.weekendbasket.app.controller;

import java.time.LocalDate;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.weekendbasket.app.dto.AuthDto.AuthResponse;
import com.weekendbasket.app.dto.AuthDto.FirebaseLoginRequest;
import com.weekendbasket.app.dto.AuthDto.LoginRequest;
import com.weekendbasket.app.dto.AuthDto.SetPasswordRequest;
import com.weekendbasket.app.model.OtpTracker;
import com.weekendbasket.app.repository.OtpTrackerRepository;
import com.weekendbasket.app.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    private final OtpTrackerRepository trackerRepository;
    
    @PostMapping("/firebase-login")
    public ResponseEntity<AuthResponse> firebaseLogin(@Valid @RequestBody FirebaseLoginRequest request) {
        return ResponseEntity.ok(authService.firebaseLogin(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PutMapping("/set-password")
    public ResponseEntity<Void> setPassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody SetPasswordRequest request) {
        authService.setPassword(userDetails.getUsername(), request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authHeader) {
        authService.logout(authHeader.substring(7));
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/check-quota")
    public ResponseEntity<?> checkAndIncrementOtpQuota() {
        LocalDate today = LocalDate.now();
        
        // Find today's log or instantiate a brand new entry if it's a new day
        OtpTracker tracker = trackerRepository.findById(today)
                .orElse(new OtpTracker(today, 0));

        // Enforce the strict 9 request safety limit
        if (tracker.getRequestCount() >= 9) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of(
                        "status", "BLOCKED", 
                        "message", "Daily application limit reached. Try tomorrow!"
                    ));
        }

        // Increment and save seamlessly back to Neon Postgres
        tracker.setRequestCount(tracker.getRequestCount() + 1);
        trackerRepository.save(tracker);

        return ResponseEntity.ok(Map.of(
            "status", "ALLOWED", 
            "currentCount", tracker.getRequestCount()
        ));
    }
}
