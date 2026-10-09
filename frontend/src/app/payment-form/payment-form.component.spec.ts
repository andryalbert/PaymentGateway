import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { PaymentFormComponent } from './payment-form.component';

describe('PaymentFormComponent', () => {
  let component: PaymentFormComponent;
  let fixture: ComponentFixture<PaymentFormComponent>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PaymentFormComponent],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideRouter([])
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(PaymentFormComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize payment with default values', () => {
    expect(component.payment.currency).toBe('USD');
    expect(component.payment.merchantId).toBe('MERCHANT-001');
    expect(component.payment.amount).toBe(0);
  });

  it('should validate form with valid data', () => {
    component.payment = {
      cardNumber: '1234567890123456',
      cardHolder: 'John Doe',
      expiryDate: '12/25',
      cvv: '123',
      amount: 100,
      currency: 'USD',
      merchantId: 'MERCHANT-001',
      description: 'Test payment'
    };
    expect(component.validateForm()).toBeTrue();
  });

  it('should invalidate form with invalid card number', () => {
    component.payment.cardNumber = '123';
    expect(component.validateForm()).toBeFalse();
    expect(component.error).toContain('16-digit card number');
  });

  it('should invalidate form with invalid card holder', () => {
    component.payment.cardHolder = 'A';
    expect(component.validateForm()).toBeFalse();
    expect(component.error).toContain('card holder name');
  });

  it('should invalidate form with invalid expiry date', () => {
    component.payment.expiryDate = '13/25';
    expect(component.validateForm()).toBeFalse();
    expect(component.error).toContain('expiry date');
  });

  it('should invalidate form with invalid CVV', () => {
    component.payment.cvv = '12';
    expect(component.validateForm()).toBeFalse();
    expect(component.error).toContain('CVV');
  });

  it('should invalidate form with invalid amount', () => {
    component.payment.amount = 0;
    expect(component.validateForm()).toBeFalse();
    expect(component.error).toContain('amount greater than 0');
  });

  it('should invalidate form with invalid currency', () => {
    component.payment.currency = 'US';
    expect(component.validateForm()).toBeFalse();
    expect(component.error).toContain('3-character currency code');
  });

  it('should format card number to remove non-digits', () => {
    component.payment.cardNumber = '1234-5678-9012-3456';
    component.formatCardNumber();
    expect(component.payment.cardNumber).toBe('1234567890123456');
  });

  it('should limit card number to 16 digits', () => {
    component.payment.cardNumber = '12345678901234567890';
    component.formatCardNumber();
    expect(component.payment.cardNumber).toBe('1234567890123456');
  });

  it('should format expiry date correctly', () => {
    component.payment.expiryDate = '1225';
    component.formatExpiryDate();
    expect(component.payment.expiryDate).toBe('12/25');
  });

  it('should format CVV to remove non-digits', () => {
    component.payment.cvv = '1-2-3';
    component.formatCvv();
    expect(component.payment.cvv).toBe('123');
  });

  it('should limit CVV to 4 digits', () => {
    component.payment.cvv = '12345';
    component.formatCvv();
    expect(component.payment.cvv).toBe('1234');
  });

  it('should reset form to default values', () => {
    component.payment.amount = 100;
    component.payment.cardNumber = '1234567890123456';
    component.resetForm();
    expect(component.payment.amount).toBe(0);
    expect(component.payment.cardNumber).toBe('');
    expect(component.payment.currency).toBe('USD');
  });

  it('should process payment successfully', () => {
    component.payment = {
      cardNumber: '1234567890123456',
      cardHolder: 'John Doe',
      expiryDate: '12/25',
      cvv: '123',
      amount: 100,
      currency: 'USD',
      merchantId: 'MERCHANT-001',
      description: 'Test payment'
    };

    component.processPayment();

    const req = httpMock.expectOne('http://localhost:8080/api/payments/process');
    expect(req.request.method).toBe('POST');
    req.flush({
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
    });

    expect(component.loading).toBeFalse();
    expect(component.success).toBe('Payment successful');
    expect(component.lastPayment).toBeTruthy();
  });

  it('should handle payment error', () => {
    component.payment = {
      cardNumber: '1234567890123456',
      cardHolder: 'John Doe',
      expiryDate: '12/25',
      cvv: '123',
      amount: 100,
      currency: 'USD',
      merchantId: 'MERCHANT-001',
      description: 'Test payment'
    };

    component.processPayment();

    const req = httpMock.expectOne('http://localhost:8080/api/payments/process');
    req.flush({ message: 'Payment failed' }, { status: 400, statusText: 'Bad Request' });

    expect(component.loading).toBeFalse();
    expect(component.error).toBe('Payment failed');
  });
});
