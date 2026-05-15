package com.weekendbasket.app.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "weekly_cycle")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeeklyCycle extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cycle_label", nullable = false, length = 100)
    private String cycleLabel;

    @Column(name = "order_open_at", nullable = false)
    private LocalDateTime orderOpenAt;

    @Column(name = "order_close_at", nullable = false)
    private LocalDateTime orderCloseAt;

    @Column(name = "delivery_date_sat")
    private LocalDate deliveryDateSat;

    @Column(name = "delivery_date_sun")
    private LocalDate deliveryDateSun;

    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private String status = "OPEN";
}
