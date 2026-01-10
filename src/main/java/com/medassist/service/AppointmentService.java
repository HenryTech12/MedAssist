package com.medassist.service;

import com.medassist.dto.AppointmentDTO;
import com.medassist.dto.AppointmentNotificationMessages;
import com.medassist.dto.AppointmentRequest;
import com.medassist.entity.Appointment;
import com.medassist.enums.AppointmentStatus;
import com.medassist.repository.AppointmentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;

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

    public AppointmentDTO createAppointment(AppointmentDTO appointmentDTO) {
        if(!Objects.isNull(appointmentDTO)) {
            Appointment appointment = new Appointment();
            appointment.setDay(appointment.getDay());
            appointment.setTime(appointment.getTime());
            appointment.setReason(appointmentDTO.getReason());
            appointment.setPhone(appointmentDTO.getPhone());
            appointment.setPatientName(appointmentDTO.getPatientName());

            appointmentRepository.save(appointment);
            log.info("appointment set for patient");

            notificationService.sendNotifications(AppointmentNotificationMessages.APPOINTMENT_SCHEDULED);
        }
        return appointmentDTO;
    }

    public Map<String,Object> sendAppointmentMessage(String message) {
        notificationService.sendNotifications(message);
        Map<String,Object> data=new HashMap<>();
        data.put("status", "success");
        return  data;
    }

    public Map<String,Object> cancelAppointment(String reason, String phone) {

        deleteAppointmentByPhone(phone);

        log.info("appointment deleted from db");

        notificationService.sendNotifications(AppointmentNotificationMessages.APPOINTMENT_CANCELLED);
        Map<String,Object> data = new HashMap<>();
        data.put("status", AppointmentStatus.CANCELLED);
        data.put("reason",reason);

        return data;
    }

    public Map<String,Object> rescheduleAppointment(String reason, AppointmentRequest appointmentRequest) {

        updateAppointment(appointmentRequest);

        notificationService.sendNotifications(AppointmentNotificationMessages.APPOINTMENT_RESCHEDULED);
        Map<String,Object> data = new HashMap<>();
        data.put("status", AppointmentStatus.RESCHEDULED);
        data.put("reason", reason);
        return data;
    }

    public List<AppointmentDTO> fetchAllAppointment() {
        return appointmentRepository.findAll()
                .stream().map(this::toAppointmentDTO).toList();
    }

    public AppointmentDTO getAppointmentByPhone(String phone) {
        return appointmentRepository.findByPhone(phone)
                .stream().map(this::toAppointmentDTO)
                        .findFirst().
                orElseThrow(() -> new RuntimeException("Invalid patient contact"));
    }


    public AppointmentDTO updateAppointment(AppointmentRequest appointmentRequest) {
        Appointment appointment = appointmentRepository
                .findByPhone(appointmentRequest.getPhone()).orElseThrow(() -> new RuntimeException("Invalid patient contact"));
        if(appointmentRequest.getDay() != null && appointmentRequest.getTime() != null) {
            appointment.setDay(appointmentRequest.getDay());
            appointment.setTime(appointmentRequest.getTime());

            appointmentRepository.save(appointment);

            log.info("appointment data updated");
        }
        else {
            throw new RuntimeException("Invalid day and time");
        }
        return toAppointmentDTO(appointment);
    }

    public void deleteAppointmentByPhone(String phone) {
        Appointment appointment = appointmentRepository
                .findByPhone(phone).orElseThrow(() -> new RuntimeException("Invalid patient contact"));
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
