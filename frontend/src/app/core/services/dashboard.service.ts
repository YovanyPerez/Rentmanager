import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { DashboardStats } from '../../shared/models/dashboard';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);

  statistics(): Observable<DashboardStats> {
    return this.http.get<DashboardStats>('/api/dashboard');
  }
}
