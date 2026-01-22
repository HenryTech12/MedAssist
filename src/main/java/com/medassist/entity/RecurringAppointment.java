package com.medassist.entity;

import com.medassist.enums.RecurrencePattern;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Represents a recurring appointment pattern for patients with regular visits
 */
@Entity
@Table(name = "recurring_appointments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecurringAppointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_id")
    private UUID patientId;

    @Column(name = "doctor_id")
    private UUID doctorId;

    @Column(name = "clinic_id")
    private UUID clinicId;

    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecurrencePattern pattern;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_day")
    private DayOfWeek preferredDay;

    @Column(name = "preferred_time")
    private LocalTime preferredTime;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate; // null for indefinite

    @Column(name = "max_occurrences")
    private Integer maxOccurrences;

    private String reason;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
