package com.medassist.controller;

import com.medassist.dto.PatientDTO;
import com.medassist.security.UserPrincipal;
import com.medassist.service.PatientRegistrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    @Autowired
    private PatientRegistrationService patientRegistrationService;

    @GetMapping
    public ResponseEntity<List<PatientDTO>> getPatients(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<PatientDTO> patients = patientRegistrationService.getPatientsByClinic(principal.getClinicId());
        return ResponseEntity.ok(patients);
    }
}
