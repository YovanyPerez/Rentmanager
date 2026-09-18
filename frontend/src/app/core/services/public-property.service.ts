import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { PublicProperty } from '../../shared/models/public-property';

@Injectable({ providedIn: 'root' })
export class PublicPropertyService {
  private readonly http = inject(HttpClient);

  search(query = ''): Observable<PublicProperty[]> {
    const params = query.trim() === '' ? new HttpParams() : new HttpParams().set('query', query.trim());
    return this.http.get<PublicProperty[]>('/api/public/properties', { params });
  }

  get(id: number): Observable<PublicProperty> {
    return this.http.get<PublicProperty>(`/api/public/properties/${id}`);
  }
}
