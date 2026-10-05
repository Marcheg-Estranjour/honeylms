import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { AuthService } from '../auth/auth.service';
import { errorInterceptor } from './error.interceptor';

describe('errorInterceptor', () => {
  let http: HttpClient;
  let httpTesting: HttpTestingController;
  const logout = vi.fn();
  const navigate = vi.fn().mockResolvedValue(true);

  beforeEach(() => {
    logout.mockClear();
    navigate.mockClear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { logout } },
        { provide: Router, useValue: { navigate, url: '/my-courses' } },
      ],
    });
    http = TestBed.inject(HttpClient);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('logs out and redirects to /login on a 401 from a protected endpoint', () => {
    let status: number | undefined;
    http.get('/api/me/courses').subscribe({ error: (e) => (status = e.status) });
    httpTesting.expectOne('/api/me/courses').flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(logout).toHaveBeenCalledOnce();
    expect(navigate).toHaveBeenCalledWith(['/login'], { queryParams: { returnUrl: '/my-courses' } });
    expect(status).toBe(401);
  });

  it('leaves a 401 from /api/auth/login to the login page (bad credentials)', () => {
    http.post('/api/auth/login', {}).subscribe({ error: () => {} });
    httpTesting.expectOne('/api/auth/login').flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(logout).not.toHaveBeenCalled();
    expect(navigate).not.toHaveBeenCalled();
  });

  it('rethrows a 403 without logging out', () => {
    let status: number | undefined;
    http.put('/api/courses/1', {}).subscribe({ error: (e) => (status = e.status) });
    httpTesting.expectOne('/api/courses/1').flush(
      { status: 403, message: 'You are not allowed to modify this course.' },
      { status: 403, statusText: 'Forbidden' },
    );

    expect(status).toBe(403);
    expect(logout).not.toHaveBeenCalled();
  });
});
