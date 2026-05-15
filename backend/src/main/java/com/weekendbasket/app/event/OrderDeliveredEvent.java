package com.weekendbasket.app.event;

public record OrderDeliveredEvent(Long orderId, Long userId) {}
