import { HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { ApiError } from '../../shared/models/api-error';

@Injectable({ providedIn: 'root' })
export class ApiErrorService {
  /** Translation key for a failed API request (`errors.<CODE>`, fallback `errors.UNKNOWN`). */
  keyOf(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      const code = (error.error as ApiError | null)?.code;
      if (code) {
        return `errors.${code}`;
      }
    }
    return 'errors.UNKNOWN';
  }
}
