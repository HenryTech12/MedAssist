package com.medassist.controller;

import com.medassist.dto.SendMessageRequest;
import com.medassist.dto.SendMessageResponse;
import com.medassist.security.UserPrincipal;
import com.medassist.service.ConversationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @Autowired
    private ConversationService conversationService;

    @PostMapping("/send")
    public ResponseEntity<SendMessageResponse> sendMessage(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SendMessageRequest request
    ) {
        SendMessageResponse response = conversationService.sendMessage(
                principal.getClinicId(),
                request
        );
        return ResponseEntity.ok(response);
    }
}
