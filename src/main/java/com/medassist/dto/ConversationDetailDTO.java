package com.medassist.dto;

import com.medassist.enums.ConversationStatus;
import com.medassist.enums.TriageLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationDetailDTO {
    private String id;
    private PatientDTO patient;
    private TriageLevel triageLevel;
    private ConversationStatus status;
    private List<MessageDTO> messages;
    private LocalDateTime createdAt;
    private LocalDateTime lastMessageAt;
}
