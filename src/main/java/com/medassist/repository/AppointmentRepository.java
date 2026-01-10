package com.medassist.repository;

import com.medassist.dto.AppointmentDTO;
import com.medassist.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment,Long> {
    Optional<Appointment> findByPhone(String phone);
}
