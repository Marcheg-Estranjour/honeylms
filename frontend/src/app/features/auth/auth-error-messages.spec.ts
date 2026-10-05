import { HttpErrorResponse } from '@angular/common/http';
import { GENERIC_ERROR_MESSAGE } from '../../core/api/api-error.model';
import { loginErrorMessage, registerErrorMessage } from './auth-error-messages';

const httpError = (status: number) => new HttpErrorResponse({ status });

describe('auth error messages', () => {
  it('maps login errors to French messages', () => {
    expect(loginErrorMessage(httpError(401))).toBe('Email ou mot de passe incorrect.');
    expect(loginErrorMessage(httpError(403))).toContain('désactivé');
    expect(loginErrorMessage(httpError(0))).toContain('injoignable');
    expect(loginErrorMessage(httpError(500))).toBe(GENERIC_ERROR_MESSAGE);
  });

  it('maps register errors to French messages', () => {
    expect(registerErrorMessage(httpError(409))).toContain('existe déjà');
    expect(registerErrorMessage(httpError(400))).toContain('invalides');
    expect(registerErrorMessage(new Error('boom'))).toBe(GENERIC_ERROR_MESSAGE);
  });
});
