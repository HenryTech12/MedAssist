package com.medassist.dto;

public class BotMessages {

    // Welcome message for new patients
    public static final String WELCOME_MESSAGE =
            "👋 Welcome to MedAssist!\n\n" +
                    "Which clinic are you registering with?\n\n" +
                    "1️⃣ City Health Clinic\n" +
                    "2️⃣ Green Cross Pharmacy\n" +
                    "3️⃣ Life Care Hospital\n\n" +
                    "Reply with the number (1, 2, or 3)";

    // Registration confirmation
    public static final String REGISTRATION_COMPLETE =
            "✅ Registration complete!\n\n" +
                    "You're now connected to %s.\n\n" +
                    "How can we help you today?";

    // Invalid clinic selection
    public static final String INVALID_CLINIC =
            "❌ Invalid selection.\n\n" +
                    "Please reply with 1, 2, or 3 to select your clinic.";

    // Ask for patient name
    public static final String ASK_NAME =
            "Great! 👍\n\n" +
                    "What's your full name?\n\n" +
                    "(Example: John Doe)";

    // Emergency detected
    public static final String EMERGENCY_ALERT =
            "🚨 EMERGENCY DETECTED\n\n" +
                    "Please call 911 immediately or go to the nearest hospital.\n\n" +
                    "Your clinic has been notified.";

    // Generic error
    public static final String ERROR_MESSAGE =
            "❌ Sorry, something went wrong.\n\n" +
                    "Please try again or contact your clinic directly.";
}