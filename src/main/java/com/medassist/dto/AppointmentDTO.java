package com.medassist.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
public class AppointmentDTO {

    private LocalDate day;
    private LocalTime time;
    private String reason;
    private String phone;
    private String patientName;
    private LocalDateTime createdAt;
}
