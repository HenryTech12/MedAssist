package com.medassist.service;

import com.medassist.dto.AppointmentNotificationMessages;
import com.medassist.entity.Appointment;
import com.medassist.enums.AppointmentStatus;
import com.medassist.repository.AppointmentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service for sending automated appointment reminders
 */
@Service
@Slf4j
public class AppointmentReminderService {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private TwilioService twilioService;

    @Autowired
    private NotificationService notificationService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("EEEE, MMMM d");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a");

    /**
     * Send reminders 24 hours before appointment
     * Runs daily at 9 AM
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void sendDayBeforeReminders() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        List<Appointment> appointments = appointmentRepository
                .findByDayAndStatus(tomorrow, AppointmentStatus.SCHEDULED);

        for (Appointment apt : appointments) {
            if (!Boolean.TRUE.equals(apt.getReminderSent24Hours())) {
                sendDayBeforeReminder(apt);
                apt.setReminderSent24Hours(true);
                appointmentRepository.save(apt);
            }
        }

        log.info("Sent {} day-before reminders", appointments.size());
    }

    /**
     * Send reminders 2 hours before appointment
     * Runs every 30 minutes
     */
    @Scheduled(cron = "0 */30 * * * *")
    public void sendHourBeforeReminders() {
        LocalDateTime twoHoursFromNow = LocalDateTime.now().plusHours(2);
        LocalDate targetDate = twoHoursFromNow.toLocalDate();
        LocalTime targetTime = twoHoursFromNow.toLocalTime();

        List<Appointment> appointments = appointmentRepository
                .findAppointmentsInTimeRange(
                        targetDate,
                        targetTime.minusMinutes(15),
                        targetTime.plusMinutes(15),
                        AppointmentStatus.SCHEDULED
                );

        for (Appointment apt : appointments) {
            if (!Boolean.TRUE.equals(apt.getReminderSent2Hours())) {
                sendHoursBeforeReminder(apt);
                apt.setReminderSent2Hours(true);
                appointmentRepository.save(apt);
            }
        }

        log.info("Sent {} 2-hour reminders", appointments.size());
    }

    /**
     * Send a day-before reminder for an appointment
     */
    private void sendDayBeforeReminder(Appointment appointment) {
        String message = String.format(
                AppointmentNotificationMessages.APPOINTMENT_REMINDER_DAY_BEFORE,
                appointment.getPatientName(),
                appointment.getDay().format(DATE_FORMATTER),
                appointment.getTime().format(TIME_FORMATTER),
                appointment.getReason() != null ? appointment.getReason() : "General visit"
        );

        sendReminder(appointment, message, "24-hour");
    }

    /**
     * Send a 2-hours-before reminder for an appointment
     */
    private void sendHoursBeforeReminder(Appointment appointment) {
        String message = String.format(
                AppointmentNotificationMessages.APPOINTMENT_REMINDER_HOURS_BEFORE,
                appointment.getPatientName(),
                appointment.getTime().format(TIME_FORMATTER)
        );

        sendReminder(appointment, message, "2-hour");
    }

    /**
     * Send reminder via WhatsApp (with SMS fallback)
     */
    private void sendReminder(Appointment appointment, String message, String type) {
        try {
            twilioService.sendMessage(appointment.getPhone(), message);
            log.info("{} WhatsApp reminder sent to {}", type, appointment.getPhone());
        } catch (Exception e) {
            log.error("Failed to send {} WhatsApp reminder to {}: {}",
                    type, appointment.getPhone(), e.getMessage());
            // Could add SMS fallback here
        }
    }

    /**
     * Send immediate confirmation after booking
     */
    public void sendBookingConfirmation(Appointment appointment) {
        String message = String.format(
                AppointmentNotificationMessages.APPOINTMENT_CONFIRMATION_DETAILS,
                appointment.getPatientName(),
                appointment.getDay().format(DATE_FORMATTER),
                appointment.getTime().format(TIME_FORMATTER),
                appointment.getReason() != null ? appointment.getReason() : "General visit"
        );

        try {
            twilioService.sendMessage(appointment.getPhone(), message);
            log.info("Booking confirmation sent to {}", appointment.getPhone());
        } catch (Exception e) {
            log.error("Failed to send booking confirmation to {}: {}",
                    appointment.getPhone(), e.getMessage());
        }
    }

    /**
     * Send cancellation notification
     */
    public void sendCancellationNotification(Appointment appointment) {
        try {
            twilioService.sendMessage(appointment.getPhone(),
                    AppointmentNotificationMessages.APPOINTMENT_CANCELLED);
            log.info("Cancellation notification sent to {}", appointment.getPhone());
        } catch (Exception e) {
            log.error("Failed to send cancellation notification to {}: {}",
                    appointment.getPhone(), e.getMessage());
        }
    }

    /**
     * Send reschedule notification
     */
    public void sendRescheduleNotification(Appointment appointment) {
        String message = String.format(
                AppointmentNotificationMessages.APPOINTMENT_CONFIRMATION_DETAILS,
                appointment.getPatientName(),
                appointment.getDay().format(DATE_FORMATTER),
                appointment.getTime().format(TIME_FORMATTER),
                appointment.getReason() != null ? appointment.getReason() : "General visit"
        );

        try {
            twilioService.sendMessage(appointment.getPhone(), message);
            log.info("Reschedule notification sent to {}", appointment.getPhone());
        } catch (Exception e) {
            log.error("Failed to send reschedule notification to {}: {}",
                    appointment.getPhone(), e.getMessage());
        }
    }
}
