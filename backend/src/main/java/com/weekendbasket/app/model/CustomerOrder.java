package com.weekendbasket.app.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "customer_order")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerOrder extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", unique = true, nullable = false, length = 50)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cycle_id", nullable = false)
    private WeeklyCycle cycle;

    @Column(name = "delivery_slot", length = 10)
    private String deliverySlot;

    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private String status = "PLACED";

    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "referral_discount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal referralDiscount = BigDecimal.ZERO;

    @Column(name = "amount_to_collect", precision = 10, scale = 2)
    private BigDecimal amountToCollect;

    @Column(name = "payment_method", length = 30)
    @Builder.Default
    private String paymentMethod = "COD";

    @Column(name = "payment_status", length = 20)
    @Builder.Default
    private String paymentStatus = "PENDING";

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
