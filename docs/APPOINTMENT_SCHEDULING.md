# Appointment Scheduling Implementation Guide

## Overview

MedAssist's Appointment Scheduling module provides a comprehensive solution for managing medical appointments through multiple channels: WhatsApp, web dashboard, and mobile applications. This document outlines the implementation details for the complete appointment scheduling system.

### Prerequisites

- **Java 17** or higher (required for switch expressions and text blocks used in code examples)
- Spring Boot 3.4.x
- MySQL database
- Twilio account (for WhatsApp/SMS integration)

---

## Table of Contents

1. [AI-Assisted Booking](#1-ai-assisted-booking)
2. [Availability Management & Double-Booking Prevention](#2-availability-management--double-booking-prevention)
3. [Cancellations, Reschedules & Recurring Appointments](#3-cancellations-reschedules--recurring-appointments)
4. [Google Calendar Integration](#4-google-calendar-integration)
5. [Automated Reminders](#5-automated-reminders)
6. [API Reference](#6-api-reference)
7. [Data Models](#7-data-models)
8. [Configuration](#8-configuration)

---

## 1. AI-Assisted Booking

### 1.1 WhatsApp Booking

Patients can book appointments directly through WhatsApp conversations using natural language processing.

#### Flow:
1. Patient sends a message expressing intent to book an appointment
2. AI service (`AIServiceClient`) processes the message and extracts scheduling intent
3. System presents available time slots
4. Patient confirms preferred slot
5. Appointment is created and confirmation sent

#### Implementation Details:

```java
// WhatsAppService handles incoming messages
@Service
public class WhatsAppService {
    
    @Autowired
    private AIServiceClient aiServiceClient;
    
    @Autowired
    private AppointmentService appointmentService;
    
    @Transactional
    public void handleIncomingMessage(String fromPhone, String messageBody) {
        // AI processes message to detect scheduling intent
        AIServiceRequest aiRequest = AIServiceRequest.builder()
            .message_id(UUID.randomUUID().toString())
            .patient_id(patientId)
            .message(messageBody)
            .timestamp(LocalDateTime.now())
            .build();
            
        AIServiceResponse response = aiServiceClient.processMessage(aiRequest);
        
        // If scheduling intent detected, extract appointment details
        if (response.containsSchedulingIntent()) {
            // Process booking request
        }
    }
}
```

#### WhatsApp Commands:
| Command | Description |
|---------|-------------|
| "Book appointment" | Initiates booking flow |
| "Schedule visit" | Alternative trigger phrase |
| "See available times" | Lists available slots |
| "Confirm [date/time]" | Confirms selected slot |

### 1.2 Dashboard Booking

Staff can create appointments through the web dashboard.

#### Endpoint:
```
POST /api/appointments
```

#### Request Body:
```json
{
    "day": "2025-01-15",
    "time": "10:30:00",
    "reason": "General checkup",
    "phone": "+1234567890",
    "patientName": "John Doe"
}
```

### 1.3 Mobile App Booking

The mobile application uses the same REST API endpoints as the dashboard:

```
POST /api/appointments
GET /api/appointments
PUT /api/appointments/reschedule
DELETE /api/appointments/cancel
```

---

## 2. Availability Management & Double-Booking Prevention

### 2.1 Checking Doctor/Clinic Availability

The system maintains availability schedules for doctors and clinics.

#### Recommended Implementation:

```java
@Entity
@Table(name = "doctor_schedules")
public class DoctorSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private UUID doctorId;
    private DayOfWeek dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer slotDurationMinutes; // e.g., 30 minutes
    private Boolean isAvailable;
}
```

#### Availability Check Service:

```java
@Service
public class AvailabilityService {
    
    @Autowired
    private AppointmentRepository appointmentRepository;
    
    @Autowired
    private DoctorScheduleRepository scheduleRepository;
    
    public List<TimeSlot> getAvailableSlots(UUID doctorId, LocalDate date) {
        // 1. Get doctor's schedule for the day
        DoctorSchedule schedule = scheduleRepository
            .findByDoctorIdAndDayOfWeek(doctorId, date.getDayOfWeek());
        
        // 2. Get existing appointments
        List<Appointment> existingAppointments = appointmentRepository
            .findByDoctorIdAndDay(doctorId, date);
        
        // 3. Calculate available slots
        return calculateAvailableSlots(schedule, existingAppointments);
    }
    
    private List<TimeSlot> calculateAvailableSlots(
            DoctorSchedule schedule, 
            List<Appointment> existingAppointments) {
        // Generate all possible slots based on schedule
        // Remove slots that conflict with existing appointments
        // Return available slots
    }
}
```

### 2.2 Double-Booking Prevention

The system implements multiple layers of protection against double-booking:

#### Database Constraint:
```sql
ALTER TABLE appointments 
ADD CONSTRAINT unique_appointment 
UNIQUE (doctor_id, day, time);
```

#### Service Layer Validation:
```java
@Service
public class AppointmentService {
    
    @Transactional
    public AppointmentDTO createAppointment(AppointmentDTO appointmentDTO) {
        // Check for existing appointment at same time
        boolean conflictExists = appointmentRepository
            .existsByDoctorIdAndDayAndTime(
                appointmentDTO.getDoctorId(),
                appointmentDTO.getDay(),
                appointmentDTO.getTime()
            );
        
        if (conflictExists) {
            throw new ConflictException("Time slot already booked");
        }
        
        // Proceed with booking
        Appointment appointment = new Appointment();
        // ... set fields
        appointmentRepository.save(appointment);
        
        return toAppointmentDTO(appointment);
    }
}
```

#### Optimistic Locking:
```java
@Entity
public class Appointment {
    @Version
    private Long version;
    
    // ... other fields
}
```

---

## 3. Cancellations, Reschedules & Recurring Appointments

### 3.1 Cancellation Flow

#### Endpoint:
```
DELETE /api/appointments/cancel
```

#### Implementation:
```java
public Map<String, Object> cancelAppointment(String reason, String phone) {
    // 1. Find appointment
    Appointment appointment = appointmentRepository
        .findByPhone(phone)
        .orElseThrow(() -> new NotFoundException("Appointment not found"));
    
    // 2. Update status (soft delete recommended)
    appointment.setStatus(AppointmentStatus.CANCELLED);
    appointment.setCancellationReason(reason);
    appointment.setCancelledAt(LocalDateTime.now());
    appointmentRepository.save(appointment);
    
    // 3. Send notification
    notificationService.sendNotifications(
        AppointmentNotificationMessages.APPOINTMENT_CANCELLED
    );
    
    // 4. Free up the time slot (automatic with status change)
    
    return Map.of(
        "status", AppointmentStatus.CANCELLED,
        "reason", reason
    );
}
```

### 3.2 Rescheduling Flow

#### Endpoint:
```
PUT /api/appointments/reschedule
```

#### Implementation:
```java
public Map<String, Object> rescheduleAppointment(
        String reason, 
        AppointmentRequest appointmentRequest) {
    
    // 1. Validate new time slot is available
    validateSlotAvailability(appointmentRequest);
    
    // 2. Update appointment
    Appointment appointment = appointmentRepository
        .findByPhone(appointmentRequest.getPhone())
        .orElseThrow(() -> new NotFoundException("Appointment not found"));
    
    // Store previous time for notification
    LocalDate previousDay = appointment.getDay();
    LocalTime previousTime = appointment.getTime();
    
    // Update to new time
    appointment.setDay(appointmentRequest.getDay());
    appointment.setTime(appointmentRequest.getTime());
    appointment.setStatus(AppointmentStatus.RESCHEDULED);
    appointmentRepository.save(appointment);
    
    // 3. Send notification
    notificationService.sendNotifications(
        AppointmentNotificationMessages.APPOINTMENT_RESCHEDULED
    );
    
    return Map.of(
        "status", AppointmentStatus.RESCHEDULED,
        "reason", reason,
        "previousDate", previousDay,
        "previousTime", previousTime,
        "newDate", appointment.getDay(),
        "newTime", appointment.getTime()
    );
}
```

### 3.3 Recurring Appointments

For patients requiring regular visits (e.g., chronic conditions, therapy sessions):

#### Recurring Appointment Entity:
```java
@Entity
@Table(name = "recurring_appointments")
public class RecurringAppointment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private UUID patientId;
    private UUID doctorId;
    private String phone;
    
    @Enumerated(EnumType.STRING)
    private RecurrencePattern pattern; // WEEKLY, BIWEEKLY, MONTHLY
    
    private DayOfWeek preferredDay;
    private LocalTime preferredTime;
    private LocalDate startDate;
    private LocalDate endDate; // null for indefinite
    private Integer maxOccurrences;
    private String reason;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
```

#### Recurrence Patterns:
```java
public enum RecurrencePattern {
    DAILY,
    WEEKLY,
    BIWEEKLY,
    MONTHLY,
    CUSTOM
}
```

#### Recurring Appointment Service:
```java
@Service
public class RecurringAppointmentService {
    
    @Scheduled(cron = "0 0 1 * * *") // Run daily at 1 AM
    public void generateUpcomingAppointments() {
        LocalDate lookAheadDate = LocalDate.now().plusWeeks(2);
        
        List<RecurringAppointment> recurring = 
            recurringAppointmentRepository.findActive();
        
        for (RecurringAppointment ra : recurring) {
            generateAppointmentsUntil(ra, lookAheadDate);
        }
    }
    
    private void generateAppointmentsUntil(
            RecurringAppointment recurring, 
            LocalDate untilDate) {
        // Calculate next occurrence dates based on pattern
        // Create individual appointments for each occurrence
        // Skip dates where conflicts exist
    }
}
```

---

## 4. Google Calendar Integration

### 4.1 Overview

Optional integration with Google Calendar enables:
- Syncing staff schedules from Google Calendar
- Pushing appointments to staff calendars
- Real-time availability updates

### 4.2 Configuration

Add dependencies to `pom.xml`:
```xml
<dependency>
    <groupId>com.google.apis</groupId>
    <artifactId>google-api-services-calendar</artifactId>
    <version>v3-rev20231123-2.0.0</version>
</dependency>
<dependency>
    <groupId>com.google.oauth-client</groupId>
    <artifactId>google-oauth-client-jetty</artifactId>
    <version>1.34.1</version>
</dependency>
```

Add to `application.properties`:
```properties
# Google Calendar Integration
google.calendar.enabled=false
google.calendar.client-id=${GOOGLE_CLIENT_ID}
google.calendar.client-secret=${GOOGLE_CLIENT_SECRET}
google.calendar.redirect-uri=http://localhost:8080/oauth/callback
```

### 4.3 Google Calendar Service

```java
@Service
@ConditionalOnProperty(name = "google.calendar.enabled", havingValue = "true")
public class GoogleCalendarService {
    
    private final Calendar calendarService;
    
    public GoogleCalendarService(
            @Value("${google.calendar.client-id}") String clientId,
            @Value("${google.calendar.client-secret}") String clientSecret) {
        // Initialize Google Calendar API client
        this.calendarService = initializeCalendarService(clientId, clientSecret);
    }
    
    /**
     * Sync events from Google Calendar to update staff availability
     */
    public void syncStaffSchedule(String staffEmail, String calendarId) {
        Events events = calendarService.events()
            .list(calendarId)
            .setTimeMin(new DateTime(System.currentTimeMillis()))
            .setTimeMax(new DateTime(getEndOfWeek()))
            .execute();
        
        // Process events to update availability
        for (Event event : events.getItems()) {
            updateStaffAvailability(staffEmail, event);
        }
    }
    
    /**
     * Create calendar event when appointment is booked
     */
    public String createAppointmentEvent(Appointment appointment, String calendarId) {
        Event event = new Event()
            .setSummary("Patient Appointment: " + appointment.getPatientName())
            .setDescription("Reason: " + appointment.getReason())
            .setStart(toEventDateTime(appointment.getDay(), appointment.getTime()))
            .setEnd(toEventDateTime(appointment.getDay(), 
                appointment.getTime().plusMinutes(30)));
        
        Event createdEvent = calendarService.events()
            .insert(calendarId, event)
            .execute();
        
        return createdEvent.getId();
    }
    
    /**
     * Delete calendar event when appointment is cancelled
     */
    public void deleteAppointmentEvent(String calendarId, String eventId) {
        calendarService.events()
            .delete(calendarId, eventId)
            .execute();
    }
    
    /**
     * Update calendar event when appointment is rescheduled
     */
    public void updateAppointmentEvent(
            String calendarId, 
            String eventId, 
            Appointment appointment) {
        Event event = calendarService.events()
            .get(calendarId, eventId)
            .execute();
        
        event.setStart(toEventDateTime(appointment.getDay(), appointment.getTime()));
        event.setEnd(toEventDateTime(appointment.getDay(), 
            appointment.getTime().plusMinutes(30)));
        
        calendarService.events()
            .update(calendarId, eventId, event)
            .execute();
    }
}
```

### 4.4 Webhook for Real-Time Updates

```java
@RestController
@RequestMapping("/api/calendar")
public class GoogleCalendarWebhookController {
    
    @Autowired
    private GoogleCalendarService calendarService;
    
    @PostMapping("/webhook")
    public ResponseEntity<Void> handleCalendarUpdate(
            @RequestHeader("X-Goog-Resource-ID") String resourceId,
            @RequestHeader("X-Goog-Channel-ID") String channelId) {
        
        // Process calendar change notification
        calendarService.handleCalendarUpdate(resourceId, channelId);
        
        return ResponseEntity.ok().build();
    }
}
```

---

## 5. Automated Reminders

### 5.1 Notification Channels

The system supports multiple notification channels:

| Channel | Service | Use Case |
|---------|---------|----------|
| WhatsApp | TwilioService | Primary communication |
| SMS | TwilioService | Fallback for WhatsApp |
| Email | EmailService | Formal communications |
| Push | WebSocket | Real-time dashboard updates |

### 5.2 Reminder Configuration

```java
@Configuration
public class NotificationConfig {
    
    @Bean
    public ReminderSchedule defaultReminderSchedule() {
        return ReminderSchedule.builder()
            .firstReminder(Duration.ofHours(24))  // 24 hours before
            .secondReminder(Duration.ofHours(2))  // 2 hours before
            .enableWhatsApp(true)
            .enableSms(true)
            .enableEmail(true)
            .build();
    }
}
```

### 5.3 Reminder Service Implementation

```java
@Service
@Slf4j
public class AppointmentReminderService {
    
    @Autowired
    private AppointmentRepository appointmentRepository;
    
    @Autowired
    private TwilioService twilioService;
    
    @Autowired
    private NotificationService notificationService;
    
    /**
     * Send reminders 24 hours before appointment
     */
    @Scheduled(cron = "0 0 9 * * *") // Run daily at 9 AM
    public void sendDayBeforeReminders() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        
        List<Appointment> appointments = appointmentRepository
            .findByDayAndStatus(tomorrow, AppointmentStatus.SCHEDULED);
        
        for (Appointment apt : appointments) {
            sendReminder(apt, ReminderType.DAY_BEFORE);
        }
        
        log.info("Sent {} day-before reminders", appointments.size());
    }
    
    /**
     * Send reminders 2 hours before appointment
     */
    @Scheduled(cron = "0 */30 * * * *") // Run every 30 minutes
    public void sendHourBeforeReminders() {
        LocalDateTime twoHoursFromNow = LocalDateTime.now().plusHours(2);
        LocalDate targetDate = twoHoursFromNow.toLocalDate();
        LocalTime targetTime = twoHoursFromNow.toLocalTime();
        
        List<Appointment> appointments = appointmentRepository
            .findAppointmentsInTimeRange(
                targetDate,
                targetTime.minusMinutes(15),
                targetTime.plusMinutes(15),
                AppointmentStatus.SCHEDULED
            );
        
        for (Appointment apt : appointments) {
            if (!apt.isReminderSent2Hours()) {
                sendReminder(apt, ReminderType.HOURS_BEFORE);
                apt.setReminderSent2Hours(true);
                appointmentRepository.save(apt);
            }
        }
    }
    
    private void sendReminder(Appointment appointment, ReminderType type) {
        String message = buildReminderMessage(appointment, type);
        
        // Send via WhatsApp
        try {
            twilioService.sendMessage(appointment.getPhone(), message);
            log.info("WhatsApp reminder sent to {}", appointment.getPhone());
        } catch (Exception e) {
            log.error("Failed to send WhatsApp reminder", e);
            // Fallback to SMS
            sendSmsReminder(appointment, message);
        }
        
        // Also send via dashboard notification
        notificationService.sendNotifications(message);
    }
    
    private String buildReminderMessage(Appointment apt, ReminderType type) {
        return switch (type) {
            case DAY_BEFORE -> String.format(
                "🏥 Appointment Reminder\n\n" +
                "Hi %s,\n\n" +
                "This is a reminder that you have an appointment tomorrow:\n" +
                "📅 Date: %s\n" +
                "⏰ Time: %s\n" +
                "📋 Reason: %s\n\n" +
                "Reply 'CONFIRM' to confirm or 'RESCHEDULE' to change your appointment.",
                apt.getPatientName(),
                apt.getDay().format(DateTimeFormatter.ofPattern("EEEE, MMMM d")),
                apt.getTime().format(DateTimeFormatter.ofPattern("h:mm a")),
                apt.getReason()
            );
            case HOURS_BEFORE -> String.format(
                "⏰ Appointment in 2 Hours\n\n" +
                "Hi %s,\n\n" +
                "Your appointment is in 2 hours:\n" +
                "⏰ Time: %s\n\n" +
                "Please arrive 10 minutes early.",
                apt.getPatientName(),
                apt.getTime().format(DateTimeFormatter.ofPattern("h:mm a"))
            );
        };
    }
}
```

### 5.4 Notification Messages

Define message templates in `AppointmentNotificationMessages`:

```java
public class AppointmentNotificationMessages {
    
    public static final String APPOINTMENT_SCHEDULED = """
        ✅ Appointment Confirmed!
        
        Your appointment has been scheduled.
        You will receive a reminder 24 hours before your visit.
        
        To reschedule or cancel, reply 'RESCHEDULE' or 'CANCEL'.
        """;
    
    public static final String APPOINTMENT_CANCELLED = """
        ❌ Appointment Cancelled
        
        Your appointment has been cancelled as requested.
        
        To book a new appointment, reply 'BOOK'.
        """;
    
    public static final String APPOINTMENT_RESCHEDULED = """
        🔄 Appointment Rescheduled
        
        Your appointment has been successfully rescheduled.
        You will receive updated confirmation details shortly.
        """;
}
```

---

## 6. API Reference

### Endpoints Summary

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/appointments` | Create new appointment |
| `GET` | `/api/appointments` | List all appointments |
| `GET` | `/api/appointments/{id}` | Get appointment by ID |
| `PUT` | `/api/appointments/reschedule` | Reschedule appointment |
| `DELETE` | `/api/appointments/cancel` | Cancel appointment |
| `POST` | `/api/appointments/notify` | Send custom notification |
| `GET` | `/api/availability/{doctorId}` | Get doctor availability |
| `POST` | `/api/recurring-appointments` | Create recurring appointment |

### Authentication

All endpoints require JWT authentication:
```
Authorization: Bearer <jwt_token>
```

### Response Codes

| Code | Description |
|------|-------------|
| 200 | Success |
| 201 | Created |
| 400 | Bad Request |
| 401 | Unauthorized |
| 404 | Not Found |
| 409 | Conflict (double booking) |
| 500 | Server Error |

---

## 7. Data Models

### Appointment Entity (Extended)

```java
@Entity
@Table(name = "appointments", indexes = {
    @Index(name = "idx_patient_phone", columnList = "phone"),
    @Index(name = "idx_appointment_date", columnList = "day"),
    @Index(name = "idx_doctor_date_time", columnList = "doctor_id, day, time")
})
public class Appointment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "doctor_id")
    private UUID doctorId;
    
    @Column(name = "clinic_id")
    private UUID clinicId;
    
    @Column(nullable = false)
    private LocalDate day;
    
    @Column(nullable = false)
    private LocalTime time;
    
    @Column(name = "duration_minutes")
    private Integer durationMinutes = 30;
    
    private String reason;
    
    @Column(nullable = false)
    private String phone;
    
    @Column(name = "patient_name")
    private String patientName;
    
    @Enumerated(EnumType.STRING)
    private AppointmentStatus status = AppointmentStatus.SCHEDULED;
    
    @Column(name = "cancellation_reason")
    private String cancellationReason;
    
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;
    
    @Column(name = "google_event_id")
    private String googleEventId;
    
    @Column(name = "recurring_appointment_id")
    private Long recurringAppointmentId;
    
    @Column(name = "reminder_sent_24h")
    private Boolean reminderSent24Hours = false;
    
    @Column(name = "reminder_sent_2h")
    private Boolean reminderSent2Hours = false;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Version
    private Long version;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

### Appointment Status

```java
public enum AppointmentStatus {
    SCHEDULED,      // Active appointment
    CONFIRMED,      // Patient confirmed
    RESCHEDULED,    // Time changed
    CANCELLED,      // Cancelled by patient/staff
    COMPLETED,      // Appointment completed
    NO_SHOW         // Patient didn't attend
}
```

---

## 8. Configuration

### Application Properties

```properties
# Appointment Settings
appointment.default-duration-minutes=30
appointment.max-advance-booking-days=90
appointment.min-cancellation-hours=2

# Reminder Settings
reminder.day-before.enabled=true
reminder.day-before.time=09:00
reminder.hours-before.enabled=true
reminder.hours-before.value=2

# Notification Channels
notification.whatsapp.enabled=true
notification.sms.enabled=true
notification.email.enabled=true

# Google Calendar (optional)
google.calendar.enabled=false
google.calendar.client-id=${GOOGLE_CLIENT_ID}
google.calendar.client-secret=${GOOGLE_CLIENT_SECRET}

# Twilio Configuration
twilio.account-sid=${TWILIO_ACCOUNT_SID}
twilio.auth-token=${TWILIO_AUTH_TOKEN}
twilio.whatsapp-number=${TWILIO_WHATSAPP_NUMBER}
```

### Environment Variables

| Variable | Description | Required |
|----------|-------------|----------|
| `TWILIO_ACCOUNT_SID` | Twilio Account SID | Yes |
| `TWILIO_AUTH_TOKEN` | Twilio Auth Token | Yes |
| `TWILIO_WHATSAPP_NUMBER` | WhatsApp Business Number | Yes |
| `GOOGLE_CLIENT_ID` | Google OAuth Client ID | No* |
| `GOOGLE_CLIENT_SECRET` | Google OAuth Client Secret | No* |

*Required only if Google Calendar integration is enabled.

---

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                        Client Applications                          │
├─────────────────┬─────────────────┬─────────────────────────────────┤
│    WhatsApp     │   Web Dashboard │      Mobile App                 │
└────────┬────────┴────────┬────────┴──────────┬──────────────────────┘
         │                 │                   │
         ▼                 ▼                   ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         API Gateway                                 │
│                    (Spring Security + JWT)                          │
└─────────────────────────────────────────────────────────────────────┘
         │                 │                   │
         ▼                 ▼                   ▼
┌─────────────────────────────────────────────────────────────────────┐
│                     Controller Layer                                │
├─────────────────┬─────────────────┬─────────────────────────────────┤
│ WebhookController│AppointmentController│  CalendarController        │
└────────┬────────┴────────┬────────┴──────────┬──────────────────────┘
         │                 │                   │
         ▼                 ▼                   ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      Service Layer                                  │
├───────────────┬───────────────┬────────────────┬────────────────────┤
│WhatsAppService│AppointmentService│ReminderService│GoogleCalendarService│
└───────┬───────┴───────┬───────┴────────┬───────┴────────────────────┘
        │               │                │
        ▼               ▼                ▼
┌─────────────────────────────────────────────────────────────────────┐
│                     Integration Layer                               │
├────────────────────┬──────────────────┬─────────────────────────────┤
│   TwilioService    │  AIServiceClient │   NotificationService       │
└────────────────────┴──────────────────┴─────────────────────────────┘
        │                    │                      │
        ▼                    ▼                      ▼
┌─────────────────┐ ┌──────────────────┐ ┌─────────────────────────────┐
│  Twilio API     │ │   AI Service     │ │     Google Calendar API     │
│ (WhatsApp/SMS)  │ │  (NLP/Intent)    │ │     (Optional)              │
└─────────────────┘ └──────────────────┘ └─────────────────────────────┘
```

---

## Implementation Checklist

### Phase 1: Core Booking (Current)
- [x] Basic appointment CRUD operations
- [x] WhatsApp message handling
- [x] Basic notification service
- [x] Twilio integration

### Phase 2: Enhanced Features
- [ ] Doctor/clinic availability management
- [ ] Double-booking prevention with database constraints
- [ ] Soft delete for cancellations
- [ ] Comprehensive rescheduling

### Phase 3: Recurring & Reminders
- [ ] Recurring appointment entity and service
- [ ] Scheduled reminder jobs
- [ ] Multi-channel notifications (WhatsApp, SMS, Email)
- [ ] Patient confirmation handling

### Phase 4: External Integrations
- [ ] Google Calendar OAuth setup
- [ ] Calendar sync service
- [ ] Real-time webhook handling
- [ ] Staff schedule import

---

## Support

For implementation questions or issues, please contact the MedAssist development team or create an issue in the repository.
