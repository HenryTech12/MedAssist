package com.medassist.service;

import com.medassist.dto.PatientDTO;
import com.medassist.entity.Clinic;
import com.medassist.entity.Patient;
import com.medassist.exception.NotFoundException;
import com.medassist.repository.ClinicRepository;
import com.medassist.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PatientService {

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private ClinicRepository clinicRepository;

    public List<PatientDTO> getPatientsByClinic(String clinicId) {
        List<Patient> patients = patientRepository.findByClinicId(UUID.fromString(clinicId));
        return patients.stream()
                .map(this::toPatientDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public Patient createOrGetPatient(String phone, String clinicId) {
        return patientRepository.findByPhone(phone).orElse(null);
    }

    private PatientDTO toPatientDTO(Patient patient) {
        return PatientDTO.builder()
                .id(patient.getId().toString())
                .phone(patient.getPhone())
                .firstName(patient.getFirstName())
                .lastName(patient.getLastName())
                .clinicId(patient.getClinic().getId().toString())
                .registeredAt(patient.getRegisteredAt())
                .build();
    }
}
