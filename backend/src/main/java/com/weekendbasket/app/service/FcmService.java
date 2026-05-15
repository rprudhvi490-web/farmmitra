package com.weekendbasket.app.service;

import com.weekendbasket.app.exception.ResourceNotFoundException;
import com.weekendbasket.app.model.FcmToken;
import com.weekendbasket.app.model.User;
import com.weekendbasket.app.repository.FcmTokenRepository;
import com.weekendbasket.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FcmService {

    private final FcmTokenRepository fcmTokenRepository;
    private final UserRepository userRepository;

    @Transactional
    public void registerToken(String phoneNumber, String token, String deviceType) {
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // If token already exists, reactivate it
        fcmTokenRepository.findByToken(token).ifPresentOrElse(
                existing -> { existing.setActive(true); fcmTokenRepository.save(existing); },
                () -> fcmTokenRepository.save(FcmToken.builder()
                        .user(user).token(token).deviceType(deviceType).build())
        );
    }

    @Transactional
    public void removeToken(Long tokenId) {
        FcmToken token = fcmTokenRepository.findById(tokenId)
                .orElseThrow(() -> new ResourceNotFoundException("FCM token not found: " + tokenId));
        token.setActive(false);
        fcmTokenRepository.save(token);
    }
}
