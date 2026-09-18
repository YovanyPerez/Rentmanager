import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Owner, OwnerRequest } from '../../shared/models/owner';

@Injectable({ providedIn: 'root' })
export class OwnerService {
  private readonly http = inject(HttpClient);

  list(): Observable<Owner[]> {
    return this.http.get<Owner[]>('/api/owners');
  }

  get(id: number): Observable<Owner> {
    return this.http.get<Owner>(`/api/owners/${id}`);
  }

  create(request: OwnerRequest): Observable<Owner> {
    return this.http.post<Owner>('/api/owners', request);
  }

  update(id: number, request: OwnerRequest): Observable<Owner> {
    return this.http.put<Owner>(`/api/owners/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`/api/owners/${id}`);
  }
}
