import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../auth/auth.service';

/**
 * Global HTTP error handling.
 *
 * - 401 on a protected endpoint = missing, invalid or expired JWT: the session is
 *   cleared and the user is sent back to the login page (with a returnUrl).
 *   A 401 on /api/auth/login means "bad credentials": it is left to the login page.
 * - Every other error (400, 403, 404, 409...) is rethrown untouched: the calling
 *   page displays the ApiError message where it makes sense.
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  return next(req).pipe(
    catchError((error: unknown) => {
      const isSessionLost =
        error instanceof HttpErrorResponse &&
        error.status === 401 &&
        !req.url.startsWith('/api/auth/');

      if (isSessionLost) {
        auth.logout();
        void router.navigate(['/login'], { queryParams: { returnUrl: router.url } });
      }
      return throwError(() => error);
    }),
  );
};
