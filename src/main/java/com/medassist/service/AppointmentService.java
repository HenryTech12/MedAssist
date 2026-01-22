package com.medassist.service;

import com.medassist.dto.AppointmentDTO;
import com.medassist.dto.AppointmentNotificationMessages;
import com.medassist.dto.AppointmentRequest;
import com.medassist.entity.Appointment;
import com.medassist.enums.AppointmentStatus;
import com.medassist.repository.AppointmentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
public class AppointmentService {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private TwilioService twilioService;

    public AppointmentDTO createAppointment(AppointmentDTO appointmentDTO) {
        if (!Objects.isNull(appointmentDTO)) {
            Appointment appointment = Appointment.builder()
                    .day(appointmentDTO.getDay())
                    .time(appointmentDTO.getTime())
                    .reason(appointmentDTO.getReason())
                    .phone(appointmentDTO.getPhone())
                    .patientName(appointmentDTO.getPatientName())
                    .status(AppointmentStatus.SCHEDULED)
                    .build();

            appointmentRepository.save(appointment);
            log.info("Appointment created for patient: {}", appointmentDTO.getPatientName());

            // Send confirmation via WhatsApp
            try {
                twilioService.sendMessage(appointmentDTO.getPhone(),
                        AppointmentNotificationMessages.APPOINTMENT_SCHEDULED);
            } catch (Exception e) {
                log.error("Failed to send appointment confirmation: {}", e.getMessage());
            }

            // Send dashboard notification
            notificationService.sendNotifications(AppointmentNotificationMessages.APPOINTMENT_SCHEDULED);
        }
        return appointmentDTO;
    }

    public Map<String, Object> sendAppointmentMessage(String message) {
        notificationService.sendNotifications(message);
        Map<String, Object> data = new HashMap<>();
        data.put("status", "success");
        return data;
    }

    public Map<String, Object> cancelAppointment(String reason, String phone) {
        Appointment appointment = appointmentRepository
                .findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        // Soft delete - update status instead of deleting
        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancellationReason(reason);
        appointment.setCancelledAt(LocalDateTime.now());
        appointmentRepository.save(appointment);

        log.info("Appointment cancelled for phone: {}", phone);

        // Send cancellation notification
        try {
            twilioService.sendMessage(phone, AppointmentNotificationMessages.APPOINTMENT_CANCELLED);
        } catch (Exception e) {
            log.error("Failed to send cancellation notification: {}", e.getMessage());
        }

        notificationService.sendNotifications(AppointmentNotificationMessages.APPOINTMENT_CANCELLED);

        Map<String, Object> data = new HashMap<>();
        data.put("status", AppointmentStatus.CANCELLED);
        data.put("reason", reason);

        return data;
    }

    public Map<String, Object> rescheduleAppointment(String reason, AppointmentRequest appointmentRequest) {
        Appointment appointment = appointmentRepository
                .findByPhone(appointmentRequest.getPhone())
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        // Store previous values for notification
        LocalDate previousDay = appointment.getDay();
        var previousTime = appointment.getTime();

        // Update to new time
        appointment.setDay(appointmentRequest.getDay());
        appointment.setTime(appointmentRequest.getTime());
        appointment.setStatus(AppointmentStatus.RESCHEDULED);
        appointmentRepository.save(appointment);

        log.info("Appointment rescheduled for phone: {}", appointmentRequest.getPhone());

        // Send reschedule notification
        try {
            twilioService.sendMessage(appointmentRequest.getPhone(),
                    AppointmentNotificationMessages.APPOINTMENT_RESCHEDULED);
        } catch (Exception e) {
            log.error("Failed to send reschedule notification: {}", e.getMessage());
        }

        notificationService.sendNotifications(AppointmentNotificationMessages.APPOINTMENT_RESCHEDULED);

        Map<String, Object> data = new HashMap<>();
        data.put("status", AppointmentStatus.RESCHEDULED);
        data.put("reason", reason);
        data.put("previousDate", previousDay);
        data.put("previousTime", previousTime);
        data.put("newDate", appointment.getDay());
        data.put("newTime", appointment.getTime());

        return data;
    }

    public List<AppointmentDTO> fetchAllAppointment() {
        return appointmentRepository.findAll()
                .stream()
                .map(this::toAppointmentDTO)
                .toList();
    }

    public AppointmentDTO getAppointmentByPhone(String phone) {
        return appointmentRepository.findByPhone(phone)
                .map(this::toAppointmentDTO)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));
    }

    public List<AppointmentDTO> getUpcomingAppointments(String phone) {
        return appointmentRepository
                .findUpcomingByPhone(phone, LocalDate.now(), AppointmentStatus.SCHEDULED)
                .stream()
                .map(this::toAppointmentDTO)
                .toList();
    }

    public AppointmentDTO updateAppointment(AppointmentRequest appointmentRequest) {
        Appointment appointment = appointmentRepository
                .findByPhone(appointmentRequest.getPhone())
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        if (appointmentRequest.getDay() != null && appointmentRequest.getTime() != null) {
            appointment.setDay(appointmentRequest.getDay());
            appointment.setTime(appointmentRequest.getTime());
            appointmentRepository.save(appointment);
            log.info("Appointment updated for phone: {}", appointmentRequest.getPhone());
        } else {
            throw new RuntimeException("Invalid day and time");
        }
        return toAppointmentDTO(appointment);
    }

    public void deleteAppointmentByPhone(String phone) {
        Appointment appointment = appointmentRepository
                .findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));
        appointmentRepository.delete(appointment);
    }

    public AppointmentDTO toAppointmentDTO(Appointment appointment) {
        return AppointmentDTO.builder()
                .day(appointment.getDay())
                .time(appointment.getTime())
                .phone(appointment.getPhone())
                .reason(appointment.getReason())
                .patientName(appointment.getPatientName())
                .createdAt(appointment.getCreatedAt())
                .build();
    }
}
