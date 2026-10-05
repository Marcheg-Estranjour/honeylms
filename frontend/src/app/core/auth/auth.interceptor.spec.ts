import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpTesting: HttpTestingController;
  let token: string | null;

  beforeEach(() => {
    token = 'jwt-token';
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { getAccessToken: () => token } },
      ],
    });
    http = TestBed.inject(HttpClient);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('adds the Bearer token to /api requests', () => {
    http.get('/api/me/courses').subscribe();
    const req = httpTesting.expectOne('/api/me/courses');
    expect(req.request.headers.get('Authorization')).toBe('Bearer jwt-token');
    req.flush([]);
  });

  it('does not add a token to the public auth endpoints', () => {
    http.post('/api/auth/login', {}).subscribe();
    const req = httpTesting.expectOne('/api/auth/login');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });

  it('never sends the token to another origin', () => {
    http.get('https://example.org/api/data').subscribe();
    const req = httpTesting.expectOne('https://example.org/api/data');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });

  it('sends the request without header when logged out', () => {
    token = null;
    http.get('/api/courses').subscribe();
    const req = httpTesting.expectOne('/api/courses');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush([]);
  });
});
