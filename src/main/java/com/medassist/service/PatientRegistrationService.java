package com.medassist.service;

import com.medassist.dto.PatientDTO;
import com.medassist.entity.Clinic;
import com.medassist.entity.Patient;
import com.medassist.repository.ClinicRepository;
import com.medassist.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for handling patient registration via WhatsApp
 * Uses database to track pending registrations (no Redis required)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PatientRegistrationService {
    
    private final PatientRepository patientRepository;
    private final ClinicRepository clinicRepository;
    
    // Clinic mapping - maps user's selection (1, 2, 3) to clinic UUIDs
    private static final Map<String, String> CLINIC_OPTIONS = Map.of(
        "1", "24cb85f9-db74-11f0-939e-b2e962fd1365",  // City Health Clinic
        "2", "24cb8bbb-db74-11f0-939e-b2e962fd1365",  // Green Cross Pharmacy
        "3", "24cb8d2f-db74-11f0-939e-b2e962fd1365"   // Life Care Hospital
    );
    
    /**
     * Find patient by phone number
     */
    public Patient findByPhone(String phone) {
        return patientRepository.findByPhone(phone).orElse(null);
    }
    
    /**
     * Check if patient is in any pending registration state
     */
    public boolean isPendingRegistration(Patient patient) {
        if (patient == null) return false;
        String status = patient.getRegistrationStatus();
        return "PENDING_CLINIC".equals(status) || "AWAITING_NAME".equals(status);
    }
    
    /**
     * Start registration process - create patient record with pending status
     */
    @Transactional
    public Patient startRegistration(String phone) {
        log.info("Starting registration for phone: {}", phone);
        
        // Create patient with pending status and temporary clinic
        Clinic defaultClinic = clinicRepository.findById(
                UUID.fromString(CLINIC_OPTIONS.get("1")))
            .orElseThrow(() -> new RuntimeException("Default clinic not found"));
        
        Patient patient = Patient.builder()
            .phone(phone)
            .clinic(defaultClinic)  // Temporary - will be updated
            .firstName("Pending")
            .lastName("Registration")
            .registrationStatus("PENDING_CLINIC")
            .build();
        
        return patientRepository.save(patient);
    }
    
    /**
     * Process clinic selection and move to name collection step
     */
    @Transactional
    public Patient setClinicSelection(Patient patient, String clinicSelection) {
        log.info("Setting clinic selection for patient {} with selection: {}", 
                 patient.getId(), clinicSelection);
        
        // Validate selection
        String clinicId = CLINIC_OPTIONS.get(clinicSelection);
        if (clinicId == null) {
            log.warn("Invalid clinic selection: {}", clinicSelection);
            return null;
        }
        
        // Get selected clinic
        Clinic clinic = clinicRepository.findById(UUID.fromString(clinicId))
            .orElseThrow(() -> new RuntimeException("Clinic not found: " + clinicId));
        
        // Update patient with clinic and change status to awaiting name
        patient.setClinic(clinic);
        patient.setRegistrationStatus("AWAITING_NAME");
        
        return patientRepository.save(patient);
    }
    
    /**
     * Complete registration with patient name
     */
    @Transactional
    public Patient completRegistrationWithName(Patient patient, String fullName) {
        log.info("Completing registration for patient {} with name: {}", 
                 patient.getId(), fullName);
        
        // Parse name (simple split on space)
        String[] nameParts = fullName.trim().split("\\s+", 2);
        String firstName = nameParts[0];
        String lastName = nameParts.length > 1 ? nameParts[1] : "";
        
        // Update patient
        patient.setFirstName(firstName);
        patient.setLastName(lastName);
        patient.setRegistrationStatus("COMPLETE");
        
        return patientRepository.save(patient);
    }
    
    /**
     * Cleanup stale pending registrations (can be run periodically)
     */
    @Transactional
    public void cleanupStalePendingRegistrations() {
        // Optional: Delete pending registrations older than 1 hour
        // This prevents database pollution from abandoned registrations
        log.info("Cleanup of stale pending registrations (not implemented yet)");
    }

    public List<PatientDTO> getPatientsByClinic(String clinicId) {
        return patientRepository.findByClinicId(UUID.fromString(clinicId))
                .stream().map(data -> new PatientDTO(data.getId().toString(),data.getPhone(),data.getFirstName(),data.getLastName(),data.getClinic().getId().toString(),data.getRegisteredAt())).toList();
    }
}
