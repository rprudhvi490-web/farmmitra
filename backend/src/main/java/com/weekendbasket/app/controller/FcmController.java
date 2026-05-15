package com.weekendbasket.app.controller;

import com.weekendbasket.app.dto.NotificationDto.FcmRegisterRequest;
import com.weekendbasket.app.service.FcmService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fcm")
@RequiredArgsConstructor
public class FcmController {

    private final FcmService fcmService;

    @PostMapping("/register")
    public ResponseEntity<Void> register(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody FcmRegisterRequest request) {
        fcmService.registerToken(userDetails.getUsername(), request.token(), request.deviceType());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{tokenId}")
    public ResponseEntity<Void> remove(@PathVariable Long tokenId) {
        fcmService.removeToken(tokenId);
        return ResponseEntity.noContent().build();
    }
}
