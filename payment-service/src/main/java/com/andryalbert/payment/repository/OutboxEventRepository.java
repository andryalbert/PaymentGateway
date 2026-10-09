package com.andryalbert.payment.repository;

import com.andryalbert.payment.entity.OutboxEvent;
import com.andryalbert.payment.entity.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    
    Optional<OutboxEvent> findByEventId(String eventId);
    
    List<OutboxEvent> findByStatus(OutboxStatus status);
    
    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(OutboxStatus status);
    
    List<OutboxEvent> findByAggregateId(String aggregateId);
}
