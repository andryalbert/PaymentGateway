package com.andryalbert.fraud.controller;

import com.andryalbert.fraud.entity.FraudCheck;
import com.andryalbert.fraud.repository.FraudCheckRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/fraud")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FraudController {

    private final FraudCheckRepository fraudCheckRepository;

    @GetMapping("/check/{checkId}")
    public ResponseEntity<FraudCheck> getFraudCheck(@PathVariable String checkId) {
        return fraudCheckRepository.findByCheckId(checkId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/payment/{paymentId}")
    public ResponseEntity<FraudCheck> getFraudCheckByPaymentId(@PathVariable String paymentId) {
        return fraudCheckRepository.findByPaymentId(paymentId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<FraudCheck>> getFraudChecksByCustomer(@PathVariable String customerId) {
        List<FraudCheck> checks = fraudCheckRepository.findByCustomerId(customerId);
        return ResponseEntity.ok(checks);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Fraud Detection Service is healthy");
    }

    @PostMapping
    public ResponseEntity<FraudCheck> createFraudCheck(@RequestBody FraudCheck fraudCheck) {
        FraudCheck savedCheck = fraudCheckRepository.save(fraudCheck);
        return ResponseEntity.ok(savedCheck);
    }
}
