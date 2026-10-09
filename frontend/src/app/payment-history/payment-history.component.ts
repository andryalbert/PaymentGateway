import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';

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
  selector: 'app-payment-history',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './payment-history.component.html',
  styleUrl: './payment-history.component.css'
})
export class PaymentHistoryComponent implements OnInit {
  private apiUrl = 'http://localhost/api/v1/payments';
  
  payments: PaymentResponse[] = [];
  loading = false;
  error: string | null = null;
  merchantId = 'MERCHANT-001';

  constructor(private http: HttpClient, private router: Router) {}

  ngOnInit() {
    this.loadPayments();
  }

  loadPayments() {
    this.loading = true;
    this.error = null;

    this.http.get<PaymentResponse[]>(`${this.apiUrl}/merchant/${this.merchantId}`).subscribe({
      next: (response) => {
        this.loading = false;
        this.payments = response;
      },
      error: (err) => {
        this.loading = false;
        this.error = 'Failed to load payment history. Please try again.';
      }
    });
  }

  goToPayment() {
    this.router.navigate(['/payment']);
  }

  refundPayment(transactionId: string) {
    if (!confirm('Are you sure you want to refund this payment?')) {
      return;
    }

    this.http.post<PaymentResponse>(`${this.apiUrl}/refund/${transactionId}`, {}).subscribe({
      next: (response) => {
        alert('Payment refunded successfully');
        this.loadPayments();
      },
      error: (err) => {
        alert('Failed to refund payment: ' + (err.error?.message || 'Unknown error'));
      }
    });
  }

  getStatusBadgeClass(status: string): string {
    switch (status.toLowerCase()) {
      case 'completed':
        return 'badge-completed';
      case 'failed':
        return 'badge-failed';
      case 'pending':
        return 'badge-pending';
      case 'refunded':
        return 'badge-refunded';
      default:
        return '';
    }
  }

  formatDate(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleString();
  }
}
