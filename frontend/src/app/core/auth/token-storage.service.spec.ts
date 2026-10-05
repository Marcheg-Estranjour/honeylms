import { TestBed } from '@angular/core/testing';
import { StoredSession } from './auth.models';
import { TokenStorageService } from './token-storage.service';

describe('TokenStorageService', () => {
  let storage: TokenStorageService;

  const session = (expiresAt: number): StoredSession => ({
    accessToken: 'jwt-token',
    expiresAt,
    user: { id: 1, email: 'camille@example.com', firstName: 'Camille', lastName: 'Martin', role: 'STUDENT' },
  });

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({});
    storage = TestBed.inject(TokenStorageService);
  });

  it('returns null when nothing is stored', () => {
    expect(storage.read()).toBeNull();
  });

  it('saves and reads back a valid session', () => {
    storage.save(session(Date.now() + 60_000));
    expect(storage.read()?.accessToken).toBe('jwt-token');
  });

  it('removes and ignores an expired session', () => {
    storage.save(session(Date.now() - 1));
    expect(storage.read()).toBeNull();
    expect(sessionStorage.getItem(TokenStorageService.STORAGE_KEY)).toBeNull();
  });

  it('removes and ignores a corrupted value', () => {
    sessionStorage.setItem(TokenStorageService.STORAGE_KEY, '{not json');
    expect(storage.read()).toBeNull();
    expect(sessionStorage.getItem(TokenStorageService.STORAGE_KEY)).toBeNull();
  });

  it('clears the session', () => {
    storage.save(session(Date.now() + 60_000));
    storage.clear();
    expect(storage.read()).toBeNull();
  });
});
