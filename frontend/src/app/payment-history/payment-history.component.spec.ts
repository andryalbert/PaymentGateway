import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { PaymentHistoryComponent } from './payment-history.component';

describe('PaymentHistoryComponent', () => {
  let component: PaymentHistoryComponent;
  let fixture: ComponentFixture<PaymentHistoryComponent>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PaymentHistoryComponent],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideRouter([])
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(PaymentHistoryComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize with default values', () => {
    expect(component.payments).toEqual([]);
    expect(component.loading).toBeFalse();
    expect(component.error).toBeNull();
    expect(component.merchantId).toBe('MERCHANT-001');
  });

  it('should load payments on init', () => {
    const mockPayments = [
      {
        id: 1,
        transactionId: 'TXN123',
        cardNumber: '1234567890123456',
        cardHolder: 'John Doe',
        amount: 100,
        currency: 'USD',
        status: 'completed',
        merchantId: 'MERCHANT-001',
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        message: 'Payment successful'
      }
    ];

    component.ngOnInit();

    const req = httpMock.expectOne('http://localhost/api/v1/payments/merchant/MERCHANT-001');
    expect(req.request.method).toBe('GET');
    req.flush(mockPayments);

    expect(component.payments).toEqual(mockPayments);
    expect(component.loading).toBeFalse();
  });

  it('should handle load payments error', () => {
    component.ngOnInit();

    const req = httpMock.expectOne('http://localhost/api/v1/payments/merchant/MERCHANT-001');
    req.flush('Error', { status: 500, statusText: 'Internal Server Error' });

    expect(component.loading).toBeFalse();
    expect(component.error).toBe('Failed to load payment history. Please try again.');
  });

  it('should load payments successfully', () => {
    const mockPayments = [
      {
        id: 1,
        transactionId: 'TXN123',
        cardNumber: '1234567890123456',
        cardHolder: 'John Doe',
        amount: 100,
        currency: 'USD',
        status: 'completed',
        merchantId: 'MERCHANT-001',
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        message: 'Payment successful'
      }
    ];

    component.loadPayments();

    const req = httpMock.expectOne('http://localhost/api/v1/payments/merchant/MERCHANT-001');
    req.flush(mockPayments);

    expect(component.payments).toEqual(mockPayments);
    expect(component.loading).toBeFalse();
  });

  it('should refund payment successfully', () => {
    const mockResponse = {
      id: 1,
      transactionId: 'TXN123',
      cardNumber: '1234567890123456',
      cardHolder: 'John Doe',
      amount: 100,
      currency: 'USD',
      status: 'refunded',
      merchantId: 'MERCHANT-001',
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      message: 'Payment refunded'
    };

    spyOn(window, 'confirm').and.returnValue(true);
    spyOn(window, 'alert');

    component.refundPayment('TXN123');

    const req = httpMock.expectOne('http://localhost/api/v1/payments/refund/TXN123');
    expect(req.request.method).toBe('POST');
    req.flush(mockResponse);

    const loadReq = httpMock.expectOne('http://localhost/api/v1/payments/merchant/MERCHANT-001');
    expect(loadReq.request.method).toBe('GET');
    loadReq.flush([]);

    expect(window.alert).toHaveBeenCalledWith('Payment refunded successfully');
  });

  it('should not refund payment when user cancels', () => {
    spyOn(window, 'confirm').and.returnValue(false);

    component.refundPayment('TXN123');

    httpMock.expectNone('http://localhost/api/v1/payments/refund/TXN123');
  });

  it('should handle refund payment error', () => {
    spyOn(window, 'confirm').and.returnValue(true);
    spyOn(window, 'alert');

    component.refundPayment('TXN123');

    const req = httpMock.expectOne('http://localhost/api/v1/payments/refund/TXN123');
    req.flush({ message: 'Refund failed' }, { status: 400, statusText: 'Bad Request' });

    expect(window.alert).toHaveBeenCalledWith('Failed to refund payment: Refund failed');
  });

  it('should return correct badge class for completed status', () => {
    expect(component.getStatusBadgeClass('completed')).toBe('badge-completed');
  });

  it('should return correct badge class for failed status', () => {
    expect(component.getStatusBadgeClass('failed')).toBe('badge-failed');
  });

  it('should return correct badge class for pending status', () => {
    expect(component.getStatusBadgeClass('pending')).toBe('badge-pending');
  });

  it('should return correct badge class for refunded status', () => {
    expect(component.getStatusBadgeClass('refunded')).toBe('badge-refunded');
  });

  it('should return empty string for unknown status', () => {
    expect(component.getStatusBadgeClass('unknown')).toBe('');
  });

  it('should return correct badge class for uppercase status', () => {
    expect(component.getStatusBadgeClass('COMPLETED')).toBe('badge-completed');
  });

  it('should format date correctly', () => {
    const dateString = '2024-01-15T10:30:00Z';
    const formattedDate = component.formatDate(dateString);
    expect(formattedDate).toBeTruthy();
    expect(typeof formattedDate).toBe('string');
  });

  it('should reload payments after successful refund', () => {
    const mockResponse = {
      id: 1,
      transactionId: 'TXN123',
      cardNumber: '1234567890123456',
      cardHolder: 'John Doe',
      amount: 100,
      currency: 'USD',
      status: 'refunded',
      merchantId: 'MERCHANT-001',
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      message: 'Payment refunded'
    };

    spyOn(window, 'confirm').and.returnValue(true);
    spyOn(window, 'alert');
    spyOn(component, 'loadPayments');

    component.refundPayment('TXN123');

    const req = httpMock.expectOne('http://localhost/api/v1/payments/refund/TXN123');
    req.flush(mockResponse);

    expect(component.loadPayments).toHaveBeenCalled();
  });
});
