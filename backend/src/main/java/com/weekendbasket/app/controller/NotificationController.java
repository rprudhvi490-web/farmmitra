package com.weekendbasket.app.controller;

import com.weekendbasket.app.dto.NotificationDto.*;
import com.weekendbasket.app.model.Notification;
import com.weekendbasket.app.repository.UserRepository;
import com.weekendbasket.app.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @GetMapping("/my")
    public ResponseEntity<Page<NotificationResponse>> getMyNotifications(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 20, sort = "sentAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Long userId = resolveUserId(userDetails);
        Page<NotificationResponse> page = notificationService.getMyNotifications(userId, pageable)
                .map(this::toResponse);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/my/unread-count")
    public ResponseEntity<Long> getUnreadCount(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(notificationService.getUnreadCount(resolveUserId(userDetails)));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        notificationService.markRead(id, resolveUserId(userDetails));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllRead(@AuthenticationPrincipal UserDetails userDetails) {
        notificationService.markAllRead(resolveUserId(userDetails));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/send")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sendToUser(@Valid @RequestBody SendNotificationRequest request) {
        Long userId = userRepository.findByPhoneNumber(request.phoneNumber())
                .orElseThrow(() -> new RuntimeException("User not found: " + request.phoneNumber()))
                .getId();
        notificationService.sendToUser(userId, request.title(), request.body(), request.type());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/broadcast")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> broadcast(@Valid @RequestBody BroadcastRequest request) {
        notificationService.broadcast(request.title(), request.body(), request.type());
        return ResponseEntity.noContent().build();
    }

    private Long resolveUserId(UserDetails userDetails) {
        return userRepository.findByPhoneNumber(userDetails.getUsername())
                .orElseThrow().getId();
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(n.getId(), n.getTitle(), n.getBody(),
                n.getType(), n.getReadStatus(), n.getSentAt());
    }
}
