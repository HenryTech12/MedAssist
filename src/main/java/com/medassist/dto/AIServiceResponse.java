package com.medassist.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.medassist.enums.TriageLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIServiceResponse {

    private String messageId;
    private String intent;
    private Double confidence;
    private String response;
    private ExtractedDataDto extractedData;
    private String nextAction;
    @JsonProperty("triage_level")
    private String triageLevel; // nullable
    private Boolean requiresHumanReview;
    private LocalDateTime timestamp;
}
