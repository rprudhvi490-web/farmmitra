package com.weekendbasket.app.event;

public record CycleOpenedEvent(Long cycleId, String cycleLabel, String orderCloseAt) {}
