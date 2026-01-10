package com.medassist.entity;

import jakarta.persistence.Id;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class Appointment {

    @Id
    private Long id;
    private LocalDate day;
    private LocalTime time;
    private String reason;
    private String phone;
    private String patientName;
    private LocalDateTime createdAt;

}
