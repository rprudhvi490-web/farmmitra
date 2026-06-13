package com.weekendbasket.app.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "otp_tracker")
@Data                       // Generates getters, setters, toString, equals, and hashCode
@NoArgsConstructor          // Generates the default blank constructor
@AllArgsConstructor         // Generates the parameterized constructor
public class OtpTracker {

    @Id
    @Column(name = "tracker_date")
    private LocalDate date;

    @Column(name = "request_count", nullable = false)
    private int requestCount;
}