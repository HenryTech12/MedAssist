package com.medassist.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class SendMessageResponse {
    private boolean success;
    private String messageId;
    private String twilioSid;
}
