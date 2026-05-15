package com.weekendbasket.app.notification;

import com.weekendbasket.app.model.Notification;
import com.weekendbasket.app.model.User;
import com.weekendbasket.app.repository.NotificationRepository;
import com.weekendbasket.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Order(1)
@RequiredArgsConstructor
public class InAppNotificationStrategy implements NotificationSendStrategy {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Override
    public void sendToUser(Long userId, String title, String body, String type) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;
        notificationRepository.save(Notification.builder()
                .user(user).title(title).body(body).type(type)
                .sentAt(LocalDateTime.now()).build());
    }

    @Override
    public void broadcast(String title, String body, String type) {
        // user = null means broadcast — shown to all users
        notificationRepository.save(Notification.builder()
                .user(null).title(title).body(body).type(type)
                .sentAt(LocalDateTime.now()).build());
    }
}
