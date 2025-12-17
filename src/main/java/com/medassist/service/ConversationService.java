package com.medassist.service;

import com.medassist.dto.*;
import com.medassist.entity.Conversation;
import com.medassist.entity.Message;
import com.medassist.entity.Patient;
import com.medassist.enums.ConversationStatus;
import com.medassist.enums.MessageRole;
import com.medassist.enums.TriageLevel;
import com.medassist.exception.ForbiddenException;
import com.medassist.exception.NotFoundException;
import com.medassist.repository.ConversationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ConversationService {

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private TwilioService twilioService;

    public List<ConversationDTO> getConversations(String clinicId, String status, String triageLevel) {
        Specification<Conversation> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("clinic").get("id"), UUID.fromString(clinicId)));

            if (status != null && !status.isEmpty()) {
                predicates.add(cb.equal(root.get("status"), ConversationStatus.valueOf(status.toUpperCase())));
            }

            if (triageLevel != null && !triageLevel.isEmpty()) {
                predicates.add(cb.equal(root.get("triageLevel"), TriageLevel.valueOf(triageLevel.toUpperCase())));
            }

            query.orderBy(cb.desc(root.get("lastMessageAt")));

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        List<Conversation> conversations = conversationRepository.findAll(spec);

        return conversations.stream()
                .map(this::toConversationDTO)
                .collect(Collectors.toList());
    }

    public ConversationDetailDTO getConversationDetail(String clinicId, String conversationId) {
        UUID convId = UUID.fromString(conversationId);
        UUID clinId = UUID.fromString(clinicId);

        Conversation conversation = conversationRepository.findByIdAndClinicIdWithMessages(convId, clinId)
                .orElseThrow(() -> new NotFoundException("Conversation not found"));

        return toConversationDetailDTO(conversation);
    }

    @Transactional
    public SendMessageResponse sendMessage(String clinicId, SendMessageRequest request) {
        UUID convId = UUID.fromString(request.getConversationId());
        UUID clinId = UUID.fromString(clinicId);

        Conversation conversation = conversationRepository.findByIdAndClinicId(convId, clinId)
                .orElseThrow(() -> new ForbiddenException("Access denied to this conversation"));

        String twilioSid = twilioService.sendMessage(
                conversation.getPatient().getPhone(),
                request.getMessage()
        );

        Message message = Message.builder()
                .conversation(conversation)
                .role(MessageRole.ADMIN)
                .content(request.getMessage())
                .build();

        conversation.addMessage(message);
        conversationRepository.save(conversation);

        return SendMessageResponse.builder()
                .success(true)
                .messageId(message.getId().toString())
                .twilioSid(twilioSid)
                .build();
    }

    @Transactional
    public void updateConversationStatus(String clinicId, String conversationId, ConversationStatus status) {
        UUID convId = UUID.fromString(conversationId);
        UUID clinId = UUID.fromString(clinicId);

        Conversation conversation = conversationRepository.findByIdAndClinicId(convId, clinId)
                .orElseThrow(() -> new ForbiddenException("Access denied to this conversation"));

        conversation.setStatus(status);
        conversationRepository.save(conversation);
    }

    private ConversationDTO toConversationDTO(Conversation conversation) {
        Patient patient = conversation.getPatient();
        String patientName = (patient.getFirstName() != null ? patient.getFirstName() : "") +
                (patient.getLastName() != null ? " " + patient.getLastName() : "");

        String preview = conversation.getMessages().isEmpty() ? "" :
                conversation.getMessages().get(conversation.getMessages().size() - 1).getContent();
        if (preview.length() > 100) {
            preview = preview.substring(0, 100) + "...";
        }

        return ConversationDTO.builder()
                .id(conversation.getId().toString())
                .patientId(patient.getId().toString())
                .patientName(patientName.trim().isEmpty() ? patient.getPhone() : patientName.trim())
                .patientPhone(patient.getPhone())
                .triageLevel(conversation.getTriageLevel())
                .status(conversation.getStatus())
                .lastMessageAt(conversation.getLastMessageAt())
                .messageCount(conversation.getMessages().size())
                .preview(preview)
                .createdAt(conversation.getCreatedAt())
                .build();
    }

    private ConversationDetailDTO toConversationDetailDTO(Conversation conversation) {
        Patient patient = conversation.getPatient();

        PatientDTO patientDTO = PatientDTO.builder()
                .id(patient.getId().toString())
                .phone(patient.getPhone())
                .firstName(patient.getFirstName())
                .lastName(patient.getLastName())
                .clinicId(patient.getClinic().getId().toString())
                .registeredAt(patient.getRegisteredAt())
                .build();

        List<MessageDTO> messageDTOs = conversation.getMessages().stream()
                .map(msg -> MessageDTO.builder()
                        .id(msg.getId().toString())
                        .role(msg.getRole())
                        .content(msg.getContent())
                        .triageLevel(msg.getTriageLevel())
                        .timestamp(msg.getTimestamp())
                        .build())
                .collect(Collectors.toList());

        return ConversationDetailDTO.builder()
                .id(conversation.getId().toString())
                .patient(patientDTO)
                .triageLevel(conversation.getTriageLevel())
                .status(conversation.getStatus())
                .messages(messageDTOs)
                .createdAt(conversation.getCreatedAt())
                .lastMessageAt(conversation.getLastMessageAt())
                .build();
    }
}
