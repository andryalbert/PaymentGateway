package com.andryalbert.reporting.service;

import com.andryalbert.reporting.entity.DailyAggregation;
import com.andryalbert.reporting.entity.PaymentReport;
import com.andryalbert.reporting.repository.DailyAggregationRepository;
import com.andryalbert.reporting.repository.PaymentReportRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportingServiceTest {

    @Mock
    private PaymentReportRepository paymentReportRepository;

    @Mock
    private DailyAggregationRepository dailyAggregationRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ReportingService reportingService;

    private String paymentEventMessage;

    @BeforeEach
    void setUp() {
        paymentEventMessage = """
            {
                "eventType": "PAYMENT_CREATED",
                "paymentId": "PAY-001",
                "merchantId": "MERCHANT-001",
                "customerId": "CUST-001",
                "amount": 100.50,
                "currency": "USD",
                "status": "AUTHORIZED",
                "timestamp": "2024-01-15T10:30:00"
            }
            """;
    }

    @Test
    void processPaymentEvent_ShouldSavePaymentReport_WhenValidMessage() throws Exception {
        when(objectMapper.readTree(paymentEventMessage)).thenReturn(new ObjectMapper().readTree(paymentEventMessage));
        when(paymentReportRepository.save(any(PaymentReport.class))).thenAnswer(invocation -> invocation.getArgument(0));

        reportingService.processPaymentEvent(paymentEventMessage);

        ArgumentCaptor<PaymentReport> reportCaptor = ArgumentCaptor.forClass(PaymentReport.class);
        verify(paymentReportRepository).save(reportCaptor.capture());

        PaymentReport savedReport = reportCaptor.getValue();
        assertEquals("PAY-001", savedReport.getPaymentId());
        assertEquals("MERCHANT-001", savedReport.getMerchantId());
        assertEquals("CUST-001", savedReport.getCustomerId());
        assertEquals(0, new BigDecimal("100.50").compareTo(savedReport.getAmount()));
        assertEquals("USD", savedReport.getCurrency());
        assertEquals("AUTHORIZED", savedReport.getStatus());
        assertEquals("PAYMENT_CREATED", savedReport.getEventType());
        assertEquals(LocalDateTime.parse("2024-01-15T10:30:00"), savedReport.getEventTimestamp());
        assertEquals("CLEARED", savedReport.getFraudCheckResult());
        assertEquals(0, savedReport.getFraudRiskScore());
    }

    @Test
    void processPaymentEvent_ShouldSetFraudDetected_WhenStatusIsRejected() throws Exception {
        String rejectedMessage = """
            {
                "eventType": "PAYMENT_REJECTED",
                "paymentId": "PAY-002",
                "merchantId": "MERCHANT-002",
                "customerId": "CUST-002",
                "amount": 1000.00,
                "currency": "USD",
                "status": "REJECTED"
            }
            """;

        when(objectMapper.readTree(rejectedMessage)).thenReturn(new ObjectMapper().readTree(rejectedMessage));
        when(paymentReportRepository.save(any(PaymentReport.class))).thenAnswer(invocation -> invocation.getArgument(0));

        reportingService.processPaymentEvent(rejectedMessage);

        ArgumentCaptor<PaymentReport> reportCaptor = ArgumentCaptor.forClass(PaymentReport.class);
        verify(paymentReportRepository).save(reportCaptor.capture());

        PaymentReport savedReport = reportCaptor.getValue();
        assertEquals("FRAUD_DETECTED", savedReport.getFraudCheckResult());
        assertEquals(100, savedReport.getFraudRiskScore());
    }

    @Test
    void processPaymentEvent_ShouldHandleMissingStatus() throws Exception {
        String messageWithoutStatus = """
            {
                "eventType": "PAYMENT_CREATED",
                "paymentId": "PAY-003",
                "merchantId": "MERCHANT-003",
                "customerId": "CUST-003",
                "amount": 50.00,
                "currency": "EUR"
            }
            """;

        when(objectMapper.readTree(messageWithoutStatus)).thenReturn(new ObjectMapper().readTree(messageWithoutStatus));
        when(paymentReportRepository.save(any(PaymentReport.class))).thenAnswer(invocation -> invocation.getArgument(0));

        reportingService.processPaymentEvent(messageWithoutStatus);

        ArgumentCaptor<PaymentReport> reportCaptor = ArgumentCaptor.forClass(PaymentReport.class);
        verify(paymentReportRepository).save(reportCaptor.capture());

        PaymentReport savedReport = reportCaptor.getValue();
        assertEquals("", savedReport.getStatus());
        assertEquals("CLEARED", savedReport.getFraudCheckResult());
    }

    @Test
    void processPaymentEvent_ShouldHandleMissingTimestamp() throws Exception {
        String messageWithoutTimestamp = """
            {
                "eventType": "PAYMENT_CREATED",
                "paymentId": "PAY-004",
                "merchantId": "MERCHANT-004",
                "customerId": "CUST-004",
                "amount": 75.00,
                "currency": "GBP",
                "status": "AUTHORIZED"
            }
            """;

        when(objectMapper.readTree(messageWithoutTimestamp)).thenReturn(new ObjectMapper().readTree(messageWithoutTimestamp));
        when(paymentReportRepository.save(any(PaymentReport.class))).thenAnswer(invocation -> invocation.getArgument(0));

        reportingService.processPaymentEvent(messageWithoutTimestamp);

        ArgumentCaptor<PaymentReport> reportCaptor = ArgumentCaptor.forClass(PaymentReport.class);
        verify(paymentReportRepository).save(reportCaptor.capture());

        PaymentReport savedReport = reportCaptor.getValue();
        assertNotNull(savedReport.getEventTimestamp());
    }

    @Test
    void processPaymentEvent_ShouldHandleException_WhenInvalidJson() throws Exception {
        String invalidMessage = "invalid json";

        when(objectMapper.readTree(invalidMessage)).thenThrow(new RuntimeException("Invalid JSON"));

        reportingService.processPaymentEvent(invalidMessage);

        verify(paymentReportRepository, never()).save(any(PaymentReport.class));
    }

    @Test
    void generateDailyAggregationForMerchant_ShouldCreateAggregation_WhenNotExists() {
        String merchantId = "MERCHANT-001";
        LocalDate date = LocalDate.of(2024, 1, 15);
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);

        when(dailyAggregationRepository.findByMerchantIdAndAggregationDate(merchantId, date)).thenReturn(Optional.empty());
        when(paymentReportRepository.countByMerchantIdAndEventTimestampAfter(merchantId, startOfDay)).thenReturn(10L);
        when(paymentReportRepository.sumSuccessfulAmountByMerchantIdAndEventTimestampAfter(merchantId, startOfDay)).thenReturn(new BigDecimal("1000.00"));
        when(paymentReportRepository.countFraudRejectedByMerchantIdAndEventTimestampAfter(merchantId, startOfDay)).thenReturn(2L);
        when(dailyAggregationRepository.save(any(DailyAggregation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        reportingService.generateDailyAggregationForMerchant(merchantId, date, startOfDay, endOfDay);

        ArgumentCaptor<DailyAggregation> aggregationCaptor = ArgumentCaptor.forClass(DailyAggregation.class);
        verify(dailyAggregationRepository).save(aggregationCaptor.capture());

        DailyAggregation savedAggregation = aggregationCaptor.getValue();
        assertEquals(merchantId, savedAggregation.getMerchantId());
        assertEquals(date, savedAggregation.getAggregationDate());
        assertEquals(10, savedAggregation.getTotalTransactions());
        assertEquals(8, savedAggregation.getSuccessfulTransactions());
        assertEquals(2, savedAggregation.getFailedTransactions());
        assertEquals(2, savedAggregation.getFraudRejectedTransactions());
        assertEquals(new BigDecimal("1000.00"), savedAggregation.getTotalAmount());
        assertEquals(new BigDecimal("1000.00"), savedAggregation.getSuccessfulAmount());
        assertEquals(0, BigDecimal.ZERO.compareTo(savedAggregation.getFailedAmount()));
        assertEquals(new BigDecimal("100.00"), savedAggregation.getAverageTransactionValue());
        assertEquals(new BigDecimal("29.00"), savedAggregation.getTotalFees());
    }

    @Test
    void generateDailyAggregationForMerchant_ShouldNotCreate_WhenAlreadyExists() {
        String merchantId = "MERCHANT-001";
        LocalDate date = LocalDate.of(2024, 1, 15);
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);

        DailyAggregation existingAggregation = new DailyAggregation();
        when(dailyAggregationRepository.findByMerchantIdAndAggregationDate(merchantId, date)).thenReturn(Optional.of(existingAggregation));

        reportingService.generateDailyAggregationForMerchant(merchantId, date, startOfDay, endOfDay);

        verify(dailyAggregationRepository, never()).save(any(DailyAggregation.class));
        verify(paymentReportRepository, never()).countByMerchantIdAndEventTimestampAfter(anyString(), any(LocalDateTime.class));
    }

    @Test
    void generateDailyAggregationForMerchant_ShouldHandleNullValues() {
        String merchantId = "MERCHANT-001";
        LocalDate date = LocalDate.of(2024, 1, 15);
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);

        when(dailyAggregationRepository.findByMerchantIdAndAggregationDate(merchantId, date)).thenReturn(Optional.empty());
        when(paymentReportRepository.countByMerchantIdAndEventTimestampAfter(merchantId, startOfDay)).thenReturn(null);
        when(paymentReportRepository.sumSuccessfulAmountByMerchantIdAndEventTimestampAfter(merchantId, startOfDay)).thenReturn(null);
        when(paymentReportRepository.countFraudRejectedByMerchantIdAndEventTimestampAfter(merchantId, startOfDay)).thenReturn(null);
        when(dailyAggregationRepository.save(any(DailyAggregation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        reportingService.generateDailyAggregationForMerchant(merchantId, date, startOfDay, endOfDay);

        ArgumentCaptor<DailyAggregation> aggregationCaptor = ArgumentCaptor.forClass(DailyAggregation.class);
        verify(dailyAggregationRepository).save(aggregationCaptor.capture());

        DailyAggregation savedAggregation = aggregationCaptor.getValue();
        assertEquals(0, savedAggregation.getTotalTransactions());
        assertEquals(BigDecimal.ZERO, savedAggregation.getTotalAmount());
        assertEquals(0, savedAggregation.getFraudRejectedTransactions());
    }

    @Test
    void generateDailyAggregationForMerchant_ShouldCalculateAverageTransactionValue() {
        String merchantId = "MERCHANT-001";
        LocalDate date = LocalDate.of(2024, 1, 15);
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);

        when(dailyAggregationRepository.findByMerchantIdAndAggregationDate(merchantId, date)).thenReturn(Optional.empty());
        when(paymentReportRepository.countByMerchantIdAndEventTimestampAfter(merchantId, startOfDay)).thenReturn(3L);
        when(paymentReportRepository.sumSuccessfulAmountByMerchantIdAndEventTimestampAfter(merchantId, startOfDay)).thenReturn(new BigDecimal("300.00"));
        when(paymentReportRepository.countFraudRejectedByMerchantIdAndEventTimestampAfter(merchantId, startOfDay)).thenReturn(0L);
        when(dailyAggregationRepository.save(any(DailyAggregation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        reportingService.generateDailyAggregationForMerchant(merchantId, date, startOfDay, endOfDay);

        ArgumentCaptor<DailyAggregation> aggregationCaptor = ArgumentCaptor.forClass(DailyAggregation.class);
        verify(dailyAggregationRepository).save(aggregationCaptor.capture());

        DailyAggregation savedAggregation = aggregationCaptor.getValue();
        assertEquals(new BigDecimal("100.00"), savedAggregation.getAverageTransactionValue());
    }

    @Test
    void getMerchantReports_ShouldReturnReports() {
        String merchantId = "MERCHANT-001";
        PaymentReport report1 = new PaymentReport();
        report1.setPaymentId("PAY-001");
        report1.setMerchantId(merchantId);

        PaymentReport report2 = new PaymentReport();
        report2.setPaymentId("PAY-002");
        report2.setMerchantId(merchantId);

        when(paymentReportRepository.findByMerchantId(merchantId)).thenReturn(Arrays.asList(report1, report2));

        List<PaymentReport> reports = reportingService.getMerchantReports(merchantId);

        assertEquals(2, reports.size());
        assertEquals("PAY-001", reports.get(0).getPaymentId());
        assertEquals("PAY-002", reports.get(1).getPaymentId());

        verify(paymentReportRepository).findByMerchantId(merchantId);
    }

    @Test
    void getMerchantAggregations_ShouldReturnAggregations() {
        String merchantId = "MERCHANT-001";
        DailyAggregation aggregation1 = new DailyAggregation();
        aggregation1.setMerchantId(merchantId);
        aggregation1.setAggregationDate(LocalDate.of(2024, 1, 15));

        DailyAggregation aggregation2 = new DailyAggregation();
        aggregation2.setMerchantId(merchantId);
        aggregation2.setAggregationDate(LocalDate.of(2024, 1, 14));

        when(dailyAggregationRepository.findByMerchantIdOrderByAggregationDateDesc(merchantId)).thenReturn(Arrays.asList(aggregation1, aggregation2));

        List<DailyAggregation> aggregations = reportingService.getMerchantAggregations(merchantId);

        assertEquals(2, aggregations.size());
        assertEquals(LocalDate.of(2024, 1, 15), aggregations.get(0).getAggregationDate());
        assertEquals(LocalDate.of(2024, 1, 14), aggregations.get(1).getAggregationDate());

        verify(dailyAggregationRepository).findByMerchantIdOrderByAggregationDateDesc(merchantId);
    }

    @Test
    void getAllAggregationsForDate_ShouldReturnAggregations() {
        LocalDate date = LocalDate.of(2024, 1, 15);
        DailyAggregation aggregation1 = new DailyAggregation();
        aggregation1.setMerchantId("MERCHANT-001");
        aggregation1.setAggregationDate(date);

        DailyAggregation aggregation2 = new DailyAggregation();
        aggregation2.setMerchantId("MERCHANT-002");
        aggregation2.setAggregationDate(date);

        when(dailyAggregationRepository.findByAggregationDateOrderByAggregationDateDesc(date)).thenReturn(Arrays.asList(aggregation1, aggregation2));

        List<DailyAggregation> aggregations = reportingService.getAllAggregationsForDate(date);

        assertEquals(2, aggregations.size());
        assertEquals("MERCHANT-001", aggregations.get(0).getMerchantId());
        assertEquals("MERCHANT-002", aggregations.get(1).getMerchantId());

        verify(dailyAggregationRepository).findByAggregationDateOrderByAggregationDateDesc(date);
    }
}
