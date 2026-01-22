package com.medassist.service;

import com.medassist.dto.*;
import com.medassist.entity.Appointment;
import com.medassist.entity.Conversation;
import com.medassist.entity.Message;
import com.medassist.entity.Patient;
import com.medassist.enums.AppointmentStatus;
import com.medassist.enums.ConversationStatus;
import com.medassist.enums.MessageRole;
import com.medassist.enums.TriageLevel;
import com.medassist.repository.AppointmentRepository;
import com.medassist.repository.ConversationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class WhatsAppService {

    private static final Logger logger = LoggerFactory.getLogger(WhatsAppService.class);

    // Patterns for parsing user input
    private static final Pattern SLOT_NUMBER_PATTERN = Pattern.compile("^([1-9])$");
    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})");
    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d{1,2}:\\d{2})");
    private static final Pattern CONFIRM_PATTERN = Pattern.compile("(?i)^confirm\\s+(.+)$");


    @Autowired
    private PatientRegistrationService registrationService;

    @Autowired
    private AIServiceClient aiServiceClient;

    @Autowired
    private TwilioService twilioService;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private AvailabilityService availabilityService;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private AppointmentReminderService reminderService;

    @Autowired
    private GoogleCalendarService googleCalendarService;

    @Transactional
    public void handleIncomingMessage(String fromPhone, String messageBody) {
        logger.info("Received WhatsApp message from {}: {}", fromPhone, messageBody);

        String normalizedPhone = normalizePhone(fromPhone);
        String messageLower = messageBody.trim().toLowerCase();

        // Step 1: Check if patient exists
        Patient patient = registrationService.findByPhone(normalizedPhone);

        if (patient == null) {
            // New patient - start registration process
            logger.info("New patient detected: {}", normalizedPhone);
            patient = registrationService.startRegistration(normalizedPhone);

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

        // Step 4: Check if patient is in booking flow
        if ("BOOKING_AWAITING_SLOT".equals(patient.getRegistrationStatus())) {
            handleSlotSelection(normalizedPhone, messageBody, patient);
            return;
        }

        if ("BOOKING_AWAITING_REASON".equals(patient.getRegistrationStatus())) {
            handleAppointmentReason(normalizedPhone, messageBody, patient);
            return;
        }

        // Step 5: Regular message processing for fully registered patients
        if (!"COMPLETE".equals(patient.getRegistrationStatus())) {
            // Safety check - shouldn't reach here, but handle gracefully
            logger.warn("Patient {} in unexpected registration status: {}",
                    patient.getId(), patient.getRegistrationStatus());
            twilioService.sendMessage(normalizedPhone, BotMessages.ERROR_MESSAGE);
            return;
        }

        // Check for appointment-related commands first
        if (isAppointmentCommand(messageLower)) {
            handleAppointmentCommand(normalizedPhone, messageLower, patient);
            return;
        }

        // Regular conversation flow
        Conversation conversation = getOrCreateConversation(patient);

        Message userMessage = Message.builder()
                .conversation(conversation)
                .role(MessageRole.USER)
                .content(messageBody)
                .build();
        conversation.addMessage(userMessage);
        conversationRepository.save(conversation);

        ConversationHistory conversationHistory = new ConversationHistory();
        conversationHistory.setContent("");
        conversationHistory.setTimestamp(LocalDateTime.now());
        conversationHistory.setRole("PATIENT");

        AIServiceRequest aiRequest = AIServiceRequest.builder()
                .message_id(UUID.randomUUID().toString())
                .patient_id(patient.getId().toString())
                .message(messageBody)
                .timestamp(LocalDateTime.now())
                .metadata(Map.of())
                .build();

        AIServiceResponse aiResponse = aiServiceClient.processMessage(aiRequest);

        // Check if AI detected scheduling intent
        if (containsSchedulingIntent(aiResponse)) {
            initiateBookingFlow(normalizedPhone, patient);
            return;
        }

        Message assistantMessage = Message.builder()
                .conversation(conversation)
                .role(MessageRole.ASSISTANT)
                .triageLevel(getTriageLevel(aiResponse.getTriageLevel()))
                .content(aiResponse.getResponse())
                .build();

        conversation.addMessage(assistantMessage);

        if (aiResponse.getTriageLevel() != null) {
            conversation.setTriageLevel(getTriageLevel(aiResponse.getTriageLevel()));
        } else {
            conversation.setTriageLevel(TriageLevel.HIGH);
        }

        conversationRepository.save(conversation);

        twilioService.sendMessage(normalizedPhone, aiResponse.getResponse());

        logger.info("Processed message for patient {} - Triage: {}",
                patient.getId(), aiResponse.getTriageLevel());
    }

    /**
     * Check if the message is an appointment-related command
     */
    private boolean isAppointmentCommand(String message) {
        return message.contains("book") ||
                message.contains("schedule") ||
                message.contains("appointment") ||
                message.contains("available") ||
                message.equals("cancel") ||
                message.equals("reschedule") ||
                message.startsWith("confirm");
    }

    /**
     * Handle appointment-related commands
     */
    private void handleAppointmentCommand(String phone, String message, Patient patient) {
        if (message.contains("book") || message.contains("schedule") || message.contains("appointment")) {
            initiateBookingFlow(phone, patient);
        } else if (message.contains("available") || message.equals("see available times")) {
            showAvailableSlots(phone, patient);
        } else if (message.equals("cancel")) {
            handleCancellation(phone, patient);
        } else if (message.equals("reschedule")) {
            handleReschedule(phone, patient);
        } else if (message.startsWith("confirm")) {
            handleConfirmation(phone, message, patient);
        }
    }

    /**
     * Check if AI response contains scheduling intent
     */
    private boolean containsSchedulingIntent(AIServiceResponse response) {
        if (response == null || response.getIntent() == null) {
            return false;
        }
        String intent = response.getIntent().toLowerCase();
        return intent.contains("book") ||
                intent.contains("schedule") ||
                intent.contains("appointment");
    }

    /**
     * Initiate the booking flow
     */
    private void initiateBookingFlow(String phone, Patient patient) {
        logger.info("Initiating booking flow for patient {}", patient.getId());

        // Update patient status to booking flow
        patient.setRegistrationStatus("BOOKING_AWAITING_SLOT");
        registrationService.updateStatus(patient);

        // Show available slots
        showAvailableSlots(phone, patient);
    }

    /**
     * Show available appointment slots
     */
    private void showAvailableSlots(String phone, Patient patient) {
        List<TimeSlot> slots = availabilityService.getNextAvailableSlots(5);

        if (slots.isEmpty()) {
            twilioService.sendMessage(phone, AppointmentNotificationMessages.NO_SLOTS_AVAILABLE);
            return;
        }

        String slotsDisplay = availabilityService.formatSlotsForDisplay(slots);
        String message = String.format(AppointmentNotificationMessages.AVAILABLE_SLOTS_MESSAGE, slotsDisplay);

        twilioService.sendMessage(phone, message);
    }

    /**
     * Handle slot selection during booking flow
     */
    private void handleSlotSelection(String phone, String messageBody, Patient patient) {
        logger.info("Processing slot selection from patient {}", patient.getId());

        String trimmedMessage = messageBody.trim();

        // Check if it's a slot number (1-9)
        Matcher slotMatcher = SLOT_NUMBER_PATTERN.matcher(trimmedMessage);
        if (slotMatcher.matches()) {
            int slotNumber = Integer.parseInt(slotMatcher.group(1));
            List<TimeSlot> slots = availabilityService.getNextAvailableSlots(5);

            if (slotNumber <= slots.size()) {
                TimeSlot selectedSlot = slots.get(slotNumber - 1);

                // Store selected slot temporarily (we'll use patient status with data)
                patient.setRegistrationStatus("BOOKING_AWAITING_REASON");
                // Store slot info in a way we can retrieve - using a simple approach
                // In production, you might use a separate booking session table
                String slotData = selectedSlot.getDate() + "T" + selectedSlot.getStartTime();
                patient.setLastName(patient.getLastName() + "|SLOT:" + slotData);
                registrationService.updateStatus(patient);

                twilioService.sendMessage(phone, AppointmentNotificationMessages.ASK_APPOINTMENT_REASON);
                return;
            }
        }

        // Try to parse a specific date/time
        LocalDate date = null;
        LocalTime time = null;

        Matcher dateMatcher = DATE_PATTERN.matcher(trimmedMessage);
        if (dateMatcher.find()) {
            try {
                date = LocalDate.parse(dateMatcher.group(1));
            } catch (DateTimeParseException e) {
                logger.warn("Failed to parse date from: {}", trimmedMessage);
            }
        }

        Matcher timeMatcher = TIME_PATTERN.matcher(trimmedMessage);
        if (timeMatcher.find()) {
            try {
                time = LocalTime.parse(timeMatcher.group(1));
            } catch (DateTimeParseException e) {
                logger.warn("Failed to parse time from: {}", trimmedMessage);
            }
        }

        if (date != null && time != null) {
            // Verify slot is available
            if (availabilityService.isSlotAvailable(date, time, phone)) {
                patient.setRegistrationStatus("BOOKING_AWAITING_REASON");
                String slotData = date + "T" + time;
                patient.setLastName(patient.getLastName() + "|SLOT:" + slotData);
                registrationService.updateStatus(patient);

                twilioService.sendMessage(phone, AppointmentNotificationMessages.ASK_APPOINTMENT_REASON);
            } else {
                List<TimeSlot> altSlots = availabilityService.getNextAvailableSlots(5);
                String altSlotsDisplay = availabilityService.formatSlotsForDisplay(altSlots);
                String message = String.format(AppointmentNotificationMessages.SLOT_ALREADY_BOOKED, altSlotsDisplay);
                twilioService.sendMessage(phone, message);
            }
            return;
        }

        // Invalid input - show slots again
        twilioService.sendMessage(phone, "Please select a slot number (1-5) or provide a specific date and time (e.g., 2026-01-25 10:30)");
    }

    /**
     * Handle appointment reason input
     */
    private void handleAppointmentReason(String phone, String messageBody, Patient patient) {
        logger.info("Processing appointment reason from patient {}", patient.getId());

        String reason = messageBody.trim();

        // Extract slot data from patient's lastName (temporary storage)
        String lastName = patient.getLastName();
        String slotData = null;
        String originalLastName = lastName;

        if (lastName != null && lastName.contains("|SLOT:")) {
            int slotIndex = lastName.indexOf("|SLOT:");
            slotData = lastName.substring(slotIndex + 6);
            originalLastName = lastName.substring(0, slotIndex);
        }

        if (slotData == null) {
            logger.error("No slot data found for patient {}", patient.getId());
            patient.setRegistrationStatus("COMPLETE");
            patient.setLastName(originalLastName);
            registrationService.updateStatus(patient);
            twilioService.sendMessage(phone, BotMessages.ERROR_MESSAGE);
            return;
        }

        try {
            // Parse slot data
            String[] parts = slotData.split("T");
            LocalDate day = LocalDate.parse(parts[0]);
            LocalTime time = LocalTime.parse(parts[1]);

            // Create the appointment
            Appointment appointment = Appointment.builder()
                    .day(day)
                    .time(time)
                    .phone(phone)
                    .patientName(patient.getFirstName() + " " + originalLastName)
                    .reason(reason)
                    .status(AppointmentStatus.SCHEDULED)
                    .clinicId(patient.getClinic().getId())
                    .build();

            appointmentRepository.save(appointment);

            // Create Google Calendar event if service is available
            if (googleCalendarService != null) {
                String googleEventId = googleCalendarService.createCalendarEvent(appointment);
                if (googleEventId != null) {
                    appointment.setGoogleEventId(googleEventId);
                    appointmentRepository.save(appointment);
                    logger.info("Google Calendar event created: {}", googleEventId);
                }
            }

            // Reset patient status
            patient.setRegistrationStatus("COMPLETE");
            patient.setLastName(originalLastName);
            registrationService.updateStatus(patient);

            // Send confirmation
            reminderService.sendBookingConfirmation(appointment);

            logger.info("Appointment created for patient {} on {} at {}",
                    patient.getId(), day, time);

        } catch (Exception e) {
            logger.error("Failed to create appointment for patient {}: {}",
                    patient.getId(), e.getMessage());
            patient.setRegistrationStatus("COMPLETE");
            patient.setLastName(originalLastName);
            registrationService.updateStatus(patient);
            twilioService.sendMessage(phone, BotMessages.ERROR_MESSAGE);
        }
    }

    /**
     * Handle appointment cancellation
     */
    private void handleCancellation(String phone, Patient patient) {
        List<Appointment> appointments = appointmentRepository
                .findUpcomingByPhone(phone, LocalDate.now(), AppointmentStatus.SCHEDULED);

        if (appointments.isEmpty()) {
            twilioService.sendMessage(phone, "You don't have any upcoming appointments to cancel.");
            return;
        }

        // Cancel the next upcoming appointment
        Appointment appointment = appointments.get(0);
        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancelledAt(LocalDateTime.now());
        appointmentRepository.save(appointment);

        // Delete Google Calendar event if exists
        if (googleCalendarService != null && appointment.getGoogleEventId() != null) {
            googleCalendarService.deleteCalendarEvent(appointment.getGoogleEventId());
            logger.info("Google Calendar event deleted: {}", appointment.getGoogleEventId());
        }

        reminderService.sendCancellationNotification(appointment);

        logger.info("Appointment cancelled for patient {}", patient.getId());
    }

    /**
     * Handle appointment reschedule
     */
    private void handleReschedule(String phone, Patient patient) {
        List<Appointment> appointments = appointmentRepository
                .findUpcomingByPhone(phone, LocalDate.now(), AppointmentStatus.SCHEDULED);

        if (appointments.isEmpty()) {
            twilioService.sendMessage(phone, "You don't have any upcoming appointments to reschedule. Would you like to book a new one? Reply 'BOOK'.");
            return;
        }

        // Cancel current and start new booking
        Appointment appointment = appointments.get(0);
        appointment.setStatus(AppointmentStatus.RESCHEDULED);
        appointmentRepository.save(appointment);

        // Delete Google Calendar event if exists
        if (googleCalendarService != null && appointment.getGoogleEventId() != null) {
            googleCalendarService.deleteCalendarEvent(appointment.getGoogleEventId());
            logger.info("Google Calendar event deleted for reschedule: {}", appointment.getGoogleEventId());
        }

        twilioService.sendMessage(phone, "Your current appointment has been marked for rescheduling. Let's find a new time.");
        initiateBookingFlow(phone, patient);
    }

    /**
     * Handle direct confirmation command
     */
    private void handleConfirmation(String phone, String message, Patient patient) {
        Matcher matcher = CONFIRM_PATTERN.matcher(message);
        if (matcher.matches()) {
            String confirmData = matcher.group(1).trim();

            // Try to parse date and time from confirmation
            LocalDate date = null;
            LocalTime time = null;

            Matcher dateMatcher = DATE_PATTERN.matcher(confirmData);
            if (dateMatcher.find()) {
                try {
                    date = LocalDate.parse(dateMatcher.group(1));
                } catch (DateTimeParseException e) {
                    logger.warn("Failed to parse date from confirm: {}", confirmData);
                }
            }

            Matcher timeMatcher = TIME_PATTERN.matcher(confirmData);
            if (timeMatcher.find()) {
                try {
                    time = LocalTime.parse(timeMatcher.group(1));
                } catch (DateTimeParseException e) {
                    logger.warn("Failed to parse time from confirm: {}", confirmData);
                }
            }

            if (date != null && time != null) {
                if (availabilityService.isSlotAvailable(date, time, phone)) {
                    // Create appointment directly
                    Appointment appointment = Appointment.builder()
                            .day(date)
                            .time(time)
                            .phone(phone)
                            .patientName(patient.getFirstName() + " " + patient.getLastName())
                            .reason("Booked via WhatsApp")
                            .status(AppointmentStatus.SCHEDULED)
                            .clinicId(patient.getClinic().getId())
                            .build();

                    appointmentRepository.save(appointment);

                    // Create Google Calendar event if service is available
                    if (googleCalendarService != null) {
                        String googleEventId = googleCalendarService.createCalendarEvent(appointment);
                        if (googleEventId != null) {
                            appointment.setGoogleEventId(googleEventId);
                            appointmentRepository.save(appointment);
                            logger.info("Google Calendar event created: {}", googleEventId);
                        }
                    }

                    reminderService.sendBookingConfirmation(appointment);

                    logger.info("Quick booking created for patient {} on {} at {}",
                            patient.getId(), date, time);
                } else {
                    twilioService.sendMessage(phone, "Sorry, that slot is no longer available. Reply 'BOOK' to see available times.");
                }
            } else {
                twilioService.sendMessage(phone, "Please use the format: confirm 2026-01-25 10:30");
            }
        }
    }

    public TriageLevel getTriageLevel(String level) {
        if (level == null) return TriageLevel.LOW;

        return switch (level.toLowerCase()) {
            case "low" -> TriageLevel.LOW;
            case "medium" -> TriageLevel.MEDIUM;
            case "high" -> TriageLevel.HIGH;
            case "critical" -> TriageLevel.CRITICAL;
            default -> TriageLevel.LOW;
        };
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
