package com.medassist.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConversationHistory
{

    private String role;
    private String content;
    private LocalDateTime timestamp;
}
