package com.andryalbert.payment.repository;

import com.andryalbert.payment.entity.Payment;
import com.andryalbert.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    
    Optional<Payment> findByPaymentId(String paymentId);
    
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
    
    List<Payment> findByMerchantId(String merchantId);
    
    List<Payment> findByCustomerId(String customerId);
    
    List<Payment> findByStatus(PaymentStatus status);
    
    List<Payment> findByMerchantIdAndStatus(String merchantId, PaymentStatus status);
}
