package com.weekendbasket.app.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
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

    // ── Firebase Phone Auth ───────────────────────────────────────────────────

    @Transactional
    public AuthResponse firebaseLogin(FirebaseLoginRequest request) {
        try {
            FirebaseToken decoded = FirebaseAuth.getInstance().verifyIdToken(request.token());
            String rawPhone = (String) decoded.getClaims().get("phone_number");
            if (rawPhone == null) {
                throw new WeekendBasketException("Phone number not found in Firebase token.");
            }
            // Firebase sends +91XXXXXXXXXX — strip country code
            String phoneNumber = rawPhone.startsWith("+91") ? rawPhone.substring(3) : rawPhone;

            boolean isNewUser = !userRepository.existsByPhoneNumber(phoneNumber);
            User user;

            if (isNewUser) {
                user = registerNewUser(phoneNumber);
                if (request.referralCode() != null && !request.referralCode().isBlank()) {
                    try {
                        referralService.applyReferral(phoneNumber, request.referralCode());
                    } catch (Exception e) {
                        log.warn("Referral apply failed for {}: {}", phoneNumber, e.getMessage());
                    }
                }
            } else {
                user = userRepository.findByPhoneNumber(phoneNumber).orElseThrow();
            }

            if (!"ACTIVE".equals(user.getStatus())) {
                throw new WeekendBasketException("Your account is blocked. Please contact support.");
            }

            List<String> roles = loadRoles(user.getId());
            String token = jwtUtil.generateToken(user.getPhoneNumber(), roles, otpExpiryMs);
            saveTokenRecord(user, token, otpExpiryMs, null);

            log.info("Firebase login success: {}", phoneNumber);
            return new AuthResponse(token, otpExpiryMs, user.getPhoneNumber(),
                    user.getUsername(), roles, isNewUser, user.getHasPassword());

        } catch (WeekendBasketException e) {
            throw e;
        } catch (Exception e) {
            log.error("Firebase token verification failed: {}", e.getMessage());
            throw new WeekendBasketException("Invalid or expired Firebase token.");
        }
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
        invalidatedTokenRepository.save(InvalidatedToken.builder()
                .token(token)
                .invalidatedAt(LocalDateTime.now())
                .build());
        log.info("Token invalidated on logout");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private User registerNewUser(String phoneNumber) {
        User user = User.builder()
                .phoneNumber(phoneNumber)
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
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
                .stream().map(ra -> ra.getRole().getRoleId()).toList();
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
                    .user(user).tokenHash(hash).issuedAt(now)
                    .expiredAt(now.plusNanos(expiryMs * 1_000_000L))
                    .lastUsedAt(now).deviceHint(deviceHint).build());
        } catch (Exception e) {
            log.warn("Failed to save token record: {}", e.getMessage());
        }
    }
}
