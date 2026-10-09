package com.andryalbert.notification.controller;

import com.andryalbert.notification.entity.Notification;
import com.andryalbert.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class NotificationController {

    private final NotificationRepository notificationRepository;

    @GetMapping("/{notificationId}")
    public ResponseEntity<Notification> getNotification(@PathVariable String notificationId) {
        return notificationRepository.findByNotificationId(notificationId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/recipient/{recipientId}")
    public ResponseEntity<List<Notification>> getNotificationsByRecipient(@PathVariable String recipientId) {
        List<Notification> notifications = notificationRepository.findByRecipientId(recipientId);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/reference/{referenceType}/{referenceId}")
    public ResponseEntity<List<Notification>> getNotificationsByReference(
            @PathVariable String referenceType,
            @PathVariable String referenceId) {
        List<Notification> notifications = notificationRepository.findByReferenceTypeAndReferenceId(referenceType, referenceId);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Notification Service is healthy");
    }
}
