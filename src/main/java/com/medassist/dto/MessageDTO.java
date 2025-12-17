package com.medassist.dto;

import com.medassist.enums.MessageRole;
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
public class MessageDTO {
    private String id;
    private MessageRole role;
    private String content;
    private TriageLevel triageLevel;
    private LocalDateTime timestamp;
}
