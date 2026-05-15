package com.weekendbasket.app.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "delivery_batch_order",
        uniqueConstraints = @UniqueConstraint(columnNames = {"batch_id", "order_id"}, name = "uq_batch_order"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryBatchOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private DeliveryBatch batch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private CustomerOrder order;
}
