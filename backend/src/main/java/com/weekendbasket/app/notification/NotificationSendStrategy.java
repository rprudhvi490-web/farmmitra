package com.weekendbasket.app.notification;

import java.util.List;

public interface NotificationSendStrategy {
    void sendToUser(Long userId, String title, String body, String type);
    void broadcast(String title, String body, String type);
}
