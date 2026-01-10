package com.medassist.controller;

import com.medassist.dto.AppointmentDTO;
import com.medassist.dto.AppointmentRequest;
import com.medassist.service.AppointmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    @Autowired
    private AppointmentService appointmentService;

    @PostMapping
    public ResponseEntity<AppointmentDTO> createAppointment(@RequestBody AppointmentDTO appointmentDTO) {
        return new ResponseEntity<>(appointmentService.createAppointment(appointmentDTO), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<AppointmentDTO>> getAllAppointments() {
        return new ResponseEntity<>(appointmentService.fetchAllAppointment(), HttpStatus.OK);
    }

    @PutMapping("/reschedule")
    public ResponseEntity<Map<String,Object>> rescheduleAppointment(@RequestBody String reason, AppointmentRequest appointmentRequest) {
        return new ResponseEntity<>(appointmentService.rescheduleAppointment(reason,appointmentRequest),HttpStatus.OK);
    }

    @DeleteMapping("/cancel")
    public ResponseEntity<Map<String,Object>> cancelAppointment(@RequestBody String phone, @RequestBody String reason) {
        return new ResponseEntity<>(appointmentService.cancelAppointment(reason,phone),HttpStatus.OK);
    }

    @PostMapping("/notify")
    public ResponseEntity<Map<String,Object>> cancelAppointment(@RequestBody String message) {
        return new ResponseEntity<>(appointmentService.sendAppointmentMessage(message),HttpStatus.OK);
    }
}
