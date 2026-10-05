import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  CanActivateFn,
  provideRouter,
  RouterStateSnapshot,
  UrlTree,
} from '@angular/router';
import { Role } from './auth.models';
import { AuthService } from './auth.service';
import { authGuard, guestGuard, roleGuard } from './auth.guards';

describe('auth guards', () => {
  let role: Role | null;

  const fakeAuth = {
    isAuthenticated: () => role !== null,
    hasAnyRole: (roles: readonly Role[]) => role !== null && roles.includes(role),
    homeUrl: () =>
      role === 'STUDENT' ? '/my-courses' : role === 'TRAINER' ? '/trainer/courses' : role === 'ADMIN' ? '/admin/users' : '/login',
  };

  const run = (guard: CanActivateFn, url = '/admin/users') =>
    TestBed.runInInjectionContext(() =>
      guard({} as ActivatedRouteSnapshot, { url } as RouterStateSnapshot),
    );

  const asUrl = (result: unknown) => (result as UrlTree).toString();

  beforeEach(() => {
    role = null;
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: AuthService, useValue: fakeAuth }],
    });
  });

  describe('authGuard', () => {
    it('redirects a visitor to /login with the requested URL', () => {
      expect(asUrl(run(authGuard, '/my-courses'))).toBe('/login?returnUrl=%2Fmy-courses');
    });

    it('lets a logged-in user through', () => {
      role = 'STUDENT';
      expect(run(authGuard)).toBe(true);
    });
  });

  describe('roleGuard', () => {
    it('lets an ADMIN into an admin route', () => {
      role = 'ADMIN';
      expect(run(roleGuard('ADMIN'))).toBe(true);
    });

    it('sends a STUDENT trying an admin route back to their home page', () => {
      role = 'STUDENT';
      expect(asUrl(run(roleGuard('ADMIN')))).toBe('/my-courses');
    });

    it('sends a visitor to /login', () => {
      expect(asUrl(run(roleGuard('TRAINER'), '/trainer/courses'))).toBe(
        '/login?returnUrl=%2Ftrainer%2Fcourses',
      );
    });
  });

  describe('guestGuard', () => {
    it('lets a visitor see the login page', () => {
      expect(run(guestGuard, '/login')).toBe(true);
    });

    it('sends a logged-in TRAINER to their home page', () => {
      role = 'TRAINER';
      expect(asUrl(run(guestGuard, '/login'))).toBe('/trainer/courses');
    });
  });
});
