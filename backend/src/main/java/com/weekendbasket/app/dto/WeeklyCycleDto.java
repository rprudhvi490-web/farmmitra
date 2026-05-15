package com.weekendbasket.app.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class WeeklyCycleDto {

    public record CycleResponse(
            Long id,
            String cycleLabel,
            String status,
            LocalDateTime orderOpenAt,
            LocalDateTime orderCloseAt,
            LocalDate deliveryDateSat,
            LocalDate deliveryDateSun,
            Long timeRemainingSeconds
    ) {}

    public record CreateCycleRequest(
            String cycleLabel,
            LocalDateTime orderOpenAt,
            LocalDateTime orderCloseAt,
            LocalDate deliveryDateSat,
            LocalDate deliveryDateSun
    ) {}
}
