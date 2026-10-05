import { HttpClient } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { Observable, map, tap } from 'rxjs';
import {
  AuthResponse,
  LoginRequest,
  RegisterRequest,
  RegisterResponse,
  Role,
  StoredSession,
  UserSummary,
} from './auth.models';
import { TokenStorageService } from './token-storage.service';

/** Landing page of each role after login (wireframe « Connexion »). */
export const HOME_URL_BY_ROLE: Record<Role, string> = {
  STUDENT: '/my-courses',
  TRAINER: '/trainer/courses',
  ADMIN: '/admin/users',
};

/**
 * Holds the authentication state of the application.
 *
 * The frontend only uses the role for ergonomics (menus, redirections, guards):
 * every authorization is enforced by the backend (Dossier §11).
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly storage = inject(TokenStorageService);

  private readonly session = signal<StoredSession | null>(this.storage.read());

  /** Logged-in user, or null. */
  readonly currentUser = computed(() => this.session()?.user ?? null);
  readonly isAuthenticated = computed(() => this.session() !== null);
  readonly role = computed(() => this.session()?.user.role ?? null);

  login(request: LoginRequest): Observable<UserSummary> {
    return this.http.post<AuthResponse>('/api/auth/login', request).pipe(
      tap((response) => {
        const session: StoredSession = {
          accessToken: response.accessToken,
          expiresAt: Date.now() + response.expiresInSeconds * 1000,
          user: response.user,
        };
        this.storage.save(session);
        this.session.set(session);
      }),
      map((response) => response.user),
    );
  }

  /** Creates a STUDENT account. Does not log the user in. */
  register(request: RegisterRequest): Observable<RegisterResponse> {
    return this.http.post<RegisterResponse>('/api/auth/register', request);
  }

  logout(): void {
    this.storage.clear();
    this.session.set(null);
  }

  /**
   * Token to send to the API, or null.
   * Re-reads the storage so that an expired token is never sent.
   */
  getAccessToken(): string | null {
    const stored = this.storage.read();
    if (!stored && this.session()) {
      this.session.set(null);
    }
    return stored?.accessToken ?? null;
  }

  hasAnyRole(roles: readonly Role[]): boolean {
    const role = this.role();
    return role !== null && roles.includes(role);
  }

  /** Landing page for the current user, or the login page when logged out. */
  homeUrl(): string {
    const role = this.role();
    return role ? HOME_URL_BY_ROLE[role] : '/login';
  }
}
