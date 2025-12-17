package com.medassist.service;

import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TwilioService {

    private static final Logger logger = LoggerFactory.getLogger(TwilioService.class);

    @Value("${twilio.whatsapp-number}")
    private String twilioWhatsAppNumber;

    public String sendMessage(String toPhone, String messageBody) {
        try {
            String toWhatsApp = toPhone.startsWith("whatsapp:") ? toPhone : "whatsapp:" + toPhone;
            String fromWhatsApp = twilioWhatsAppNumber.startsWith("whatsapp:") ?
                twilioWhatsAppNumber : "whatsapp:" + twilioWhatsAppNumber;

            Message message = Message.creator(
                    new PhoneNumber(toWhatsApp),
                    new PhoneNumber(fromWhatsApp),
                    messageBody
            ).create();

            logger.info("WhatsApp message sent to {}: {}", toPhone, message.getSid());
            return message.getSid();

        } catch (Exception e) {
            logger.error("Failed to send WhatsApp message to {}: {}", toPhone, e.getMessage());
            throw new RuntimeException("Failed to send WhatsApp message: " + e.getMessage());
        }
    }
}
