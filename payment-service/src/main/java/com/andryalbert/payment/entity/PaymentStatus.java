package com.andryalbert.payment.entity;

public enum PaymentStatus {
    PENDING,
    VALIDATING,
    FRAUD_CHECK,
    AUTHORIZED,
    CAPTURED,
    SETTLED,
    FAILED,
    REFUNDED,
    CANCELLED,
    REJECTED
}
