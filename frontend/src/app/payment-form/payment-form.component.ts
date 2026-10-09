import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';

interface PaymentRequest {
  cardNumber: string;
  cardHolder: string;
  expiryDate: string;
  cvv: string;
  amount: number;
  currency: string;
  merchantId: string;
  description?: string;
}

interface PaymentResponse {
  id: number;
  transactionId: string;
  cardNumber: string;
  cardHolder: string;
  amount: number;
  currency: string;
  status: string;
  merchantId: string;
  description?: string;
  createdAt: string;
  updatedAt: string;
  message: string;
}

@Component({
  selector: 'app-payment-form',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './payment-form.component.html',
  styleUrl: './payment-form.component.css'
})
export class PaymentFormComponent {
  private apiUrl = 'http://localhost/api/v1/payments';
  
  payment: PaymentRequest = {
    cardNumber: '',
    cardHolder: '',
    expiryDate: '',
    cvv: '',
    amount: 0,
    currency: 'USD',
    merchantId: 'MERCHANT-001',
    description: ''
  };

  loading = false;
  error: string | null = null;
  success: string | null = null;
  lastPayment: PaymentResponse | null = null;

  constructor(private http: HttpClient, private router: Router) {}

  processPayment() {
    if (!this.validateForm()) {
      return;
    }

    this.loading = true;
    this.error = null;
    this.success = null;

    this.http.post<PaymentResponse>(this.apiUrl, this.payment).subscribe({
      next: (response) => {
        this.loading = false;
        this.lastPayment = response;
        this.success = response.message;
        this.resetForm();
      },
      error: (err) => {
        this.loading = false;
        this.error = err.error?.message || 'Payment processing failed. Please try again.';
      }
    });
  }

  validateForm(): boolean {
    if (!this.payment.cardNumber || this.payment.cardNumber.length !== 16) {
      this.error = 'Please enter a valid 16-digit card number';
      return false;
    }
    if (!this.payment.cardHolder || this.payment.cardHolder.length < 2) {
      this.error = 'Please enter the card holder name';
      return false;
    }
    if (!this.payment.expiryDate || !/^(0[1-9]|1[0-2])\/\d{2}$/.test(this.payment.expiryDate)) {
      this.error = 'Please enter a valid expiry date (MM/YY)';
      return false;
    }
    if (!this.payment.cvv || this.payment.cvv.length < 3 || this.payment.cvv.length > 4) {
      this.error = 'Please enter a valid CVV (3 or 4 digits)';
      return false;
    }
    if (!this.payment.amount || this.payment.amount <= 0) {
      this.error = 'Please enter a valid amount greater than 0';
      return false;
    }
    if (!this.payment.currency || this.payment.currency.length !== 3) {
      this.error = 'Please enter a valid 3-character currency code';
      return false;
    }
    return true;
  }

  resetForm() {
    this.payment = {
      cardNumber: '',
      cardHolder: '',
      expiryDate: '',
      cvv: '',
      amount: 0,
      currency: 'USD',
      merchantId: 'MERCHANT-001',
      description: ''
    };
  }

  goToHistory() {
    this.router.navigate(['/history']);
  }

  formatCardNumber() {
    let value = this.payment.cardNumber.replace(/\D/g, '');
    if (value.length > 16) value = value.substring(0, 16);
    this.payment.cardNumber = value;
  }

  formatExpiryDate(event: Event) {
    const input = event.target as HTMLInputElement;
    let value = input.value.replace(/\D/g, '');
    if (value.length >= 2) {
      value = value.substring(0, 2) + '/' + value.substring(2, 4);
    }
    this.payment.expiryDate = value;
  }

  formatCvv() {
    let value = this.payment.cvv.replace(/\D/g, '');
    if (value.length > 4) value = value.substring(0, 4);
    this.payment.cvv = value;
  }
}
