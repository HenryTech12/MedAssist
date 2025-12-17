package com.medassist.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIServiceRequest {

    @JsonProperty("message_id")
    private String messageId;

    @JsonProperty("patient_id")
    private String patientId;

    @JsonProperty("session_id")
    private String sessionId;

    @JsonProperty("message")
    private String message;
}
