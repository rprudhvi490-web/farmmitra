package com.weekendbasket.app.event;

public record BatchCompletedEvent(Long batchId, Long cycleId) {}
