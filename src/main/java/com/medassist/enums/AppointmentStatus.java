package com.medassist.enums;

public enum AppointmentStatus {
    SCHEDULED,      // Active appointment
    CONFIRMED,      // Patient confirmed
    RESCHEDULED,    // Time changed
    CANCELLED,      // Cancelled by patient/staff
    COMPLETED,      // Appointment completed
    NO_SHOW         // Patient didn't attend
}
