package com.weekendbasket.app.dto;

import jakarta.validation.constraints.NotBlank;

public class CommunityDto {

    public record CreateCommunityRequest(
            @NotBlank(message = "Community name is required")
            String name,
            String city,
            String address
    ) {}

    public record CommunityResponse(
            Long id,
            String name,
            String city,
            String address,
            Boolean active
    ) {}
}
