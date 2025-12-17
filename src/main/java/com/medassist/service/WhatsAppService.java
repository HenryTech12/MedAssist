package com.medassist.service;

import com.medassist.dto.AIServiceRequest;
import com.medassist.dto.AIServiceResponse;
import com.medassist.dto.BotMessages;
import com.medassist.entity.Clinic;
import com.medassist.entity.Conversation;
import com.medassist.entity.Message;
import com.medassist.entity.Patient;
import com.medassist.enums.ConversationStatus;
import com.medassist.enums.MessageRole;
import com.medassist.repository.ConversationRepository;
import com.medassist.repository.PatientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class WhatsAppService {

    private static final Logger logger = LoggerFactory.getLogger(WhatsAppService.class);

    @Autowired
    private PatientService patientService;

    @Autowired
    private AIServiceClient aiServiceClient;

    @Autowired
    private TwilioService twilioService;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Transactional
    public void handleIncomingMessage(String fromPhone, String messageBody) {
        logger.info("Received WhatsApp message from {}: {}", fromPhone, messageBody);

        String normalizedPhone = normalizePhone(fromPhone);

        Patient patient = patientService.createOrGetPatient(normalizedPhone, getDefaultClinicId());

        if(patient == null) {
            twilioService.sendMessage(normalizedPhone, BotMessages.WELCOME_MESSAGE);
        }
        else if(patientService.completeRegistration(patient) == null) {
           if(messageBody.contains("1")) {
               Clinic clinic = patientService.getClinic();
               Patient updatePatientData = Patient.builder()
                       .clinic(clinic)
                       .phone(fromPhone)
                       .registeredAt(LocalDateTime.now())
                       .build();
               communicate(normalizedPhone,messageBody,updatePatientData);
           }
           else {
               twilioService.sendMessage(normalizedPhone,BotMessages.INVALID_CLINIC);
           }
        }
        else {
            communicate(normalizedPhone,messageBody,patient);
        }
    }

    public void communicate(String normalizedPhone, String messageBody, Patient patient) {
        Conversation conversation = getOrCreateConversation(patient);

        Message userMessage = Message.builder()
                .conversation(conversation)
                .role(MessageRole.USER)
                .content(messageBody)
                .build();
        conversation.addMessage(userMessage);
        patientRepository.save(patient);
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
