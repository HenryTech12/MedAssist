package com.medassist.service;

import com.medassist.dto.TimeSlot;
import com.medassist.entity.Appointment;
import com.medassist.entity.DoctorSchedule;
import com.medassist.enums.AppointmentStatus;
import com.medassist.repository.AppointmentRepository;
import com.medassist.repository.DoctorScheduleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing doctor/clinic availability and checking for conflicts
 */
@Service
@Slf4j
public class AvailabilityService {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private DoctorScheduleRepository scheduleRepository;

    // Default working hours if no schedule is defined
    private static final LocalTime DEFAULT_START_TIME = LocalTime.of(9, 0);
    private static final LocalTime DEFAULT_END_TIME = LocalTime.of(17, 0);
    private static final int DEFAULT_SLOT_DURATION = 30;

    /**
     * Get available time slots for a specific doctor on a specific date
     */
    public List<TimeSlot> getAvailableSlots(UUID doctorId, LocalDate date) {
        // Get doctor's schedule for the day
        DoctorSchedule schedule = scheduleRepository
                .findByDoctorIdAndDayOfWeek(doctorId, date.getDayOfWeek())
                .orElse(null);

        // Use defaults if no schedule found
        LocalTime startTime = schedule != null ? schedule.getStartTime() : DEFAULT_START_TIME;
        LocalTime endTime = schedule != null ? schedule.getEndTime() : DEFAULT_END_TIME;
        int slotDuration = schedule != null ? schedule.getSlotDurationMinutes() : DEFAULT_SLOT_DURATION;

        // Check if doctor is available on this day
        if (schedule != null && !schedule.getIsAvailable()) {
            return new ArrayList<>(); // Doctor not available
        }

        // Get existing appointments for this doctor on this date
        List<Appointment> existingAppointments = appointmentRepository
                .findByDoctorIdAndDay(doctorId, date)
                .stream()
                .filter(a -> a.getStatus() != AppointmentStatus.CANCELLED)
                .collect(Collectors.toList());

        // Calculate available slots
        return calculateAvailableSlots(date, startTime, endTime, slotDuration, existingAppointments);
    }

    /**
     * Get available slots for a clinic (any available doctor)
     */
    public List<TimeSlot> getAvailableSlotsForClinic(UUID clinicId, LocalDate date) {
        // For now, return default slots based on clinic hours
        // In a full implementation, this would aggregate across all doctors
        return generateDefaultSlots(date);
    }

    /**
     * Generate default available slots for a date (used when no specific doctor)
     */
    public List<TimeSlot> generateDefaultSlots(LocalDate date) {
        // Don't generate slots for past dates
        if (date.isBefore(LocalDate.now())) {
            return new ArrayList<>();
        }

        List<TimeSlot> slots = new ArrayList<>();
        LocalTime current = DEFAULT_START_TIME;

        // If date is today, start from next available hour
        if (date.equals(LocalDate.now())) {
            LocalTime now = LocalTime.now();
            if (now.isAfter(DEFAULT_START_TIME)) {
                // Round up to next half hour
                int minutes = now.getMinute();
                if (minutes > 30) {
                    current = LocalTime.of(now.getHour() + 1, 0);
                } else if (minutes > 0) {
                    current = LocalTime.of(now.getHour(), 30);
                } else {
                    current = LocalTime.of(now.getHour(), 0);
                }
            }
        }

        while (current.plusMinutes(DEFAULT_SLOT_DURATION).isBefore(DEFAULT_END_TIME)
                || current.plusMinutes(DEFAULT_SLOT_DURATION).equals(DEFAULT_END_TIME)) {
            slots.add(TimeSlot.builder()
                    .date(date)
                    .startTime(current)
                    .endTime(current.plusMinutes(DEFAULT_SLOT_DURATION))
                    .durationMinutes(DEFAULT_SLOT_DURATION)
                    .available(true)
                    .build());
            current = current.plusMinutes(DEFAULT_SLOT_DURATION);
        }

        return slots;
    }

    /**
     * Check if a specific slot is available
     */
    public boolean isSlotAvailable(UUID doctorId, LocalDate day, LocalTime time) {
        return !appointmentRepository.existsByDoctorIdAndDayAndTimeAndStatusNot(
                doctorId, day, time, AppointmentStatus.CANCELLED);
    }

    /**
     * Check if a slot conflicts with existing appointments (without doctor)
     */
    public boolean isSlotAvailable(LocalDate day, LocalTime time, String phone) {
        // Check if this patient already has an appointment at this time
        List<Appointment> existingAppointments = appointmentRepository
                .findByPhoneAndStatus(phone, AppointmentStatus.SCHEDULED);

        return existingAppointments.stream()
                .noneMatch(a -> a.getDay().equals(day) && a.getTime().equals(time));
    }

    /**
     * Get next available slots (returns next N available slots across upcoming days)
     */
    public List<TimeSlot> getNextAvailableSlots(int count) {
        List<TimeSlot> slots = new ArrayList<>();
        LocalDate currentDate = LocalDate.now();
        int daysChecked = 0;
        int maxDaysToCheck = 14; // Look up to 2 weeks ahead

        while (slots.size() < count && daysChecked < maxDaysToCheck) {
            // Skip weekends (optional - remove if clinic operates on weekends)
            if (currentDate.getDayOfWeek().getValue() <= 5) {
                List<TimeSlot> dailySlots = generateDefaultSlots(currentDate);
                for (TimeSlot slot : dailySlots) {
                    if (slots.size() < count) {
                        slots.add(slot);
                    }
                }
            }
            currentDate = currentDate.plusDays(1);
            daysChecked++;
        }

        return slots;
    }

    /**
     * Format available slots as a numbered list for WhatsApp display
     */
    public String formatSlotsForDisplay(List<TimeSlot> slots) {
        if (slots.isEmpty()) {
            return "No available slots found.";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < slots.size(); i++) {
            TimeSlot slot = slots.get(i);
            sb.append(String.format("%d. %s at %s\n",
                    i + 1,
                    slot.getDate().toString(),
                    slot.getStartTime().toString()));
        }
        return sb.toString();
    }

    private List<TimeSlot> calculateAvailableSlots(
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            int slotDuration,
            List<Appointment> existingAppointments) {

        List<TimeSlot> availableSlots = new ArrayList<>();
        LocalTime current = startTime;

        // If date is today, adjust start time
        if (date.equals(LocalDate.now())) {
            LocalTime now = LocalTime.now();
            if (now.isAfter(startTime)) {
                int minutes = now.getMinute();
                if (minutes > 30) {
                    current = LocalTime.of(now.getHour() + 1, 0);
                } else if (minutes > 0) {
                    current = LocalTime.of(now.getHour(), 30);
                }
            }
        }

        while (current.plusMinutes(slotDuration).isBefore(endTime)
                || current.plusMinutes(slotDuration).equals(endTime)) {

            final LocalTime slotTime = current;
            boolean isBooked = existingAppointments.stream()
                    .anyMatch(a -> a.getTime().equals(slotTime));

            if (!isBooked) {
                availableSlots.add(TimeSlot.builder()
                        .date(date)
                        .startTime(current)
                        .endTime(current.plusMinutes(slotDuration))
                        .durationMinutes(slotDuration)
                        .available(true)
                        .build());
            }

            current = current.plusMinutes(slotDuration);
        }

        return availableSlots;
    }
}
