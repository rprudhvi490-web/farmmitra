package com.weekendbasket.app.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class UserDto {

    public record UpdateProfileRequest(
            String username,
            String firstName,
            String lastName,
            String email,
            String flatNumber,
            String block
    ) {}

    public record UserProfileResponse(
            Long userId,
            String phoneNumber,
            String username,
            String referralCode,
            String status,
            String firstName,
            String lastName,
            String email,
            String flatNumber,
            String block,
            List<String> roles
    ) {}

    public record AssignRoleRequest(
            @NotBlank(message = "Role ID is required")
            String roleId
    ) {}

    public record UpdateUsernameRequest(
            @NotBlank(message = "Username is required")
            String username
    ) {}
}
