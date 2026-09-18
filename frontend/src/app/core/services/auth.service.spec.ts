import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  const authResponse = {
    token: 'jwt-token',
    expiresAt: new Date(Date.now() + 3_600_000).toISOString(),
    userId: 7,
    email: 'user@test.local',
    fullName: 'Test User',
    role: 'TENANT' as const,
  };

  let service: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    configure();
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('stores the session on successful login', () => {
    service.login({ email: 'user@test.local', password: 'secret123' }).subscribe((user) => {
      expect(user.fullName).toBe('Test User');
    });

    http.expectOne('/api/auth/login').flush(authResponse);

    expect(service.isAuthenticated()).toBe(true);
    expect(service.token()).toBe('jwt-token');
    expect(service.role()).toBe('TENANT');
  });

  it('keeps the session empty when login fails', () => {
    service.login({ email: 'user@test.local', password: 'wrong' }).subscribe({ error: () => undefined });

    http
      .expectOne('/api/auth/login')
      .flush({ code: 'INVALID_CREDENTIALS' }, { status: 401, statusText: 'Unauthorized' });

    expect(service.isAuthenticated()).toBe(false);
    expect(service.token()).toBeNull();
  });

  it('clears the session on logout', () => {
    service.login({ email: 'user@test.local', password: 'secret123' }).subscribe();
    http.expectOne('/api/auth/login').flush(authResponse);

    service.logout();

    expect(service.isAuthenticated()).toBe(false);
    expect(localStorage.getItem('rentmanager.token')).toBeNull();
  });

  it('discards an expired stored session', () => {
    localStorage.setItem(
      'rentmanager.auth',
      JSON.stringify({ ...authResponse, expiresAt: new Date(Date.now() - 1000).toISOString() }),
    );
    localStorage.setItem('rentmanager.token', 'stale-token');

    TestBed.resetTestingModule();
    configure();

    expect(TestBed.inject(AuthService).isAuthenticated()).toBe(false);
  });

  function configure(): void {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'login', children: [] }]),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
  }
});
