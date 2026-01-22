package com.medassist.config;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

/**
 * Configuration for Google Calendar API integration
 * Only enabled when google.calendar.enabled=true
 */
@Configuration
@ConditionalOnProperty(name = "google.calendar.enabled", havingValue = "true")
@Slf4j
public class GoogleCalendarConfig {

    @Value("${google.calendar.credentials-file:}")
    private String credentialsFilePath;

    @Value("${google.calendar.application-name:MedAssist}")
    private String applicationName;

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    @Bean
    public Calendar googleCalendarService() throws GeneralSecurityException, IOException {
        log.info("Initializing Google Calendar service...");

        HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

        GoogleCredentials credentials = getCredentials();

        return new Calendar.Builder(httpTransport, JSON_FACTORY, new HttpCredentialsAdapter(credentials))
                .setApplicationName(applicationName)
                .build();
    }

    private GoogleCredentials getCredentials() throws IOException {
        GoogleCredentials credentials;

        if (credentialsFilePath != null && !credentialsFilePath.isEmpty()) {
            // Use service account credentials from file
            log.info("Loading Google credentials from file: {}", credentialsFilePath);
            credentials = ServiceAccountCredentials
                    .fromStream(new FileInputStream(credentialsFilePath))
                    .createScoped(Collections.singleton(CalendarScopes.CALENDAR));
        } else {
            // Use Application Default Credentials (ADC)
            log.info("Using Application Default Credentials for Google Calendar");
            credentials = GoogleCredentials.getApplicationDefault()
                    .createScoped(Collections.singleton(CalendarScopes.CALENDAR));
        }

        return credentials;
    }
}
