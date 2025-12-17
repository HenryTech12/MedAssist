package com.medassist.service;

import com.medassist.dto.AIServiceRequest;
import com.medassist.dto.AIServiceResponse;
import com.medassist.dto.BotMessages;
import com.medassist.entity.Conversation;
import com.medassist.entity.Message;
import com.medassist.entity.Patient;
import com.medassist.enums.ConversationStatus;
import com.medassist.enums.MessageRole;
import com.medassist.repository.ConversationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class WhatsAppService {

    private static final Logger logger = LoggerFactory.getLogger(WhatsAppService.class);

    @Autowired
    private PatientRegistrationService patientService;

    @Autowired
    private PatientRegistrationService registrationService;

    @Autowired
    private AIServiceClient aiServiceClient;

    @Autowired
    private TwilioService twilioService;

    @Autowired
    private ConversationRepository conversationRepository;

    @Transactional
    public void handleIncomingMessage(String fromPhone, String messageBody) {
        logger.info("Received WhatsApp message from {}: {}", fromPhone, messageBody);

        String normalizedPhone = normalizePhone(fromPhone);

        // Step 1: Check if patient exists
        Patient patient = registrationService.findByPhone(normalizedPhone);

        if (patient == null) {
            // New patient - start registration process
            logger.info("New patient detected: {}", normalizedPhone);
            registrationService.startRegistration(normalizedPhone);
            twilioService.sendMessage(normalizedPhone, BotMessages.WELCOME_MESSAGE);
            return;
        }

        // Step 2: Check if patient is pending clinic selection
        if ("PENDING_CLINIC".equals(patient.getRegistrationStatus())) {
            logger.info("Patient {} is selecting clinic", normalizedPhone);
            
            // Validate selection (must be 1, 2, or 3)
            if (!messageBody.trim().matches("[123]")) {
                twilioService.sendMessage(normalizedPhone, BotMessages.INVALID_CLINIC);
                return;
            }
            
            // Set clinic and move to name collection
            Patient updatedPatient = registrationService.setClinicSelection(patient, messageBody.trim());
            
            if (updatedPatient == null) {
                twilioService.sendMessage(normalizedPhone, BotMessages.INVALID_CLINIC);
                return;
            }
            
            // Ask for name
            twilioService.sendMessage(normalizedPhone, BotMessages.ASK_NAME);
            logger.info("Asked patient {} for name", updatedPatient.getId());
            return;
        }
        
        // Step 3: Check if patient is providing their name
        if ("AWAITING_NAME".equals(patient.getRegistrationStatus())) {
            logger.info("Patient {} is providing name", normalizedPhone);
            
            // Complete registration with name
            Patient completedPatient = registrationService.completRegistrationWithName(patient, messageBody.trim());
            
            // Send confirmation
            String confirmationMessage = String.format(
                BotMessages.REGISTRATION_COMPLETE,
                completedPatient.getClinic().getName()
            );
            twilioService.sendMessage(normalizedPhone, confirmationMessage);
            
            logger.info("Registration completed for patient {} ({} {}) - Clinic: {}", 
                       completedPatient.getId(),
                       completedPatient.getFirstName(),
                       completedPatient.getLastName(),
                       completedPatient.getClinic().getName());
            return;
        }

        // Step 4: Regular message processing for fully registered patients
        if (!"COMPLETE".equals(patient.getRegistrationStatus())) {
            // Safety check - shouldn't reach here, but handle gracefully
            logger.warn("Patient {} in unexpected registration status: {}", 
                       patient.getId(), patient.getRegistrationStatus());
            twilioService.sendMessage(normalizedPhone, BotMessages.ERROR_MESSAGE);
            return;
        }
        
        Conversation conversation = getOrCreateConversation(patient);

        Message userMessage = Message.builder()
                .conversation(conversation)
                .role(MessageRole.USER)
                .content(messageBody)
                .build();
        conversation.addMessage(userMessage);
        conversationRepository.save(conversation);

        AIServiceRequest aiRequest = AIServiceRequest.builder()
                .messageId(UUID.randomUUID().toString())
                .patientId(patient.getId().toString())
                .sessionId(conversation.getSessionId())
                .message(messageBody)
                .build();

        AIServiceResponse aiResponse = aiServiceClient.processMessage(aiRequest);

        Message assistantMessage = Message.builder()
                .conversation(conversation)
                .role(MessageRole.ASSISTANT)
                .content(aiResponse.getResponse())
                .triageLevel(aiResponse.getTriageLevel())
                .build();
        conversation.addMessage(assistantMessage);

        if (aiResponse.getTriageLevel() != null) {
            conversation.setTriageLevel(aiResponse.getTriageLevel());
        }

        conversationRepository.save(conversation);

        twilioService.sendMessage(normalizedPhone, aiResponse.getResponse());

        logger.info("Processed message for patient {} - Triage: {}",
                patient.getId(), aiResponse.getTriageLevel());
    }

    private String normalizePhone(String phone) {
        return phone.replace("whatsapp:", "").trim();
    }

    private Conversation getOrCreateConversation(Patient patient) {
        return conversationRepository.findByClinicIdAndPatientId(
                        patient.getClinic().getId(),
                        patient.getId()
                )
                .stream()
                .filter(c -> c.getStatus() == ConversationStatus.ACTIVE)
                .findFirst()
                .orElseGet(() -> {
                    Conversation newConversation = Conversation.builder()
                            .patient(patient)
                            .clinic(patient.getClinic())
                            .sessionId(UUID.randomUUID().toString())
                            .status(ConversationStatus.ACTIVE)
                            .build();
                    return conversationRepository.save(newConversation);
                });
    }

    private String getDefaultClinicId() {
        return "00000000-0000-0000-0000-000000000001";
    }
}
