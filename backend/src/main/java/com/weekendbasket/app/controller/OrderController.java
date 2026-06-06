package com.weekendbasket.app.controller;

import com.weekendbasket.app.dto.OrderDto.*;
import com.weekendbasket.app.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // ── Customer ─────────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody PlaceOrderRequest request) {
        return ResponseEntity.ok(orderService.placeOrder(userDetails.getUsername(), request));
    }

    @GetMapping("/my")
    public ResponseEntity<List<OrderResponse>> getMyOrders(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(orderService.getMyOrders(userDetails.getUsername()));
    }

    @GetMapping("/my/{orderId}")
    public ResponseEntity<OrderResponse> getMyOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.getMyOrder(userDetails.getUsername(), orderId));
    }

    @PutMapping("/my/{orderId}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.cancelOrder(userDetails.getUsername(), orderId));
    }

    // ── Admin ─────────────────────────────────────────────────────────────────

    @GetMapping("/cycle/{cycleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<OrderResponse>> getOrdersByCycle(
            @PathVariable Long cycleId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(orderService.getOrdersByCycle(cycleId, pageable));
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.getOrderById(orderId));
    }

    @PutMapping("/{orderId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderResponse> updateStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        return ResponseEntity.ok(orderService.updateStatus(orderId, request.status()));
    }

    @PutMapping("/{orderId}/slot")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderResponse> assignDeliverySlot(
            @PathVariable Long orderId,
            @RequestParam String slot) {
        return ResponseEntity.ok(orderService.assignDeliverySlot(orderId, slot));
    }

    @PutMapping("/{orderId}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderResponse> adminCancelOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.adminCancelOrder(orderId));
    }

    @PutMapping("/{orderId}/payment")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> markPaid(@PathVariable Long orderId) {
        orderService.markPaid(orderId);
        return ResponseEntity.noContent().build();
    }
}
