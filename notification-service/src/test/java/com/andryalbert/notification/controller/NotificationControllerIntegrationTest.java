package com.andryalbert.notification.controller;

import com.andryalbert.notification.entity.Notification;
import com.andryalbert.notification.entity.NotificationChannel;
import com.andryalbert.notification.entity.NotificationStatus;
import com.andryalbert.notification.entity.NotificationType;
import com.andryalbert.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class NotificationControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
        DockerImageName.parse("postgres:15-alpine"))
        .withDatabaseName("notificationdb")
        .withUsername("testuser")
        .withPassword("testpass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.kafka.enabled", () -> "false");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotificationRepository notificationRepository;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
    }

    @Test
    void getNotification_ShouldReturnNotification_WhenNotificationExists() throws Exception {
        Notification notification = notificationRepository.save(new Notification(
            "NOTIF-TEST-001", "CUST-001", "CUSTOMER", "customer@example.com",
            "+1234567890", NotificationType.PAYMENT_CREATED, NotificationChannel.EMAIL,
            "Payment Initiated - PAY-001", "Your payment has been initiated.",
            "PAYMENT", "PAY-001"
        ));

        mockMvc.perform(get("/api/v1/notifications/{notificationId}", notification.getNotificationId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notificationId").value(notification.getNotificationId()))
                .andExpect(jsonPath("$.recipientId").value("CUST-001"))
                .andExpect(jsonPath("$.recipientType").value("CUSTOMER"))
                .andExpect(jsonPath("$.recipientEmail").value("customer@example.com"))
                .andExpect(jsonPath("$.recipientPhone").value("+1234567890"))
                .andExpect(jsonPath("$.type").value("PAYMENT_CREATED"))
                .andExpect(jsonPath("$.channel").value("EMAIL"))
                .andExpect(jsonPath("$.subject").value("Payment Initiated - PAY-001"))
                .andExpect(jsonPath("$.referenceType").value("PAYMENT"))
                .andExpect(jsonPath("$.referenceId").value("PAY-001"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.retryCount").value(0));
    }

    @Test
    void getNotification_ShouldReturn404_WhenNotificationNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/{notificationId}", "NOTIF-NONEXISTENT"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getNotificationsByRecipient_ShouldReturnNotifications_WhenNotificationsExist() throws Exception {
        Notification notification1 = notificationRepository.save(new Notification(
            "NOTIF-TEST-001", "CUST-001", "CUSTOMER", "customer@example.com",
            "+1234567890", NotificationType.PAYMENT_CREATED, NotificationChannel.EMAIL,
            "Subject 1", "Content 1", "PAYMENT", "PAY-001"
        ));

        Notification notification2 = notificationRepository.save(new Notification(
            "NOTIF-TEST-002", "CUST-001", "CUSTOMER", "customer@example.com",
            "+1234567890", NotificationType.PAYMENT_AUTHORIZED, NotificationChannel.EMAIL,
            "Subject 2", "Content 2", "PAYMENT", "PAY-002"
        ));

        mockMvc.perform(get("/api/v1/notifications/recipient/{recipientId}", "CUST-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].recipientId", everyItem(is("CUST-001"))))
                .andExpect(jsonPath("$[*].notificationId", containsInAnyOrder(
                    notification1.getNotificationId(),
                    notification2.getNotificationId()
                )));
    }

    @Test
    void getNotificationsByRecipient_ShouldReturnEmptyList_WhenNoNotifications() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/recipient/{recipientId}", "CUST-NONEXISTENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getNotificationsByReference_ShouldReturnNotifications_WhenNotificationsExist() throws Exception {
        Notification notification1 = notificationRepository.save(new Notification(
            "NOTIF-TEST-001", "CUST-001", "CUSTOMER", "customer@example.com",
            "+1234567890", NotificationType.PAYMENT_CREATED, NotificationChannel.EMAIL,
            "Subject 1", "Content 1", "PAYMENT", "PAY-001"
        ));

        Notification notification2 = notificationRepository.save(new Notification(
            "NOTIF-TEST-002", "MERCHANT-001", "MERCHANT", "merchant@example.com",
            "+0987654321", NotificationType.PAYMENT_CREATED, NotificationChannel.EMAIL,
            "Subject 2", "Content 2", "PAYMENT", "PAY-001"
        ));

        mockMvc.perform(get("/api/v1/notifications/reference/{referenceType}/{referenceId}", "PAYMENT", "PAY-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].referenceType", everyItem(is("PAYMENT"))))
                .andExpect(jsonPath("$[*].referenceId", everyItem(is("PAY-001"))))
                .andExpect(jsonPath("$[*].notificationId", containsInAnyOrder(
                    notification1.getNotificationId(),
                    notification2.getNotificationId()
                )));
    }

    @Test
    void getNotificationsByReference_ShouldReturnEmptyList_WhenNoNotifications() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/reference/{referenceType}/{referenceId}", "PAYMENT", "PAY-NONEXISTENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void health_ShouldReturnHealthyStatus() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("Notification Service is healthy"));
    }

    @Test
    void fullFlow_CreateAndRetrieveNotifications() throws Exception {
        Notification customerNotification = notificationRepository.save(new Notification(
            "NOTIF-FLOW-001", "CUST-FLOW-001", "CUSTOMER", "customer@example.com",
            "+1234567890", NotificationType.PAYMENT_SETTLED, NotificationChannel.EMAIL,
            "Payment Completed - PAY-FLOW-001", "Your payment has been completed.",
            "PAYMENT", "PAY-FLOW-001"
        ));

        Notification merchantNotification = notificationRepository.save(new Notification(
            "NOTIF-FLOW-002", "MERCHANT-FLOW-001", "MERCHANT", "merchant@example.com",
            "+0987654321", NotificationType.PAYMENT_SETTLED, NotificationChannel.EMAIL,
            "Payment Settled - PAY-FLOW-001", "Payment has been settled.",
            "PAYMENT", "PAY-FLOW-001"
        ));

        mockMvc.perform(get("/api/v1/notifications/{notificationId}", customerNotification.getNotificationId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notificationId").value(customerNotification.getNotificationId()))
                .andExpect(jsonPath("$.recipientId").value("CUST-FLOW-001"));

        mockMvc.perform(get("/api/v1/notifications/recipient/{recipientId}", "CUST-FLOW-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].notificationId").value(customerNotification.getNotificationId()));

        mockMvc.perform(get("/api/v1/notifications/reference/{referenceType}/{referenceId}", "PAYMENT", "PAY-FLOW-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].referenceId", everyItem(is("PAY-FLOW-001"))));

        List<Notification> allNotifications = notificationRepository.findAll();
        assertEquals(2, allNotifications.size());
    }

    @Test
    void multipleRecipients_ShouldBeStoredIndependently() throws Exception {
        notificationRepository.save(new Notification(
            "NOTIF-MULTI-001", "CUST-001", "CUSTOMER", "customer1@example.com",
            "+1234567890", NotificationType.PAYMENT_CREATED, NotificationChannel.EMAIL,
            "Subject 1", "Content 1", "PAYMENT", "PAY-001"
        ));

        notificationRepository.save(new Notification(
            "NOTIF-MULTI-002", "CUST-002", "CUSTOMER", "customer2@example.com",
            "+0987654321", NotificationType.PAYMENT_CREATED, NotificationChannel.EMAIL,
            "Subject 2", "Content 2", "PAYMENT", "PAY-002"
        ));

        mockMvc.perform(get("/api/v1/notifications/recipient/{recipientId}", "CUST-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].recipientEmail").value("customer1@example.com"));

        mockMvc.perform(get("/api/v1/notifications/recipient/{recipientId}", "CUST-002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].recipientEmail").value("customer2@example.com"));

        List<Notification> allNotifications = notificationRepository.findAll();
        assertEquals(2, allNotifications.size());
    }

    @Test
    void differentNotificationTypes_ShouldBeStoredCorrectly() throws Exception {
        notificationRepository.save(new Notification(
            "NOTIF-TYPE-001", "CUST-001", "CUSTOMER", "customer@example.com",
            "+1234567890", NotificationType.PAYMENT_CREATED, NotificationChannel.EMAIL,
            "Created", "Payment created", "PAYMENT", "PAY-001"
        ));

        notificationRepository.save(new Notification(
            "NOTIF-TYPE-002", "CUST-001", "CUSTOMER", "customer@example.com",
            "+1234567890", NotificationType.PAYMENT_AUTHORIZED, NotificationChannel.SMS,
            "Authorized", "Payment authorized", "PAYMENT", "PAY-002"
        ));

        notificationRepository.save(new Notification(
            "NOTIF-TYPE-003", "CUST-001", "CUSTOMER", "customer@example.com",
            "+1234567890", NotificationType.FRAUD_DETECTED, NotificationChannel.EMAIL,
            "Fraud Alert", "Fraud detected", "PAYMENT", "PAY-003"
        ));

        mockMvc.perform(get("/api/v1/notifications/recipient/{recipientId}", "CUST-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].type", containsInAnyOrder(
                    "PAYMENT_CREATED", "PAYMENT_AUTHORIZED", "FRAUD_DETECTED"
                )))
                .andExpect(jsonPath("$[*].channel", containsInAnyOrder("EMAIL", "SMS", "EMAIL")));
    }

    @Test
    void notificationStatus_ShouldBePersistedCorrectly() throws Exception {
        Notification pendingNotification = notificationRepository.save(new Notification(
            "NOTIF-STATUS-001", "CUST-001", "CUSTOMER", "customer@example.com",
            "+1234567890", NotificationType.PAYMENT_CREATED, NotificationChannel.EMAIL,
            "Subject", "Content", "PAYMENT", "PAY-001"
        ));

        pendingNotification.setStatus(NotificationStatus.SENT);
        pendingNotification.setSentAt(java.time.LocalDateTime.now());
        notificationRepository.save(pendingNotification);

        mockMvc.perform(get("/api/v1/notifications/{notificationId}", pendingNotification.getNotificationId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SENT"))
                .andExpect(jsonPath("$.sentAt").isNotEmpty());
    }
}
