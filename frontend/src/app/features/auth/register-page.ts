import { Component, inject, signal } from '@angular/core';
import {
  AbstractControl,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Router, RouterLink } from '@angular/router';
import { switchMap, tap } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { registerErrorMessage } from './auth-error-messages';
import { AuthLayout } from './auth-layout';

/** Same bounds as the backend RegisterRequest (@Size(min = 8, max = 100)). */
export const PASSWORD_MIN_LENGTH = 8;
export const PASSWORD_MAX_LENGTH = 100;

/** Group validator: `confirmPassword` must equal `password`. */
export const passwordsMatch: ValidatorFn = (group: AbstractControl): ValidationErrors | null => {
  const password = group.get('password')?.value;
  const confirm = group.get('confirmPassword')?.value;
  return password && confirm && password !== confirm ? { passwordsMismatch: true } : null;
};

/**
 * US-AUTH-01 — Student self-registration.
 * The role is never chosen here: the backend always creates a STUDENT account.
 * On success the new student is logged in automatically (same credentials).
 * Front validation is for comfort only, the backend validation is the reference.
 */
@Component({
  selector: 'app-register-page',
  imports: [ReactiveFormsModule, RouterLink, MatFormFieldModule, MatInputModule, MatButtonModule, AuthLayout],
  templateUrl: './register-page.html',
  styleUrl: './auth-form.scss',
})
export class RegisterPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly minLength = PASSWORD_MIN_LENGTH;

  protected readonly form = inject(NonNullableFormBuilder).group(
    {
      firstName: ['', [Validators.required, Validators.maxLength(100)]],
      lastName: ['', [Validators.required, Validators.maxLength(100)]],
      email: ['', [Validators.required, Validators.email, Validators.maxLength(255)]],
      password: [
        '',
        [
          Validators.required,
          Validators.minLength(PASSWORD_MIN_LENGTH),
          Validators.maxLength(PASSWORD_MAX_LENGTH),
        ],
      ],
      confirmPassword: ['', Validators.required],
    },
    { validators: passwordsMatch },
  );

  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  private registered = false;

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.errorMessage.set(null);
    this.registered = false;

    const { firstName, lastName, email, password } = this.form.getRawValue();
    const request = {
      firstName: firstName.trim(),
      lastName: lastName.trim(),
      email: email.trim(),
      password,
    };
    this.auth
      .register(request)
      .pipe(
        tap(() => (this.registered = true)),
        switchMap(() => this.auth.login({ email: request.email, password })),
      )
      .subscribe({
        // Account created and logged in: straight to the student home page.
        next: () => void this.router.navigateByUrl(this.auth.homeUrl()),
        error: (error: unknown) => {
          this.submitting.set(false);
          if (this.registered) {
            // Created but the automatic login failed: let the user log in manually.
            void this.router.navigate(['/login'], { queryParams: { registered: '1' } });
            return;
          }
          this.errorMessage.set(registerErrorMessage(error));
        },
      });
  }
}
