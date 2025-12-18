package com.medassist.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIServiceRequest {

    private String message_id;
    private String patient_id;
    private String message;
    private LocalDateTime timestamp;
    private List<ConversationHistory> conversationHistory;
    private Map<String, Object> metadata;
}
