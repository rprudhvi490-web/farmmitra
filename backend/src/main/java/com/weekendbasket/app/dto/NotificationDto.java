package com.weekendbasket.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class NotificationDto {

    public record NotificationResponse(
            Long id,
            String title,
            String body,
            String type,
            Boolean readStatus,
            LocalDateTime sentAt
    ) {}

    public record SendNotificationRequest(
            @NotBlank(message = "Phone number is required")
            String phoneNumber,
            @NotBlank(message = "Title is required")
            String title,
            @NotBlank(message = "Body is required")
            String body,
            @NotBlank(message = "Type is required")
            String type
    ) {}

    public record BroadcastRequest(
            @NotBlank(message = "Title is required")
            String title,
            @NotBlank(message = "Body is required")
            String body,
            @NotBlank(message = "Type is required")
            String type
    ) {}

    public record FcmRegisterRequest(
            @NotBlank(message = "Token is required")
            String token,
            String deviceType
    ) {}
}
