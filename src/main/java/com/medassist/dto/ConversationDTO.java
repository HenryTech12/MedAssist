package com.medassist.dto;

import com.medassist.enums.ConversationStatus;
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
public class ConversationDTO {
    private String id;
    private String patientId;
    private String patientName;
    private String patientPhone;
    private TriageLevel triageLevel;
    private ConversationStatus status;
    private LocalDateTime lastMessageAt;
    private Integer messageCount;
    private String preview;
    private LocalDateTime createdAt;
}
