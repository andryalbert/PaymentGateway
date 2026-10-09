package com.andryalbert.reporting.controller;

import com.andryalbert.reporting.entity.DailyAggregation;
import com.andryalbert.reporting.entity.PaymentReport;
import com.andryalbert.reporting.service.ReportingService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ReportingController {

    private final ReportingService reportingService;

    @GetMapping("/merchant/{merchantId}/payments")
    public ResponseEntity<List<PaymentReport>> getMerchantReports(@PathVariable String merchantId) {
        List<PaymentReport> reports = reportingService.getMerchantReports(merchantId);
        return ResponseEntity.ok(reports);
    }

    @GetMapping("/merchant/{merchantId}/aggregations")
    public ResponseEntity<List<DailyAggregation>> getMerchantAggregations(@PathVariable String merchantId) {
        List<DailyAggregation> aggregations = reportingService.getMerchantAggregations(merchantId);
        return ResponseEntity.ok(aggregations);
    }

    @GetMapping("/date/{date}")
    public ResponseEntity<List<DailyAggregation>> getAllAggregationsForDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<DailyAggregation> aggregations = reportingService.getAllAggregationsForDate(date);
        return ResponseEntity.ok(aggregations);
    }

    @PostMapping("/aggregate")
    public ResponseEntity<String> triggerAggregation() {
        reportingService.generateDailyAggregations();
        return ResponseEntity.ok("Aggregation triggered");
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Reporting Service is healthy");
    }
}
