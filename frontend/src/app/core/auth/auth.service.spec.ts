import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthResponse } from './auth.models';
import { AuthService } from './auth.service';
import { TokenStorageService } from './token-storage.service';

describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;

  const loginResponse: AuthResponse = {
    accessToken: 'jwt-token',
    tokenType: 'Bearer',
    expiresInSeconds: 86_400,
    user: { id: 7, email: 'trainer@example.com', firstName: 'Paul', lastName: 'Durand', role: 'TRAINER' },
  };

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('starts logged out when no session is stored', () => {
    expect(service.isAuthenticated()).toBe(false);
    expect(service.currentUser()).toBeNull();
    expect(service.homeUrl()).toBe('/login');
  });

  it('logs in, stores the session and exposes the user', () => {
    let returnedRole: string | undefined;
    service.login({ email: 'trainer@example.com', password: 'secret123' }).subscribe((user) => {
      returnedRole = user.role;
    });

    const req = http.expectOne('/api/auth/login');
    expect(req.request.method).toBe('POST');
    req.flush(loginResponse);

    expect(returnedRole).toBe('TRAINER');
    expect(service.isAuthenticated()).toBe(true);
    expect(service.getAccessToken()).toBe('jwt-token');
    expect(service.homeUrl()).toBe('/trainer/courses');
    expect(TestBed.inject(TokenStorageService).read()?.user.id).toBe(7);
  });

  it('does not store anything when login fails', () => {
    service.login({ email: 'x@example.com', password: 'wrong-pass' }).subscribe({ error: () => {} });
    http
      .expectOne('/api/auth/login')
      .flush({ status: 401, message: 'Invalid credentials' }, { status: 401, statusText: 'Unauthorized' });

    expect(service.isAuthenticated()).toBe(false);
    expect(service.getAccessToken()).toBeNull();
  });

  it('registers without logging in', () => {
    service
      .register({ email: 'new@example.com', password: 'secret123', firstName: 'Ana', lastName: 'Lopez' })
      .subscribe();
    http.expectOne('/api/auth/register').flush({ id: 9, email: 'new@example.com', firstName: 'Ana', lastName: 'Lopez', role: 'STUDENT' });

    expect(service.isAuthenticated()).toBe(false);
  });

  it('logs out and clears the storage', () => {
    service.login({ email: 'trainer@example.com', password: 'secret123' }).subscribe();
    http.expectOne('/api/auth/login').flush(loginResponse);

    service.logout();

    expect(service.isAuthenticated()).toBe(false);
    expect(TestBed.inject(TokenStorageService).read()).toBeNull();
  });

  it('checks roles', () => {
    service.login({ email: 'trainer@example.com', password: 'secret123' }).subscribe();
    http.expectOne('/api/auth/login').flush(loginResponse);

    expect(service.hasAnyRole(['TRAINER', 'ADMIN'])).toBe(true);
    expect(service.hasAnyRole(['STUDENT'])).toBe(false);
  });
});
