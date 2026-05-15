package com.weekendbasket.app.event;

public record OrderPlacedEvent(Long orderId, Long userId, String orderNumber, String totalAmount) {}
