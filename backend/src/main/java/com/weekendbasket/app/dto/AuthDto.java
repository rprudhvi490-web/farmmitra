package com.weekendbasket.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public class AuthDto {

    public record FirebaseLoginRequest(
            @NotBlank(message = "Firebase token is required") String token,
            String referralCode
    ) {}

    public record LoginRequest(
            @NotBlank(message = "Phone number is required")
            @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid Indian phone number")
            String phoneNumber,

            @NotBlank(message = "Password is required")
            String password
    ) {}

    public record SetPasswordRequest(
            @NotBlank(message = "Password is required")
            @Size(min = 6, message = "Password must be at least 6 characters")
            String password,

            @NotBlank(message = "Confirm password is required")
            String confirmPassword
    ) {}

    public record AuthResponse(
            String token,
            long expiresIn,
            String phoneNumber,
            String username,
            List<String> roles,
            boolean isNewUser,
            boolean hasPassword
    ) {}
}
