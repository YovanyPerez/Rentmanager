import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { AppRole } from '../../shared/models/auth';
import { authGuard } from './auth.guard';
import { roleGuard } from './role.guard';

describe('guards', () => {
  const route = {} as ActivatedRouteSnapshot;
  const state = { url: '/properties' } as RouterStateSnapshot;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
  });

  it('authGuard redirects to login with returnUrl when there is no session', () => {
    const result = TestBed.runInInjectionContext(() => authGuard(route, state));

    expect(result).toBeInstanceOf(UrlTree);
    expect(String(result)).toBe('/login?returnUrl=%2Fproperties');
  });

  it('authGuard allows authenticated users', () => {
    login('TENANT');

    const result = TestBed.runInInjectionContext(() => authGuard(route, state));

    expect(result).toBe(true);
  });

  it('roleGuard allows matching roles', () => {
    login('ADMIN');

    const result = TestBed.runInInjectionContext(() => roleGuard('ADMIN')(route, state));

    expect(result).toBe(true);
  });

  it('roleGuard redirects users without the required role', () => {
    login('TENANT');

    const result = TestBed.runInInjectionContext(() => roleGuard('ADMIN')(route, state));

    expect(result).toBeInstanceOf(UrlTree);
    expect(String(result)).toBe('/home');
  });

  function login(role: AppRole): void {
    localStorage.setItem('rentmanager.token', 'jwt-token');
    localStorage.setItem(
      'rentmanager.auth',
      JSON.stringify({
        userId: 1,
        email: 'user@test.local',
        fullName: 'Test User',
        role,
        expiresAt: new Date(Date.now() + 3_600_000).toISOString(),
      }),
    );
    TestBed.inject(Router);
    TestBed.inject(AuthService);
  }
});
