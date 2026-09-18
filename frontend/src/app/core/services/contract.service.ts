import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Contract, ContractRequest } from '../../shared/models/contract';

@Injectable({ providedIn: 'root' })
export class ContractService {
  private readonly http = inject(HttpClient);

  list(): Observable<Contract[]> {
    return this.http.get<Contract[]>('/api/contracts');
  }

  get(id: number): Observable<Contract> {
    return this.http.get<Contract>(`/api/contracts/${id}`);
  }

  create(request: ContractRequest): Observable<Contract> {
    return this.http.post<Contract>('/api/contracts', request);
  }

  update(id: number, request: ContractRequest): Observable<Contract> {
    return this.http.put<Contract>(`/api/contracts/${id}`, request);
  }

  activate(id: number): Observable<Contract> {
    return this.http.post<Contract>(`/api/contracts/${id}/activate`, {});
  }

  terminate(id: number): Observable<Contract> {
    return this.http.post<Contract>(`/api/contracts/${id}/terminate`, {});
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`/api/contracts/${id}`);
  }
}
