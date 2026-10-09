import { Routes } from '@angular/router';
import { PaymentFormComponent } from './payment-form/payment-form.component';
import { PaymentHistoryComponent } from './payment-history/payment-history.component';

export const routes: Routes = [
  { path: '', redirectTo: '/payment', pathMatch: 'full' },
  { path: 'payment', component: PaymentFormComponent },
  { path: 'history', component: PaymentHistoryComponent }
];
