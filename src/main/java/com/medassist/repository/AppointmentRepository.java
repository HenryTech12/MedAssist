package com.medassist.repository;

import com.medassist.entity.Appointment;
import com.medassist.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    Optional<Appointment> findByPhone(String phone);

    List<Appointment> findByPhoneAndStatus(String phone, AppointmentStatus status);

    /**
     * Find appointments for a specific doctor on a specific day
     */
    List<Appointment> findByDoctorIdAndDay(UUID doctorId, LocalDate day);

    /**
     * Find appointments by day and status
     */
    List<Appointment> findByDayAndStatus(LocalDate day, AppointmentStatus status);

    /**
     * Check if a slot is already booked
     */
    boolean existsByDoctorIdAndDayAndTimeAndStatusNot(
        UUID doctorId,
        LocalDate day,
        LocalTime time,
        AppointmentStatus status
    );

    /**
     * Find appointments by clinic and date range
     */
    @Query("SELECT a FROM Appointment a WHERE a.clinicId = :clinicId AND a.day BETWEEN :startDate AND :endDate")
    List<Appointment> findByClinicIdAndDateRange(
        @Param("clinicId") UUID clinicId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Find appointments in a specific time range for reminders
     */
    @Query("SELECT a FROM Appointment a WHERE a.day = :day AND a.time BETWEEN :startTime AND :endTime AND a.status = :status")
    List<Appointment> findAppointmentsInTimeRange(
        @Param("day") LocalDate day,
        @Param("startTime") LocalTime startTime,
        @Param("endTime") LocalTime endTime,
        @Param("status") AppointmentStatus status
    );

    /**
     * Find appointments for a patient
     */
    List<Appointment> findByPhoneOrderByDayDescTimeDesc(String phone);

    /**
     * Find upcoming appointments for a patient
     */
    @Query("SELECT a FROM Appointment a WHERE a.phone = :phone AND a.day >= :today AND a.status = :status ORDER BY a.day, a.time")
    List<Appointment> findUpcomingByPhone(
        @Param("phone") String phone,
        @Param("today") LocalDate today,
        @Param("status") AppointmentStatus status
    );
}
