package com.medassist.controller;

import com.medassist.dto.ConversationDTO;
import com.medassist.dto.ConversationDetailDTO;
import com.medassist.dto.SendMessageRequest;
import com.medassist.dto.SendMessageResponse;
import com.medassist.enums.ConversationStatus;
import com.medassist.security.UserPrincipal;
import com.medassist.service.ConversationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    @Autowired
    private ConversationService conversationService;

    @GetMapping
    public ResponseEntity<List<ConversationDTO>> getConversations(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String triageLevel
    ) {
        List<ConversationDTO> conversations = conversationService.getConversations(
                principal.getClinicId(),
                status,
                triageLevel
        );
        return ResponseEntity.ok(conversations);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConversationDetailDTO> getConversationDetail(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id
    ) {
        ConversationDetailDTO conversation = conversationService.getConversationDetail(
                principal.getClinicId(),
                id
        );
        return ResponseEntity.ok(conversation);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Void> updateConversationStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id,
            @RequestBody Map<String, String> body
    ) {
        String status = body.get("status");
        if (status != null) {
            conversationService.updateConversationStatus(
                    principal.getClinicId(),
                    id,
                    ConversationStatus.valueOf(status.toUpperCase())
            );
        }
        return ResponseEntity.noContent().build();
    }
}
