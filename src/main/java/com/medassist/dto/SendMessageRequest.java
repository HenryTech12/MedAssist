package com.medassist.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendMessageRequest {

    @NotBlank(message = "Conversation ID is required")
    private String conversationId;

    @NotBlank(message = "Message is required")
    private String message;
}
