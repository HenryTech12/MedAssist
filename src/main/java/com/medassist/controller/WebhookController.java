package com.medassist.controller;

import com.medassist.service.WhatsAppService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhook")
public class WebhookController {

    private static final Logger logger = LoggerFactory.getLogger(WebhookController.class);

    @Autowired
    private WhatsAppService whatsAppService;

    @PostMapping("/whatsapp")
    public ResponseEntity<String> handleWhatsAppMessage(
            @RequestParam("From") String from,
            @RequestParam("Body") String body,
            @RequestParam(value = "MediaUrl0", required = false) String mediaUrl
    ) {
        try {
            logger.info("Received WhatsApp webhook - From: {}, Body: {}", from, body);

            whatsAppService.handleIncomingMessage(from, body);

            return ResponseEntity.ok("Message received");

        } catch (Exception e) {
            logger.error("Error processing WhatsApp message: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error processing message");
        }
    }

    @GetMapping("/whatsapp")
    public ResponseEntity<String> verifyWebhook() {
        return ResponseEntity.ok("Webhook is active");
    }
}
