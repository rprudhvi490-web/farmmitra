package com.weekendbasket.app.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "cycle_product", uniqueConstraints = @UniqueConstraint(columnNames = {"cycle_id", "product_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CycleProduct extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cycle_id", nullable = false)
    private WeeklyCycle cycle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "max_stock", nullable = false, precision = 10, scale = 2)
    private BigDecimal maxStock;

    @Column(name = "ordered_qty", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal orderedQty = BigDecimal.ZERO;

    @Column(name = "sold_out", nullable = false)
    @Builder.Default
    private Boolean soldOut = false;
}
