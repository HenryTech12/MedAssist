package com.medassist.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class NotificationService {

    @Autowired
    private SimpMessageSendingOperations sendingOperations;


    public void sendNotifications(String notifications) {
        Map<String,Object> data = new HashMap<>();
        sendingOperations.convertAndSend("/api/notifications",data);
    }
}
