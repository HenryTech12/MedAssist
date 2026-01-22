package com.medassist.repository;

import com.medassist.entity.DoctorSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DoctorScheduleRepository extends JpaRepository<DoctorSchedule, Long> {

    /**
     * Find a doctor's schedule for a specific day of the week
     */
    Optional<DoctorSchedule> findByDoctorIdAndDayOfWeek(UUID doctorId, DayOfWeek dayOfWeek);

    /**
     * Find all schedules for a doctor
     */
    List<DoctorSchedule> findByDoctorId(UUID doctorId);

    /**
     * Find all schedules for a clinic
     */
    List<DoctorSchedule> findByClinicId(UUID clinicId);

    /**
     * Find available schedules for a doctor
     */
    List<DoctorSchedule> findByDoctorIdAndIsAvailableTrue(UUID doctorId);
}
