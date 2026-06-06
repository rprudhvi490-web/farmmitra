package com.weekendbasket.app.service;

import com.weekendbasket.app.dto.AuthDto.*;
import com.weekendbasket.app.exception.WeekendBasketException;
import com.weekendbasket.app.model.*;
import com.weekendbasket.app.repository.*;
import com.weekendbasket.app.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LogManager.getLogger(AuthService.class);

    private final OtpService otpService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;
    private final RoleRepository roleRepository;
    private final RoleAccessRepository roleAccessRepository;
    private final InvalidatedTokenRepository invalidatedTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final ReferralService referralService;
    private final UserTokenRepository userTokenRepository;

    @Value("${jwt.expiration.ms}")
    private long otpExpiryMs;

    @Value("${jwt.expiration.password.ms}")
    private long passwordExpiryMs;

    // ── OTP flow ──────────────────────────────────────────────────────────────

    public SendOtpResponse sendOtp(String phoneNumber) {
        otpService.generateAndSaveOtp(phoneNumber);
        return new SendOtpResponse("OTP sent successfully", phoneNumber);
    }

    @Transactional
    public AuthResponse verifyOtp(VerifyOtpRequest request) {
        otpService.verifyOtp(request.phoneNumber(), request.otp());

        boolean isNewUser = !userRepository.existsByPhoneNumber(request.phoneNumber());
        User user;

        if (isNewUser) {
            user = registerNewUser(request.phoneNumber());
            if (request.referralCode() != null && !request.referralCode().isBlank()) {
                try {
                    referralService.applyReferral(request.phoneNumber(), request.referralCode());
                } catch (Exception e) {
                    log.warn("Referral apply failed for {}: {}", request.phoneNumber(), e.getMessage());
                }
            }
        } else {
            user = userRepository.findByPhoneNumber(request.phoneNumber()).orElseThrow();
        }

        // OTP login → 10 hour token
        List<String> roles = loadRoles(user.getId());
        String token = jwtUtil.generateToken(user.getPhoneNumber(), roles, otpExpiryMs);
        saveTokenRecord(user, token, otpExpiryMs, null);

        return new AuthResponse(token, otpExpiryMs, user.getPhoneNumber(),
                user.getUsername(), roles, isNewUser, user.getHasPassword());
    }

    // ── Password flow ─────────────────────────────────────────────────────────

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByPhoneNumber(request.phoneNumber())
                .orElseThrow(() -> new WeekendBasketException("Invalid phone number or password."));

        if (!user.getHasPassword()) {
            throw new WeekendBasketException("No password set. Please login with OTP.");
        }

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new WeekendBasketException("Your account is blocked. Please contact support.");
        }

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new WeekendBasketException("Invalid phone number or password.");
        }

        // Password login → 7 day token
        List<String> roles = loadRoles(user.getId());
        String token = jwtUtil.generateToken(user.getPhoneNumber(), roles, passwordExpiryMs);
        saveTokenRecord(user, token, passwordExpiryMs, null);

        log.info("Password login: {}", user.getPhoneNumber());
        return new AuthResponse(token, passwordExpiryMs, user.getPhoneNumber(),
                user.getUsername(), roles, false, true);
    }

    @Transactional
    public void setPassword(String phoneNumber, SetPasswordRequest request) {
        if (!request.password().equals(request.confirmPassword())) {
            throw new WeekendBasketException("Passwords do not match.");
        }

        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new WeekendBasketException("User not found."));

        user.setPassword(passwordEncoder.encode(request.password()));
        user.setHasPassword(true);
        userRepository.save(user);

        log.info("Password set for: {}", phoneNumber);
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    @Transactional
    public void logout(String token) {
        InvalidatedToken invalidated = InvalidatedToken.builder()
                .token(token)
                .invalidatedAt(LocalDateTime.now())
                .build();
        invalidatedTokenRepository.save(invalidated);
        log.info("Token invalidated on logout");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private User registerNewUser(String phoneNumber) {
        User user = User.builder()
                .phoneNumber(phoneNumber)
                .password(passwordEncoder.encode(phoneNumber)) // placeholder until user sets real password
                .username("guest")
                .referralCode(generateReferralCode())
                .status("ACTIVE")
                .hasPassword(false)
                .build();
        userRepository.save(user);

        profileRepository.save(UserProfile.builder().user(user).build());

        Role customerRole = roleRepository.findByRoleId("ROLE_CUSTOMER")
                .orElseThrow(() -> new RuntimeException("ROLE_CUSTOMER not seeded in DB"));
        roleAccessRepository.save(RoleAccess.builder().user(user).role(customerRole).build());

        log.info("New user registered: {}", phoneNumber);
        return user;
    }

    private List<String> loadRoles(Long userId) {
        return roleAccessRepository.findByUserId(userId)
                .stream()
                .map(ra -> ra.getRole().getRoleId())
                .toList();
    }

    private String generateReferralCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    private void saveTokenRecord(User user, String token, long expiryMs, String deviceHint) {
        try {
            String hash = HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
            LocalDateTime now = LocalDateTime.now();
            userTokenRepository.save(UserToken.builder()
                    .user(user)
                    .tokenHash(hash)
                    .issuedAt(now)
                    .expiredAt(now.plusNanos(expiryMs * 1_000_000L))
                    .lastUsedAt(now)
                    .deviceHint(deviceHint)
                    .build());
        } catch (Exception e) {
            log.warn("Failed to save token record: {}", e.getMessage());
        }
    }
}
