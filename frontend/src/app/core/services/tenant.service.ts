import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Tenant, TenantRequest } from '../../shared/models/tenant';

@Injectable({ providedIn: 'root' })
export class TenantService {
  private readonly http = inject(HttpClient);

  list(): Observable<Tenant[]> {
    return this.http.get<Tenant[]>('/api/tenants');
  }

  get(id: number): Observable<Tenant> {
    return this.http.get<Tenant>(`/api/tenants/${id}`);
  }

  create(request: TenantRequest): Observable<Tenant> {
    return this.http.post<Tenant>('/api/tenants', request);
  }

  update(id: number, request: TenantRequest): Observable<Tenant> {
    return this.http.put<Tenant>(`/api/tenants/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`/api/tenants/${id}`);
  }
}
