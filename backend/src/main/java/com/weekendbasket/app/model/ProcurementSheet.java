package com.weekendbasket.app.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "procurement_sheet",
        uniqueConstraints = @UniqueConstraint(columnNames = {"cycle_id", "product_id"}, name = "uq_procurement_cycle_product"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcurementSheet extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cycle_id", nullable = false)
    private WeeklyCycle cycle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "total_quantity", nullable = false, precision = 8, scale = 2)
    private BigDecimal totalQuantity;

    @Column(name = "unit", length = 50)
    private String unit;

    @Column(name = "vendor_name", length = 200)
    private String vendorName;

    @Column(name = "vendor_notes", columnDefinition = "TEXT")
    private String vendorNotes;

    @Column(name = "procured_qty", precision = 8, scale = 2)
    private BigDecimal procuredQty;

    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private String status = "PENDING";
}
