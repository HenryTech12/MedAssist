package com.medassist.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;


@Data
public class AppointmentRequest {

    private LocalTime time;
    private LocalDate day;
    private String phone;
}
