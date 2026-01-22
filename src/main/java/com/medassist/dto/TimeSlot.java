package com.medassist.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Represents an available time slot for booking
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeSlot {

    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer durationMinutes;
    private boolean available;

    /**
     * Returns a formatted string representation of the slot
     */
    public String toDisplayString() {
        return String.format("%s at %s",
            date.toString(),
            startTime.toString());
    }
}
