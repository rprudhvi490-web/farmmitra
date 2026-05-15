package com.weekendbasket.app.event;

public record TransportStageUpdatedEvent(Long cycleId, String stage, String notes) {}
