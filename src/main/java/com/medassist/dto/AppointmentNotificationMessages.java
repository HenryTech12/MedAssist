package com.medassist.dto;

/**
 * Message templates for appointment notifications sent via WhatsApp/SMS
 */
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

    public static final String APPOINTMENT_REMINDER_DAY_BEFORE = """
        🏥 Appointment Reminder
        
        Hi %s,
        
        This is a reminder that you have an appointment tomorrow:
        📅 Date: %s
        ⏰ Time: %s
        📋 Reason: %s
        
        Reply 'CONFIRM' to confirm or 'RESCHEDULE' to change your appointment.
        """;

    public static final String APPOINTMENT_REMINDER_HOURS_BEFORE = """
        ⏰ Appointment in 2 Hours
        
        Hi %s,
        
        Your appointment is in 2 hours:
        ⏰ Time: %s
        
        Please arrive 10 minutes early.
        """;

    public static final String APPOINTMENT_CONFIRMATION_DETAILS = """
        ✅ Appointment Confirmed!
        
        Hi %s,
        
        Your appointment details:
        📅 Date: %s
        ⏰ Time: %s
        📋 Reason: %s
        
        Reply 'CANCEL' to cancel or 'RESCHEDULE' to change.
        """;

    public static final String AVAILABLE_SLOTS_MESSAGE = """
        📅 Available Time Slots
        
        Here are the available slots:
        %s
        
        Reply with the slot number to book (e.g., '1' for the first slot).
        Or reply with a specific date/time like 'Book January 25 at 2pm'.
        """;

    public static final String NO_SLOTS_AVAILABLE = """
        😔 No Available Slots
        
        Sorry, there are no available slots for the requested time.
        
        Would you like to:
        1. See slots for another day
        2. Join the waitlist
        
        Reply with '1' or '2'.
        """;

    public static final String BOOKING_INITIATED = """
        📅 Let's Book Your Appointment
        
        What day would you prefer?
        
        Reply with:
        - A specific date (e.g., 'January 25')
        - 'Tomorrow' or 'Next week'
        - 'See available times' for this week's slots
        """;

    public static final String ASK_APPOINTMENT_REASON = """
        📋 What's the reason for your visit?
        
        Please briefly describe why you need this appointment.
        (e.g., 'General checkup', 'Follow-up', 'New symptoms')
        """;

    public static final String SLOT_ALREADY_BOOKED = """
        ⚠️ Slot Unavailable
        
        Sorry, that time slot was just booked by someone else.
        
        Here are other available slots:
        %s
        
        Reply with a slot number to book.
        """;
}
