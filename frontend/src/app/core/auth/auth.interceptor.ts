import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';

/** Public auth endpoints: never send a token to them. */
const PUBLIC_AUTH_URLS = ['/api/auth/login', '/api/auth/register'];

/**
 * Adds `Authorization: Bearer <JWT>` to requests sent to our own API (`/api/...`) only.
 * Requests to any other origin never receive the token.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const isOwnApi = req.url.startsWith('/api/');
  const isPublicAuth = PUBLIC_AUTH_URLS.includes(req.url);
  if (!isOwnApi || isPublicAuth) {
    return next(req);
  }

  const token = inject(AuthService).getAccessToken();
  if (!token) {
    return next(req);
  }

  return next(req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
};
