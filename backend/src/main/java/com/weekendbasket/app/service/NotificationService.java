package com.weekendbasket.app.service;

import com.weekendbasket.app.event.*;
import com.weekendbasket.app.exception.ResourceNotFoundException;
import com.weekendbasket.app.model.Notification;
import com.weekendbasket.app.model.User;
import com.weekendbasket.app.notification.NotificationSendStrategy;
import com.weekendbasket.app.repository.*;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final Logger log = LogManager.getLogger(NotificationService.class);

    // All strategies injected in @Order — InApp first, FCM second
    private final List<NotificationSendStrategy> strategies;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final CustomerOrderRepository orderRepository;

    // ── Event Listeners (all async) ───────────────────────────────────────────

    @EventListener
    @Async("appTaskExecutor")
    public void onCycleOpened(CycleOpenedEvent event) {
        broadcast("Ordering is now Open!",
                "This week's ordering is open. Order by " + event.orderCloseAt(),
                "ANNOUNCEMENT");
    }

    @EventListener
    @Async("appTaskExecutor")
    public void onCycleClosed(CycleClosedEvent event) {
        broadcast("Ordering Closed",
                "Ordering closed. We're now planning your procurement!",
                "ANNOUNCEMENT");
    }

    @EventListener
    @Async("appTaskExecutor")
    public void onOrderPlaced(OrderPlacedEvent event) {
        sendToUser(event.userId(),
                "Order Placed!",
                "Order #" + event.orderNumber() + " placed successfully. Total: ₹" + event.totalAmount(),
                "ORDER_UPDATE");
    }

    @EventListener
    @Async("appTaskExecutor")
    @Transactional
    public void onTransportStageUpdated(TransportStageUpdatedEvent event) {
        String message = stageMessage(event.stage(), event.notes());
        // Notify all customers who have orders in this cycle
        orderRepository.findByCycleIdAndStatusNot(event.cycleId(), "CANCELLED")
                .forEach(order -> sendToUser(order.getUser().getId(), "Order Update", message, "ORDER_UPDATE"));
    }

    @EventListener
    @Async("appTaskExecutor")
    public void onOrderDelivered(OrderDeliveredEvent event) {
        sendToUser(event.userId(),
                "Order Delivered!",
                "Your order has been delivered. Enjoy!",
                "ORDER_UPDATE");
    }

    // ── Public API (admin-triggered) ──────────────────────────────────────────

    @Transactional
    public void sendToUser(Long userId, String title, String body, String type) {
        strategies.forEach(s -> s.sendToUser(userId, title, body, type));
    }

    @Transactional
    public void broadcast(String title, String body, String type) {
        strategies.forEach(s -> s.broadcast(title, body, type));
    }

    @Transactional(readOnly = true)
    public Page<Notification> getMyNotifications(Long userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrBroadcast(userId, pageable);
    }

    @Transactional(readOnly = true)
    public List<Notification> getMyNotifications(Long userId) {
        return notificationRepository.findByUserIdOrBroadcast(userId);
    }

    @Transactional
    public void markRead(Long notificationId, Long userId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));
        n.setReadStatus(true);
        notificationRepository.save(n);
    }

    @Transactional
    public void markAllRead(Long userId) {
        notificationRepository.findByUserIdOrBroadcast(userId)
                .forEach(n -> { n.setReadStatus(true); notificationRepository.save(n); });
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadStatusFalse(userId);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String stageMessage(String stage, String notes) {
        return switch (stage) {
            case "PROCUREMENT_STARTED" -> "We've started purchasing your items!";
            case "GOODS_LOADED"        -> "Goods loaded and ready for transport.";
            case "IN_TRANSIT"          -> "Your groceries are on the way to Hyderabad!";
            case "ARRIVED"             -> "Goods arrived in Hyderabad. Packing begins!";
            case "PACKING"             -> "Your order is being packed.";
            case "DISPATCHED"          -> "Out for weekend delivery!";
            default                    -> notes != null ? notes : stage;
        };
    }
}
