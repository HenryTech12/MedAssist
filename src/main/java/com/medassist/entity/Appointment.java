package com.medassist.entity;

import com.medassist.enums.AppointmentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "appointments", indexes = {
    @Index(name = "idx_patient_phone", columnList = "phone"),
    @Index(name = "idx_appointment_date", columnList = "day"),
    @Index(name = "idx_doctor_date_time", columnList = "doctor_id, day, time")
})
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "doctor_id")
    private UUID doctorId;

    @Column(name = "clinic_id")
    private UUID clinicId;

    @Column(nullable = false)
    private LocalDate day;

    @Column(nullable = false)
    private LocalTime time;

    @Column(name = "duration_minutes")
    @Builder.Default
    private Integer durationMinutes = 30;

    private String reason;

    @Column(nullable = false)
    private String phone;

    @Column(name = "patient_name")
    private String patientName;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AppointmentStatus status = AppointmentStatus.SCHEDULED;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "google_event_id")
    private String googleEventId;

    @Column(name = "recurring_appointment_id")
    private Long recurringAppointmentId;

    @Column(name = "reminder_sent_24h")
    @Builder.Default
    private Boolean reminderSent24Hours = false;

    @Column(name = "reminder_sent_2h")
    @Builder.Default
    private Boolean reminderSent2Hours = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
