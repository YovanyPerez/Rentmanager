import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  Maintenance,
  MaintenanceCreateRequest,
  MaintenanceUpdateRequest,
} from '../../shared/models/maintenance';

@Injectable({ providedIn: 'root' })
export class MaintenanceService {
  private readonly http = inject(HttpClient);

  list(): Observable<Maintenance[]> {
    return this.http.get<Maintenance[]>('/api/maintenance');
  }

  get(id: number): Observable<Maintenance> {
    return this.http.get<Maintenance>(`/api/maintenance/${id}`);
  }

  create(request: MaintenanceCreateRequest): Observable<Maintenance> {
    return this.http.post<Maintenance>('/api/maintenance', request);
  }

  update(id: number, request: MaintenanceUpdateRequest): Observable<Maintenance> {
    return this.http.put<Maintenance>(`/api/maintenance/${id}`, request);
  }
}
