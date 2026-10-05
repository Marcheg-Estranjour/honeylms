import { HttpErrorResponse } from '@angular/common/http';
import { GENERIC_ERROR_MESSAGE, toErrorMessage } from './api-error.model';

describe('toErrorMessage', () => {
  it('returns the backend message when the body is an ApiError', () => {
    const error = new HttpErrorResponse({
      status: 409,
      error: {
        timestamp: '2026-10-05T10:00:00Z',
        status: 409,
        error: 'Conflict',
        message: 'Email already in use.',
        path: '/api/auth/register',
      },
    });
    expect(toErrorMessage(error)).toBe('Email already in use.');
  });

  it('falls back to a generic message otherwise', () => {
    expect(toErrorMessage(new HttpErrorResponse({ status: 0 }))).toBe(GENERIC_ERROR_MESSAGE);
    expect(toErrorMessage(new Error('boom'))).toBe(GENERIC_ERROR_MESSAGE);
  });
});
