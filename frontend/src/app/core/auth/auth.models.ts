/**
 * TypeScript mirrors of the backend auth DTOs (package `user.dto`).
 * Written by hand from the Java records — keep them in sync when a record changes.
 */

/** Role codes as returned by the backend (`role.code`). One role per user (Dossier §12.1). */
export type Role = 'STUDENT' | 'TRAINER' | 'ADMIN';

/** Backend record `UserSummary`. */
export interface UserSummary {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  role: Role;
  /** Gap G9 — present in every UserSummary since S10 (optional here: older sessions lack it). */
  active?: boolean;
  createdAt?: string | null;
}

/** Payload of POST /api/auth/login. */
export interface LoginRequest {
  email: string;
  password: string;
}

/** Response of POST /api/auth/login. */
export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
  user: UserSummary;
}

/** Payload of POST /api/auth/register. The role is always STUDENT, set by the backend. */
export interface RegisterRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
}

/** Response of POST /api/auth/register. */
export interface RegisterResponse {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  role: Role;
}

/** What the frontend keeps in sessionStorage after a successful login. */
export interface StoredSession {
  accessToken: string;
  /** Epoch milliseconds after which the token must be considered expired. */
  expiresAt: number;
  user: UserSummary;
}
