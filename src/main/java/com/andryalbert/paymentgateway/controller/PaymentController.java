package com.andryalbert.paymentgateway.controller;

import com.andryalbert.paymentgateway.dto.PaymentRequest;
import com.andryalbert.paymentgateway.dto.PaymentResponse;
import com.andryalbert.paymentgateway.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/process")
    public ResponseEntity<PaymentResponse> processPayment(@Valid @RequestBody PaymentRequest request) {
        PaymentResponse response = paymentService.processPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/transaction/{transactionId}")
    public ResponseEntity<PaymentResponse> getPaymentByTransactionId(@PathVariable String transactionId) {
        PaymentResponse response = paymentService.getPaymentByTransactionId(transactionId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/merchant/{merchantId}")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByMerchantId(@PathVariable String merchantId) {
        List<PaymentResponse> responses = paymentService.getPaymentsByMerchantId(merchantId);
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/refund/{transactionId}")
    public ResponseEntity<PaymentResponse> refundPayment(@PathVariable String transactionId) {
        PaymentResponse response = paymentService.refundPayment(transactionId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Payment Gateway API is running");
    }
}
