package com.medassist.repository;

import com.medassist.entity.RecurringAppointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface RecurringAppointmentRepository extends JpaRepository<RecurringAppointment, Long> {

    /**
     * Find all active recurring appointments
     */
    @Query("SELECT r FROM RecurringAppointment r WHERE r.isActive = true AND (r.endDate IS NULL OR r.endDate >= CURRENT_DATE)")
    List<RecurringAppointment> findActive();

    /**
     * Find recurring appointments for a patient
     */
    List<RecurringAppointment> findByPatientId(UUID patientId);

    /**
     * Find recurring appointments by phone
     */
    List<RecurringAppointment> findByPhone(String phone);

    /**
     * Find active recurring appointments for a doctor
     */
    List<RecurringAppointment> findByDoctorIdAndIsActiveTrue(UUID doctorId);
}
