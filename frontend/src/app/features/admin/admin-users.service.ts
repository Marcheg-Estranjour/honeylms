import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { UserSummary } from '../../core/auth/auth.models';

/** A user as listed by the Admin: active and createdAt are always sent by GET /api/users (G9). */
export interface AdminUser extends UserSummary {
  active: boolean;
  createdAt: string | null;
}

/** Backend record `CreateTrainerRequest` — the role is forced to TRAINER by the server. */
export interface CreateTrainerRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
}

/** Admin back-office: user accounts (US-ADMIN-01, US-ADMIN-06, US-AUTH-03). ADMIN only. */
@Injectable({ providedIn: 'root' })
export class AdminUsersService {
  private readonly http = inject(HttpClient);

  /** All accounts, sorted by last name then first name. */
  listUsers(): Observable<AdminUser[]> {
    return this.http.get<AdminUser[]>('/api/users');
  }

  /** 409 when the email already exists. */
  createTrainer(body: CreateTrainerRequest): Observable<AdminUser> {
    return this.http.post<AdminUser>('/api/users/trainers', body);
  }

  /** Activates / deactivates an account. 403 when an Admin tries to deactivate himself. */
  setActive(userId: number, active: boolean): Observable<AdminUser> {
    return this.http.patch<AdminUser>(`/api/users/${userId}/status`, { active });
  }
}
