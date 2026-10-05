import { Injectable } from '@angular/core';
import { StoredSession } from './auth.models';

/**
 * Persists the session (JWT + user) in sessionStorage.
 *
 * CHOIX TECHNIQUE (validé) : sessionStorage plutôt que localStorage — la session
 * disparaît à la fermeture de l'onglet, ce qui limite l'exposition du token.
 * Le token n'est jamais rafraîchi (pas de refresh token, Dossier §12.2) :
 * une session expirée est simplement supprimée.
 */
@Injectable({ providedIn: 'root' })
export class TokenStorageService {
  static readonly STORAGE_KEY = 'hg.session';

  save(session: StoredSession): void {
    sessionStorage.setItem(TokenStorageService.STORAGE_KEY, JSON.stringify(session));
  }

  /** Returns the stored session, or null if absent, unreadable or expired (in which case it is removed). */
  read(now: number = Date.now()): StoredSession | null {
    const raw = sessionStorage.getItem(TokenStorageService.STORAGE_KEY);
    if (!raw) {
      return null;
    }
    try {
      const session = JSON.parse(raw) as StoredSession;
      if (!session.accessToken || typeof session.expiresAt !== 'number' || session.expiresAt <= now) {
        this.clear();
        return null;
      }
      return session;
    } catch {
      this.clear();
      return null;
    }
  }

  clear(): void {
    sessionStorage.removeItem(TokenStorageService.STORAGE_KEY);
  }
}
