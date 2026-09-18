import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { UserSummary } from '../../shared/models/user';

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly http = inject(HttpClient);

  list(): Observable<UserSummary[]> {
    return this.http.get<UserSummary[]>('/api/users');
  }
}
