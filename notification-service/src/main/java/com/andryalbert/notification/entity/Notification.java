package com.andryalbert.notification.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String notificationId;

    @Column(nullable = false)
    private String recipientId;

    @Column(nullable = false)
    private String recipientType;

    @Column(nullable = false)
    private String recipientEmail;

    @Column(nullable = false)
    private String recipientPhone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannel channel;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private String referenceType;

    @Column(nullable = false)
    private String referenceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status;

    @Column(length = 1000)
    private String errorMessage;

    @Column(nullable = false)
    private Integer retryCount;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime sentAt;

    public Notification(String notificationId, String recipientId, String recipientType,
                       String recipientEmail, String recipientPhone, NotificationType type,
                       NotificationChannel channel, String subject, String content,
                       String referenceType, String referenceId) {
        this.notificationId = notificationId;
        this.recipientId = recipientId;
        this.recipientType = recipientType;
        this.recipientEmail = recipientEmail;
        this.recipientPhone = recipientPhone;
        this.type = type;
        this.channel = channel;
        this.subject = subject;
        this.content = content;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.status = NotificationStatus.PENDING;
        this.retryCount = 0;
    }

    public static String generateNotificationId() {
        return "NOTIF-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
    }
}
