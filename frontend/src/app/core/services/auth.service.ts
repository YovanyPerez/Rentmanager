import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { AuthResponse, AuthUser, LoginRequest, RegisterRequest } from '../../shared/models/auth';

const TOKEN_KEY = 'rentmanager.token';
const USER_KEY = 'rentmanager.auth';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly currentUser = signal<AuthUser | null>(readStoredUser());

  readonly user = this.currentUser.asReadonly();
  readonly isAuthenticated = computed(() => this.currentUser() !== null);
  readonly role = computed(() => this.currentUser()?.role ?? null);

  login(credentials: LoginRequest): Observable<AuthUser> {
    return this.http
      .post<AuthResponse>('/api/auth/login', credentials)
      .pipe(tap((response) => this.store(response)));
  }

  register(request: RegisterRequest): Observable<AuthUser> {
    return this.http
      .post<AuthResponse>('/api/auth/register', request)
      .pipe(tap((response) => this.store(response)));
  }

  token(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  logout(): void {
    clearStorage();
    this.currentUser.set(null);
    void this.router.navigate(['/login']);
  }

  private store(response: AuthResponse): AuthUser {
    const user: AuthUser = {
      userId: response.userId,
      email: response.email,
      fullName: response.fullName,
      role: response.role,
      expiresAt: response.expiresAt,
    };
    localStorage.setItem(TOKEN_KEY, response.token);
    localStorage.setItem(USER_KEY, JSON.stringify(user));
    this.currentUser.set(user);
    return user;
  }
}

function readStoredUser(): AuthUser | null {
  const raw = localStorage.getItem(USER_KEY);
  if (!raw) {
    return null;
  }
  try {
    const user = JSON.parse(raw) as AuthUser;
    if (new Date(user.expiresAt).getTime() <= Date.now()) {
      clearStorage();
      return null;
    }
    return user;
  } catch {
    clearStorage();
    return null;
  }
}

function clearStorage(): void {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
}
