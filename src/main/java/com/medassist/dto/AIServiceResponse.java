package com.medassist.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.medassist.enums.TriageLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIServiceResponse {

    private String intent;

    private String response;

    @JsonProperty("triage_level")
    private TriageLevel triageLevel;

    private Double confidence;
}
