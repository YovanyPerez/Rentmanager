import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Property, PropertyImage, PropertyRequest, PropertyStatus } from '../../shared/models/property';

@Injectable({ providedIn: 'root' })
export class PropertyService {
  private readonly http = inject(HttpClient);

  list(): Observable<Property[]> {
    return this.http.get<Property[]>('/api/properties');
  }

  get(id: number): Observable<Property> {
    return this.http.get<Property>(`/api/properties/${id}`);
  }

  create(request: PropertyRequest): Observable<Property> {
    return this.http.post<Property>('/api/properties', request);
  }

  update(id: number, request: PropertyRequest): Observable<Property> {
    return this.http.put<Property>(`/api/properties/${id}`, request);
  }

  changeStatus(id: number, status: PropertyStatus): Observable<Property> {
    return this.http.patch<Property>(`/api/properties/${id}/status`, { status });
  }

  deactivate(id: number): Observable<void> {
    return this.http.delete<void>(`/api/properties/${id}`);
  }

  uploadImage(id: number, file: File): Observable<PropertyImage> {
    const data = new FormData();
    data.append('file', file);
    return this.http.post<PropertyImage>(`/api/properties/${id}/images`, data);
  }

  deleteImage(id: number, imageId: number): Observable<void> {
    return this.http.delete<void>(`/api/properties/${id}/images/${imageId}`);
  }

  setCover(id: number, imageId: number): Observable<PropertyImage[]> {
    return this.http.put<PropertyImage[]>(`/api/properties/${id}/images/${imageId}/cover`, {});
  }

  reorderImages(id: number, imageIds: number[]): Observable<PropertyImage[]> {
    return this.http.put<PropertyImage[]>(`/api/properties/${id}/images/order`, { imageIds });
  }
}
