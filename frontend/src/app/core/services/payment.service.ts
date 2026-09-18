import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Payment, PaymentRequest, PaymentUpdateRequest } from '../../shared/models/payment';

@Injectable({ providedIn: 'root' })
export class PaymentService {
  private readonly http = inject(HttpClient);

  list(): Observable<Payment[]> {
    return this.http.get<Payment[]>('/api/payments');
  }

  get(id: number): Observable<Payment> {
    return this.http.get<Payment>(`/api/payments/${id}`);
  }

  create(request: PaymentRequest): Observable<Payment> {
    return this.http.post<Payment>('/api/payments', request);
  }

  update(id: number, request: PaymentUpdateRequest): Observable<Payment> {
    return this.http.put<Payment>(`/api/payments/${id}`, request);
  }
}
