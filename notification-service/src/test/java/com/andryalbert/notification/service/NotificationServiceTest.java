package com.andryalbert.notification.service;

import com.andryalbert.notification.entity.Notification;
import com.andryalbert.notification.entity.NotificationChannel;
import com.andryalbert.notification.entity.NotificationStatus;
import com.andryalbert.notification.entity.NotificationType;
import com.andryalbert.notification.repository.NotificationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private JavaMailSender mailSender;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        // No setup needed
    }

    @Test
    void createAndQueueNotification_ShouldSaveNotificationWithPendingStatus() {
        when(notificationRepository.save(any(Notification.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        String recipientId = "CUST-001";
        String recipientType = "CUSTOMER";
        String recipientEmail = "customer@example.com";
        String recipientPhone = "+1234567890";
        NotificationType type = NotificationType.PAYMENT_CREATED;
        NotificationChannel channel = NotificationChannel.EMAIL;
        String subject = "Test Subject";
        String content = "Test Content";
        String referenceType = "PAYMENT";
        String referenceId = "PAY-001";

        notificationService.createAndQueueNotification(
            recipientId, recipientType, recipientEmail, recipientPhone,
            type, channel, subject, content, referenceType, referenceId
        );

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());

        Notification savedNotification = notificationCaptor.getValue();
        assertEquals(recipientId, savedNotification.getRecipientId());
        assertEquals(recipientType, savedNotification.getRecipientType());
        assertEquals(recipientEmail, savedNotification.getRecipientEmail());
        assertEquals(recipientPhone, savedNotification.getRecipientPhone());
        assertEquals(type, savedNotification.getType());
        assertEquals(channel, savedNotification.getChannel());
        assertEquals(subject, savedNotification.getSubject());
        assertEquals(content, savedNotification.getContent());
        assertEquals(referenceType, savedNotification.getReferenceType());
        assertEquals(referenceId, savedNotification.getReferenceId());
        assertEquals(NotificationStatus.PENDING, savedNotification.getStatus());
        assertEquals(0, savedNotification.getRetryCount());
        assertNotNull(savedNotification.getNotificationId());
    }

    @Test
    void processPaymentEvent_PAYMENT_CREATED_ShouldNotThrowException() {
        String message = """
            {
                "eventType": "PAYMENT_CREATED",
                "paymentId": "PAY-001",
                "customerId": "CUST-001",
                "merchantId": "MERCHANT-001",
                "amount": 100.50,
                "currency": "USD"
            }
            """;

        assertDoesNotThrow(() -> notificationService.processPaymentEvent(message));
    }

    @Test
    void processPaymentEvent_PAYMENT_AUTHORIZED_ShouldNotThrowException() {
        String message = """
            {
                "eventType": "PAYMENT_AUTHORIZED",
                "paymentId": "PAY-002",
                "customerId": "CUST-002",
                "merchantId": "MERCHANT-002",
                "amount": 250.00,
                "currency": "EUR",
                "status": "AUTHORIZED"
            }
            """;

        assertDoesNotThrow(() -> notificationService.processPaymentEvent(message));
    }

    @Test
    void processPaymentEvent_PAYMENT_REFUNDED_ShouldNotThrowException() {
        String message = """
            {
                "eventType": "PAYMENT_REFUNDED",
                "paymentId": "PAY-003",
                "customerId": "CUST-003",
                "merchantId": "MERCHANT-003",
                "amount": 75.25,
                "currency": "GBP",
                "status": "REFUNDED"
            }
            """;

        assertDoesNotThrow(() -> notificationService.processPaymentEvent(message));
    }

    @Test
    void processPaymentEvent_PAYMENT_SETTLED_ShouldNotThrowException() {
        String message = """
            {
                "eventType": "PAYMENT_SETTLED",
                "paymentId": "PAY-004",
                "customerId": "CUST-004",
                "merchantId": "MERCHANT-004",
                "amount": 500.00,
                "currency": "USD",
                "status": "SETTLED"
            }
            """;

        assertDoesNotThrow(() -> notificationService.processPaymentEvent(message));
    }

    @Test
    void processPaymentEvent_PAYMENT_REJECTED_ShouldNotThrowException() {
        String message = """
            {
                "eventType": "PAYMENT_REJECTED",
                "paymentId": "PAY-005",
                "customerId": "CUST-005",
                "merchantId": "MERCHANT-005",
                "amount": 1000.00,
                "currency": "USD",
                "status": "REJECTED"
            }
            """;

        assertDoesNotThrow(() -> notificationService.processPaymentEvent(message));
    }

    @Test
    void processPaymentEvent_UnknownEventType_ShouldNotThrowException() {
        String message = """
            {
                "eventType": "UNKNOWN_EVENT",
                "paymentId": "PAY-006",
                "customerId": "CUST-006",
                "merchantId": "MERCHANT-006",
                "amount": 100.00,
                "currency": "USD"
            }
            """;

        assertDoesNotThrow(() -> notificationService.processPaymentEvent(message));
    }

    @Test
    void processPendingNotifications_EmailChannel_ShouldSendEmailAndUpdateStatus() {
        when(notificationRepository.save(any(Notification.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        Notification notification = new Notification(
            "NOTIF-001", "CUST-001", "CUSTOMER", "customer@example.com",
            "+1234567890", NotificationType.PAYMENT_CREATED, NotificationChannel.EMAIL,
            "Test Subject", "Test Content", "PAYMENT", "PAY-001"
        );

        when(notificationRepository.findByStatusOrderByCreatedAtAsc(NotificationStatus.PENDING))
            .thenReturn(Arrays.asList(notification));

        notificationService.processPendingNotifications();

        verify(mailSender).send(any(SimpleMailMessage.class));
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());

        Notification updatedNotification = notificationCaptor.getValue();
        assertEquals(NotificationStatus.SENT, updatedNotification.getStatus());
        assertNotNull(updatedNotification.getSentAt());
    }

    @Test
    void processPendingNotifications_SmsChannel_ShouldLogAndMarkAsSent() {
        when(notificationRepository.save(any(Notification.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        Notification notification = new Notification(
            "NOTIF-002", "CUST-002", "CUSTOMER", "customer@example.com",
            "+1234567890", NotificationType.PAYMENT_AUTHORIZED, NotificationChannel.SMS,
            "Test Subject", "Test Content", "PAYMENT", "PAY-002"
        );

        when(notificationRepository.findByStatusOrderByCreatedAtAsc(NotificationStatus.PENDING))
            .thenReturn(Arrays.asList(notification));

        notificationService.processPendingNotifications();

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());

        Notification updatedNotification = notificationCaptor.getValue();
        assertEquals(NotificationStatus.SENT, updatedNotification.getStatus());
        assertNotNull(updatedNotification.getSentAt());
    }

    @Test
    void processPendingNotifications_PushChannel_ShouldLogAndMarkAsSent() {
        when(notificationRepository.save(any(Notification.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        Notification notification = new Notification(
            "NOTIF-003", "CUST-003", "CUSTOMER", "customer@example.com",
            "+1234567890", NotificationType.PAYMENT_SETTLED, NotificationChannel.PUSH,
            "Test Subject", "Test Content", "PAYMENT", "PAY-003"
        );

        when(notificationRepository.findByStatusOrderByCreatedAtAsc(NotificationStatus.PENDING))
            .thenReturn(Arrays.asList(notification));

        notificationService.processPendingNotifications();

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());

        Notification updatedNotification = notificationCaptor.getValue();
        assertEquals(NotificationStatus.SENT, updatedNotification.getStatus());
        assertNotNull(updatedNotification.getSentAt());
    }

    @Test
    void processPendingNotifications_WebhookChannel_ShouldLogAndMarkAsSent() {
        when(notificationRepository.save(any(Notification.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        Notification notification = new Notification(
            "NOTIF-004", "CUST-004", "CUSTOMER", "customer@example.com",
            "+1234567890", NotificationType.PAYMENT_REFUNDED, NotificationChannel.WEBHOOK,
            "Test Subject", "Test Content", "PAYMENT", "PAY-004"
        );

        when(notificationRepository.findByStatusOrderByCreatedAtAsc(NotificationStatus.PENDING))
            .thenReturn(Arrays.asList(notification));

        notificationService.processPendingNotifications();

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());

        Notification updatedNotification = notificationCaptor.getValue();
        assertEquals(NotificationStatus.SENT, updatedNotification.getStatus());
        assertNotNull(updatedNotification.getSentAt());
    }

    @Test
    void processPendingNotifications_EmailFailure_ShouldIncrementRetryCount() {
        when(notificationRepository.save(any(Notification.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        Notification notification = new Notification(
            "NOTIF-005", "CUST-005", "CUSTOMER", "customer@example.com",
            "+1234567890", NotificationType.PAYMENT_CREATED, NotificationChannel.EMAIL,
            "Test Subject", "Test Content", "PAYMENT", "PAY-005"
        );

        when(notificationRepository.findByStatusOrderByCreatedAtAsc(NotificationStatus.PENDING))
            .thenReturn(Arrays.asList(notification));
        doThrow(new RuntimeException("Email send failed")).when(mailSender).send(any(SimpleMailMessage.class));

        notificationService.processPendingNotifications();

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());

        Notification updatedNotification = notificationCaptor.getValue();
        assertEquals(1, updatedNotification.getRetryCount());
        assertEquals(NotificationStatus.RETRYING, updatedNotification.getStatus());
    }

    @Test
    void processPendingNotifications_MaxRetriesExceeded_ShouldMarkAsFailed() {
        when(notificationRepository.save(any(Notification.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        Notification notification = new Notification(
            "NOTIF-006", "CUST-006", "CUSTOMER", "customer@example.com",
            "+1234567890", NotificationType.PAYMENT_CREATED, NotificationChannel.EMAIL,
            "Test Subject", "Test Content", "PAYMENT", "PAY-006"
        );
        notification.setRetryCount(3);

        when(notificationRepository.findByStatusOrderByCreatedAtAsc(NotificationStatus.PENDING))
            .thenReturn(Arrays.asList(notification));
        doThrow(new RuntimeException("Email send failed")).when(mailSender).send(any(SimpleMailMessage.class));

        notificationService.processPendingNotifications();

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());

        Notification updatedNotification = notificationCaptor.getValue();
        assertEquals(4, updatedNotification.getRetryCount());
        assertEquals(NotificationStatus.FAILED, updatedNotification.getStatus());
    }

    @Test
    void processPendingNotifications_EmptyList_ShouldNotCallSave() {
        when(notificationRepository.findByStatusOrderByCreatedAtAsc(NotificationStatus.PENDING))
            .thenReturn(Arrays.asList());

        notificationService.processPendingNotifications();

        verify(notificationRepository, never()).save(any(Notification.class));
    }
}
