package com.medassist.repository;

import com.medassist.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientRepository extends JpaRepository<Patient, UUID> {
    Optional<Patient> findByPhone(String phone);
    List<Patient> findByClinicId(UUID clinicId);
    boolean existsByPhone(String phone);
}
