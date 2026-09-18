import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AppRole } from '../../shared/models/auth';
import { AuthService } from '../services/auth.service';

export function roleGuard(...roles: AppRole[]): CanActivateFn {
  return () => {
    const auth = inject(AuthService);
    const router = inject(Router);
    const role = auth.role();
    if (role && roles.includes(role)) {
      return true;
    }
    return router.createUrlTree(['/home']);
  };
}
