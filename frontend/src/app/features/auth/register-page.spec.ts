import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { FormControl, FormGroup } from '@angular/forms';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of, throwError } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { passwordsMatch, RegisterPage } from './register-page';

describe('passwordsMatch', () => {
  const group = (password: string, confirmPassword: string) =>
    new FormGroup({ password: new FormControl(password), confirmPassword: new FormControl(confirmPassword) });

  it('accepts identical passwords', () => {
    expect(passwordsMatch(group('secret123', 'secret123'))).toBeNull();
  });

  it('flags different passwords', () => {
    expect(passwordsMatch(group('secret123', 'secret124'))).toEqual({ passwordsMismatch: true });
  });
});

describe('RegisterPage', () => {
  const register = vi.fn();
  const login = vi.fn();
  let harness: RouterTestingHarness;
  let router: Router;

  const fill = (selector: string, value: string) => {
    const input = harness.routeNativeElement!.querySelector<HTMLInputElement>(selector)!;
    input.value = value;
    input.dispatchEvent(new Event('input'));
    input.dispatchEvent(new Event('blur'));
  };
  const fillValidForm = () => {
    fill('#firstName', ' Ana ');
    fill('#lastName', 'Lopez');
    fill('#email', 'ana@example.com');
    fill('#password', 'secret123');
    fill('#confirmPassword', 'secret123');
  };
  const submit = () => {
    harness.routeNativeElement!.querySelector<HTMLFormElement>('form')!.dispatchEvent(new Event('submit'));
    harness.detectChanges();
  };
  const text = () => harness.routeNativeElement!.textContent ?? '';

  beforeEach(async () => {
    register.mockReset();
    login.mockReset();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'register', component: RegisterPage }]),
        { provide: AuthService, useValue: { register, login, homeUrl: () => '/my-courses' } },
      ],
    });
    harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/register', RegisterPage);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  it('refuses mismatching passwords without calling the API', () => {
    fillValidForm();
    fill('#confirmPassword', 'other-pass');
    submit();

    expect(register).not.toHaveBeenCalled();
    expect(text()).toContain('Les deux mots de passe ne correspondent pas.');
  });

  it('refuses a password shorter than 8 characters', () => {
    fillValidForm();
    fill('#password', 'short');
    fill('#confirmPassword', 'short');
    submit();

    expect(register).not.toHaveBeenCalled();
  });

  it('registers, logs in automatically and goes to the student home page', () => {
    register.mockReturnValue(of({ id: 9, email: 'ana@example.com', role: 'STUDENT' }));
    login.mockReturnValue(of({ role: 'STUDENT' }));
    fillValidForm();
    submit();

    expect(register).toHaveBeenCalledWith({
      firstName: 'Ana',
      lastName: 'Lopez',
      email: 'ana@example.com',
      password: 'secret123',
    });
    expect(login).toHaveBeenCalledWith({ email: 'ana@example.com', password: 'secret123' });
    expect(router.navigateByUrl).toHaveBeenCalledWith('/my-courses');
  });

  it('shows a French message when the email is already used', () => {
    register.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 409 })));
    fillValidForm();
    submit();

    expect(text()).toContain('Un compte existe déjà avec cette adresse email.');
    expect(login).not.toHaveBeenCalled();
  });

  it('sends the user to the login page if the automatic login fails', () => {
    register.mockReturnValue(of({ id: 9, email: 'ana@example.com', role: 'STUDENT' }));
    login.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 0 })));
    fillValidForm();
    submit();

    expect(router.navigate).toHaveBeenCalledWith(['/login'], { queryParams: { registered: '1' } });
  });
});
