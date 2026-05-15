package com.weekendbasket.app.dto;

import jakarta.validation.constraints.NotBlank;

public class RoleDto {

    public record CreateRoleRequest(
            @NotBlank(message = "Role name is required")
            String roleName,
            @NotBlank(message = "Role ID is required")
            String roleId
    ) {}

    public record RoleResponse(
            Long id,
            String roleName,
            String roleId
    ) {}
}
