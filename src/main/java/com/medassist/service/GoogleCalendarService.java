package com.medassist.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.EventReminder;
import com.google.api.services.calendar.model.Events;
import com.medassist.entity.Appointment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Service for integrating with Google Calendar API
 * Handles creating, updating, and deleting calendar events for appointments
 */
@Service
@Slf4j
public class GoogleCalendarService {

    private static final String APPLICATION_NAME = "MedAssist Appointment System";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final List<String> SCOPES = Collections.singletonList(CalendarScopes.CALENDAR);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

    @Value("${google.calendar.enabled:false}")
    private boolean calendarEnabled;

    @Value("${google.calendar.id:primary}")
    private String calendarId;

    @Value("${app.timezone:America/New_York}")
    private String timezone;

    private Calendar calendarService;

    @PostConstruct
    public void init() {
        if (calendarEnabled) {
            try {
                final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
                calendarService = new Calendar.Builder(httpTransport, JSON_FACTORY, getCredentials(httpTransport))
                        .setApplicationName(APPLICATION_NAME)
                        .build();
                log.info("Google Calendar service initialized successfully");
            } catch (Exception e) {
                log.error("Failed to initialize Google Calendar service: {}", e.getMessage());
                calendarEnabled = false;
            }
        } else {
            log.info("Google Calendar integration is disabled");
        }
    }

    /**
     * Creates an authorization credential for Google Calendar API
     */
    private Credential getCredentials(final NetHttpTransport httpTransport) throws IOException {
        InputStream in = GoogleCalendarService.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();

        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    /**
     * Create a Google Calendar event for an appointment
     * @return The Google Calendar event ID, or null if creation failed
     */
    public String createCalendarEvent(Appointment appointment) {
        if (!calendarEnabled || calendarService == null) {
            log.debug("Google Calendar is disabled, skipping event creation");
            return null;
        }

        try {
            Event event = buildEventFromAppointment(appointment);

            Event createdEvent = calendarService.events()
                    .insert(calendarId, event)
                    .execute();

            log.info("Created Google Calendar event: {} for appointment ID: {}",
                    createdEvent.getId(), appointment.getId());

            return createdEvent.getId();

        } catch (IOException e) {
            log.error("Failed to create Google Calendar event for appointment {}: {}",
                    appointment.getId(), e.getMessage());
            return null;
        }
    }

    /**
     * Update an existing Google Calendar event
     */
    public boolean updateCalendarEvent(Appointment appointment) {
        if (!calendarEnabled || calendarService == null) {
            return false;
        }

        String eventId = appointment.getGoogleEventId();
        if (eventId == null || eventId.isEmpty()) {
            log.warn("No Google Calendar event ID found for appointment {}", appointment.getId());
            return false;
        }

        try {
            Event event = buildEventFromAppointment(appointment);

            calendarService.events()
                    .update(calendarId, eventId, event)
                    .execute();

            log.info("Updated Google Calendar event: {} for appointment ID: {}",
                    eventId, appointment.getId());
            return true;

        } catch (IOException e) {
            log.error("Failed to update Google Calendar event {} for appointment {}: {}",
                    eventId, appointment.getId(), e.getMessage());
            return false;
        }
    }

    /**
     * Delete a Google Calendar event
     */
    public boolean deleteCalendarEvent(String eventId) {
        if (!calendarEnabled || calendarService == null) {
            return false;
        }

        if (eventId == null || eventId.isEmpty()) {
            return false;
        }

        try {
            calendarService.events()
                    .delete(calendarId, eventId)
                    .execute();

            log.info("Deleted Google Calendar event: {}", eventId);
            return true;

        } catch (IOException e) {
            log.error("Failed to delete Google Calendar event {}: {}", eventId, e.getMessage());
            return false;
        }
    }

    /**
     * Get available time slots from Google Calendar for a specific date
     */
    public List<LocalTime> getAvailableSlots(LocalDate date, int slotDurationMinutes) {
        if (!calendarEnabled || calendarService == null) {
            return Collections.emptyList();
        }

        try {
            // Define working hours
            LocalTime workStart = LocalTime.of(9, 0);
            LocalTime workEnd = LocalTime.of(17, 0);

            // Get busy times from calendar
            ZonedDateTime startOfDay = date.atTime(workStart).atZone(ZoneId.of(timezone));
            ZonedDateTime endOfDay = date.atTime(workEnd).atZone(ZoneId.of(timezone));

            Events events = calendarService.events()
                    .list(calendarId)
                    .setTimeMin(new DateTime(startOfDay.toInstant().toEpochMilli()))
                    .setTimeMax(new DateTime(endOfDay.toInstant().toEpochMilli()))
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute();

            // Calculate available slots (simplified - in production use FreeBusy API)
            List<Event> busyEvents = events.getItems();

            // For now, return all slots and let AvailabilityService handle conflicts
            return Collections.emptyList();

        } catch (IOException e) {
            log.error("Failed to get available slots from Google Calendar: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Build a Google Calendar Event from an Appointment
     */
    private Event buildEventFromAppointment(Appointment appointment) {
        Event event = new Event();

        // Set event summary and description
        String summary = String.format("Medical Appointment - %s",
                appointment.getPatientName() != null ? appointment.getPatientName() : "Patient");
        event.setSummary(summary);

        String description = buildEventDescription(appointment);
        event.setDescription(description);

        // Set event times
        LocalDateTime startDateTime = LocalDateTime.of(appointment.getDay(), appointment.getTime());
        int duration = appointment.getDurationMinutes() != null ? appointment.getDurationMinutes() : 30;
        LocalDateTime endDateTime = startDateTime.plusMinutes(duration);

        ZonedDateTime zonedStart = startDateTime.atZone(ZoneId.of(timezone));
        ZonedDateTime zonedEnd = endDateTime.atZone(ZoneId.of(timezone));

        EventDateTime start = new EventDateTime()
                .setDateTime(new DateTime(zonedStart.toInstant().toEpochMilli()))
                .setTimeZone(timezone);
        event.setStart(start);

        EventDateTime end = new EventDateTime()
                .setDateTime(new DateTime(zonedEnd.toInstant().toEpochMilli()))
                .setTimeZone(timezone);
        event.setEnd(end);

        // Set reminders
        Event.Reminders reminders = new Event.Reminders()
                .setUseDefault(false)
                .setOverrides(Arrays.asList(
                        new EventReminder().setMethod("email").setMinutes(24 * 60), // 24 hours before
                        new EventReminder().setMethod("popup").setMinutes(120),      // 2 hours before
                        new EventReminder().setMethod("popup").setMinutes(30)        // 30 minutes before
                ));
        event.setReminders(reminders);

        return event;
    }

    /**
     * Build event description from appointment details
     */
    private String buildEventDescription(Appointment appointment) {
        StringBuilder sb = new StringBuilder();
        sb.append("Appointment Details:\n\n");

        if (appointment.getPatientName() != null) {
            sb.append("Patient: ").append(appointment.getPatientName()).append("\n");
        }
        if (appointment.getPhone() != null) {
            sb.append("Phone: ").append(appointment.getPhone()).append("\n");
        }
        if (appointment.getReason() != null) {
            sb.append("Reason: ").append(appointment.getReason()).append("\n");
        }

        sb.append("\nBooked via MedAssist WhatsApp");

        return sb.toString();
    }

    /**
     * Check if Google Calendar integration is enabled and working
     */
    public boolean isEnabled() {
        return calendarEnabled && calendarService != null;
    }
}
