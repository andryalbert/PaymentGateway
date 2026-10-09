package com.andryalbert.notification.service;

import com.andryalbert.notification.entity.Notification;
import com.andryalbert.notification.entity.NotificationChannel;
import com.andryalbert.notification.entity.NotificationStatus;
import com.andryalbert.notification.entity.NotificationType;
import com.andryalbert.notification.repository.NotificationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final JavaMailSender mailSender;
    private final ObjectMapper objectMapper;

    @Transactional
    public void processPaymentEvent(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            String eventType = root.get("eventType").asText();
            String paymentId = root.get("paymentId").asText();
            String customerId = root.get("customerId").asText();
            String merchantId = root.get("merchantId").asText();
            BigDecimal amount = new BigDecimal(root.get("amount").asText());
            String currency = root.get("currency").asText();
            String status = root.has("status") ? root.get("status").asText() : "";

            // Determine notification type based on event
            NotificationType notificationType = determineNotificationType(eventType, status);
            
            if (notificationType != null) {
                // Send notification to customer
                createAndQueueNotification(
                    customerId,
                    "CUSTOMER",
                    "customer@example.com", // In production, get from customer service
                    "+1234567890",
                    notificationType,
                    NotificationChannel.EMAIL,
                    buildCustomerSubject(notificationType, paymentId),
                    buildCustomerContent(notificationType, paymentId, amount, currency, status),
                    "PAYMENT",
                    paymentId
                );

                // Send notification to merchant
                createAndQueueNotification(
                    merchantId,
                    "MERCHANT",
                    "merchant@example.com", // In production, get from merchant service
                    "+0987654321",
                    notificationType,
                    NotificationChannel.EMAIL,
                    buildMerchantSubject(notificationType, paymentId),
                    buildMerchantContent(notificationType, paymentId, amount, currency, status),
                    "PAYMENT",
                    paymentId
                );
            }

        } catch (Exception e) {
            log.error("Error processing payment event for notification", e);
        }
    }

    @Transactional
    public void createAndQueueNotification(String recipientId, String recipientType,
                                          String recipientEmail, String recipientPhone,
                                          NotificationType type, NotificationChannel channel,
                                          String subject, String content, String referenceType,
                                          String referenceId) {
        String notificationId = Notification.generateNotificationId();
        
        Notification notification = new Notification(
            notificationId,
            recipientId,
            recipientType,
            recipientEmail,
            recipientPhone,
            type,
            channel,
            subject,
            content,
            referenceType,
            referenceId
        );

        notificationRepository.save(notification);
        log.info("Notification created: {} for recipient: {}", notificationId, recipientId);
    }

    @Scheduled(fixedDelay = 10000)
    @Transactional
    public void processPendingNotifications() {
        List<Notification> pendingNotifications = notificationRepository.findByStatusOrderByCreatedAtAsc(NotificationStatus.PENDING);
        
        for (Notification notification : pendingNotifications) {
            try {
                boolean sent = false;
                
                switch (notification.getChannel()) {
                    case EMAIL:
                        sent = sendEmailNotification(notification);
                        break;
                    case SMS:
                        sent = sendSmsNotification(notification);
                        break;
                    case PUSH:
                        sent = sendPushNotification(notification);
                        break;
                    case WEBHOOK:
                        sent = sendWebhookNotification(notification);
                        break;
                }

                if (sent) {
                    notification.setStatus(NotificationStatus.SENT);
                    notification.setSentAt(LocalDateTime.now());
                    log.info("Notification sent successfully: {}", notification.getNotificationId());
                } else {
                    notification.setRetryCount(notification.getRetryCount() + 1);
                    if (notification.getRetryCount() >= 3) {
                        notification.setStatus(NotificationStatus.FAILED);
                    } else {
                        notification.setStatus(NotificationStatus.RETRYING);
                    }
                }

                notificationRepository.save(notification);

            } catch (Exception e) {
                log.error("Error sending notification: {}", notification.getNotificationId(), e);
                notification.setRetryCount(notification.getRetryCount() + 1);
                notification.setErrorMessage(e.getMessage());
                
                if (notification.getRetryCount() >= 3) {
                    notification.setStatus(NotificationStatus.FAILED);
                }
                
                notificationRepository.save(notification);
            }
        }
    }

    private boolean sendEmailNotification(Notification notification) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(notification.getRecipientEmail());
            message.setSubject(notification.getSubject());
            message.setText(notification.getContent());
            
            mailSender.send(message);
            return true;
        } catch (Exception e) {
            log.error("Failed to send email notification", e);
            return false;
        }
    }

    private boolean sendSmsNotification(Notification notification) {
        // In production, integrate with SMS service like Twilio
        log.info("SMS notification would be sent to: {} - {}", notification.getRecipientPhone(), notification.getContent());
        return true;
    }

    private boolean sendPushNotification(Notification notification) {
        // In production, integrate with push notification service
        log.info("Push notification would be sent to: {}", notification.getRecipientId());
        return true;
    }

    private boolean sendWebhookNotification(Notification notification) {
        // In production, make HTTP request to webhook URL
        log.info("Webhook notification would be sent for: {}", notification.getReferenceId());
        return true;
    }

    private NotificationType determineNotificationType(String eventType, String status) {
        switch (eventType) {
            case "PAYMENT_CREATED":
                return NotificationType.PAYMENT_CREATED;
            case "PAYMENT_AUTHORIZED":
                return NotificationType.PAYMENT_AUTHORIZED;
            case "PAYMENT_REFUNDED":
                return NotificationType.PAYMENT_REFUNDED;
            case "PAYMENT_SETTLED":
                return NotificationType.PAYMENT_SETTLED;
            case "PAYMENT_REJECTED":
                return NotificationType.FRAUD_DETECTED;
            default:
                return null;
        }
    }

    private String buildCustomerSubject(NotificationType type, String paymentId) {
        switch (type) {
            case PAYMENT_CREATED:
                return "Payment Initiated - " + paymentId;
            case PAYMENT_AUTHORIZED:
                return "Payment Authorized - " + paymentId;
            case PAYMENT_REFUNDED:
                return "Payment Refunded - " + paymentId;
            case PAYMENT_SETTLED:
                return "Payment Completed - " + paymentId;
            case FRAUD_DETECTED:
                return "Payment Security Alert - " + paymentId;
            default:
                return "Payment Update - " + paymentId;
        }
    }

    private String buildCustomerContent(NotificationType type, String paymentId, 
                                        BigDecimal amount, String currency, String status) {
        StringBuilder content = new StringBuilder();
        content.append("Dear Customer,\n\n");
        
        switch (type) {
            case PAYMENT_CREATED:
                content.append("Your payment of ").append(amount).append(" ").append(currency)
                      .append(" has been initiated.\n");
                content.append("Payment ID: ").append(paymentId).append("\n");
                content.append("Status: Processing\n");
                break;
            case PAYMENT_AUTHORIZED:
                content.append("Your payment of ").append(amount).append(" ").append(currency)
                      .append(" has been authorized.\n");
                content.append("Payment ID: ").append(paymentId).append("\n");
                content.append("Status: Authorized\n");
                break;
            case PAYMENT_REFUNDED:
                content.append("Your refund of ").append(amount).append(" ").append(currency)
                      .append(" has been processed.\n");
                content.append("Payment ID: ").append(paymentId).append("\n");
                content.append("The refund will appear in your account within 3-5 business days.\n");
                break;
            case PAYMENT_SETTLED:
                content.append("Your payment of ").append(amount).append(" ").append(currency)
                      .append(" has been completed.\n");
                content.append("Payment ID: ").append(paymentId).append("\n");
                content.append("Status: Settled\n");
                break;
            case FRAUD_DETECTED:
                content.append("We detected suspicious activity on payment ").append(paymentId).append(".\n");
                content.append("Amount: ").append(amount).append(" ").append(currency).append("\n");
                content.append("For your security, this payment has been blocked.\n");
                content.append("Please contact support if you did not authorize this transaction.\n");
                break;
        }
        
        content.append("\nThank you for using our payment service.\n");
        content.append("Payment Gateway Team");
        
        return content.toString();
    }

    private String buildMerchantSubject(NotificationType type, String paymentId) {
        switch (type) {
            case PAYMENT_CREATED:
                return "New Payment Received - " + paymentId;
            case PAYMENT_AUTHORIZED:
                return "Payment Authorized - " + paymentId;
            case PAYMENT_REFUNDED:
                return "Payment Refunded - " + paymentId;
            case PAYMENT_SETTLED:
                return "Payment Settled - " + paymentId;
            default:
                return "Payment Update - " + paymentId;
        }
    }

    private String buildMerchantContent(NotificationType type, String paymentId,
                                       BigDecimal amount, String currency, String status) {
        StringBuilder content = new StringBuilder();
        content.append("Dear Merchant,\n\n");
        
        switch (type) {
            case PAYMENT_CREATED:
                content.append("New payment of ").append(amount).append(" ").append(currency)
                      .append(" received.\n");
                content.append("Payment ID: ").append(paymentId).append("\n");
                content.append("Status: Processing\n");
                break;
            case PAYMENT_AUTHORIZED:
                content.append("Payment of ").append(amount).append(" ").append(currency)
                      .append(" has been authorized.\n");
                content.append("Payment ID: ").append(paymentId).append("\n");
                content.append("Funds will be settled according to your agreement.\n");
                break;
            case PAYMENT_REFUNDED:
                content.append("Refund processed for payment ").append(paymentId).append(".\n");
                content.append("Amount: ").append(amount).append(" ").append(currency).append("\n");
                content.append("This amount will be deducted from your next settlement.\n");
                break;
            case PAYMENT_SETTLED:
                content.append("Payment settled: ").append(paymentId).append("\n");
                content.append("Amount: ").append(amount).append(" ").append(currency).append("\n");
                content.append("Funds have been transferred to your account.\n");
                break;
        }
        
        content.append("\nBest regards,\n");
        content.append("Payment Gateway Team");
        
        return content.toString();
    }
}
