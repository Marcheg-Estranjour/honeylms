import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of, throwError } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { LoginPage } from './login-page';

describe('LoginPage', () => {
  const login = vi.fn();
  let harness: RouterTestingHarness;
  let router: Router;

  const fill = (selector: string, value: string) => {
    const input = harness.routeNativeElement!.querySelector<HTMLInputElement>(selector)!;
    input.value = value;
    input.dispatchEvent(new Event('input'));
  };
  const submit = () => {
    harness.routeNativeElement!.querySelector<HTMLFormElement>('form')!.dispatchEvent(new Event('submit'));
    harness.detectChanges();
  };
  const text = () => harness.routeNativeElement!.textContent ?? '';

  async function open(url: string) {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'login', component: LoginPage }]),
        { provide: AuthService, useValue: { login, homeUrl: () => '/my-courses' } },
      ],
    });
    harness = await RouterTestingHarness.create();
    await harness.navigateByUrl(url, LoginPage);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
  }

  beforeEach(() => login.mockReset());

  it('does not call the API when the form is invalid', async () => {
    await open('/login');
    submit();
    expect(login).not.toHaveBeenCalled();
    expect(text()).toContain("L'adresse email est obligatoire.");
  });

  it('logs in with a trimmed email and goes to the role home page', async () => {
    await open('/login');
    login.mockReturnValue(of({ role: 'STUDENT' }));
    fill('#email', '  camille@example.com ');
    fill('#password', 'secret123');
    submit();

    expect(login).toHaveBeenCalledWith({ email: 'camille@example.com', password: 'secret123' });
    expect(router.navigateByUrl).toHaveBeenCalledWith('/my-courses');
  });

  it('goes back to a safe returnUrl after login', async () => {
    await open('/login?returnUrl=%2Fcatalog');
    login.mockReturnValue(of({ role: 'STUDENT' }));
    fill('#email', 'camille@example.com');
    fill('#password', 'secret123');
    submit();

    expect(router.navigateByUrl).toHaveBeenCalledWith('/catalog');
  });

  it('ignores an external returnUrl (open redirect)', async () => {
    await open('/login?returnUrl=https%3A%2F%2Fevil.example');
    login.mockReturnValue(of({ role: 'STUDENT' }));
    fill('#email', 'camille@example.com');
    fill('#password', 'secret123');
    submit();

    expect(router.navigateByUrl).toHaveBeenCalledWith('/my-courses');
  });

  it('shows a French message on bad credentials', async () => {
    await open('/login');
    login.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 401 })));
    fill('#email', 'camille@example.com');
    fill('#password', 'wrong-pass');
    submit();

    expect(text()).toContain('Email ou mot de passe incorrect.');
    expect(router.navigateByUrl).not.toHaveBeenCalled();
  });

  it('shows the confirmation banner after a registration', async () => {
    await open('/login?registered=1');
    expect(text()).toContain('Votre compte est créé');
  });
});
