import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  let client: HttpClient;
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'login', children: [] }]),
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    client = TestBed.inject(HttpClient);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('adds the bearer token to API requests when a token exists', () => {
    localStorage.setItem('rentmanager.token', 'jwt-token');

    client.get('/api/properties').subscribe();

    const request = http.expectOne('/api/properties');
    expect(request.request.headers.get('Authorization')).toBe('Bearer jwt-token');
    request.flush([]);
  });

  it('does not add a header when there is no token', () => {
    client.get('/api/health').subscribe();

    const request = http.expectOne('/api/health');
    expect(request.request.headers.has('Authorization')).toBe(false);
    request.flush({ status: 'UP' });
  });

  it('clears the session when a protected request returns 401', () => {
    localStorage.setItem('rentmanager.token', 'jwt-token');
    localStorage.setItem('rentmanager.auth', JSON.stringify({ userId: 1 }));

    client.get('/api/properties').subscribe({ error: () => undefined });
    http
      .expectOne('/api/properties')
      .flush({ code: 'UNAUTHORIZED' }, { status: 401, statusText: 'Unauthorized' });

    expect(localStorage.getItem('rentmanager.token')).toBeNull();
    expect(localStorage.getItem('rentmanager.auth')).toBeNull();
  });

  it('keeps the session on failed login', () => {
    localStorage.setItem('rentmanager.token', 'keep-me');

    client.post('/api/auth/login', {}).subscribe({ error: () => undefined });
    http
      .expectOne('/api/auth/login')
      .flush({ code: 'INVALID_CREDENTIALS' }, { status: 401, statusText: 'Unauthorized' });

    expect(localStorage.getItem('rentmanager.token')).toBe('keep-me');
  });
});
