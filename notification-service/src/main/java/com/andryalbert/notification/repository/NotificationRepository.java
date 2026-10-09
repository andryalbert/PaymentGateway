package com.andryalbert.notification.repository;

import com.andryalbert.notification.entity.Notification;
import com.andryalbert.notification.entity.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    Optional<Notification> findByNotificationId(String notificationId);
    
    List<Notification> findByRecipientId(String recipientId);
    
    List<Notification> findByReferenceTypeAndReferenceId(String referenceType, String referenceId);
    
    List<Notification> findByStatus(NotificationStatus status);
    
    List<Notification> findByStatusOrderByCreatedAtAsc(NotificationStatus status);
}
