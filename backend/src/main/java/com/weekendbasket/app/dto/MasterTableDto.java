package com.weekendbasket.app.dto;

import jakarta.validation.constraints.NotBlank;

public class MasterTableDto {

    public record CreateMasterRequest(
            @NotBlank(message = "Type is required")
            String type,
            @NotBlank(message = "Lookup code is required")
            String lookupCode,
            @NotBlank(message = "Lookup item is required")
            String lookupItem,
            String lookupValue
    ) {}

    public record MasterResponse(
            Long id,
            String type,
            String lookupCode,
            String lookupItem,
            String lookupValue
    ) {}
}
