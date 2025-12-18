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

    private String messageId;
    private String patientId;
    private String message;
    private LocalDateTime timestamp;
    private List<ConversationHistory> conversationHistory;
    private Map<String, Object> metadata;
}
