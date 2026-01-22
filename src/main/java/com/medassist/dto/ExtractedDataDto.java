package com.medassist.dto;

import lombok.Data;

import java.util.List;

@Data
public class ExtractedDataDto {

    private String question;

    // Optional ISO date (yyyy-MM-dd) parsed by AI when user selects or confirms a slot
    private String day;

    // Optional time (HH:mm) parsed by AI when user selects or confirms a slot
    private String time;

    // If AI returns multiple suggested slots, they'll be provided here as human-readable strings
    private List<String> availableSlots;

    // Patient name (optional)
    private String patientName;

    // Phone number (optional)
    private String phone;
}
