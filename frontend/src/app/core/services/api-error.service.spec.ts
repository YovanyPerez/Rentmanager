import { HttpErrorResponse } from '@angular/common/http';
import { ApiErrorService } from './api-error.service';

describe('ApiErrorService', () => {
  const service = new ApiErrorService();

  it('maps backend codes to errors.<CODE>', () => {
    const error = new HttpErrorResponse({ status: 404, error: { code: 'PROPERTY_NOT_FOUND' } });
    expect(service.keyOf(error)).toBe('errors.PROPERTY_NOT_FOUND');
  });

  it('falls back to errors.UNKNOWN', () => {
    expect(service.keyOf(new Error('boom'))).toBe('errors.UNKNOWN');
    expect(service.keyOf(new HttpErrorResponse({ status: 500 }))).toBe('errors.UNKNOWN');
  });
});
