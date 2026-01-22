package com.medassist.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientDTO {
    private String id;
    private String phone;
    private String firstName;
    private String lastName;
    private String clinicId;
    private LocalDateTime registeredAt;
}
