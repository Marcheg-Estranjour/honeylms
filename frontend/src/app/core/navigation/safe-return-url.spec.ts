import { safeReturnUrl } from './safe-return-url';

describe('safeReturnUrl', () => {
  it('accepts internal paths', () => {
    expect(safeReturnUrl('/catalog')).toBe('/catalog');
    expect(safeReturnUrl('/lessons/12?tab=resources')).toBe('/lessons/12?tab=resources');
  });

  it('rejects empty values', () => {
    expect(safeReturnUrl(null)).toBeNull();
    expect(safeReturnUrl(undefined)).toBeNull();
    expect(safeReturnUrl('')).toBeNull();
  });

  it('rejects external or protocol-relative URLs (open redirect)', () => {
    expect(safeReturnUrl('https://evil.example')).toBeNull();
    expect(safeReturnUrl('//evil.example')).toBeNull();
    expect(safeReturnUrl('/\\evil.example')).toBeNull();
    expect(safeReturnUrl('javascript:alert(1)')).toBeNull();
  });

  it('rejects the auth pages themselves', () => {
    expect(safeReturnUrl('/login')).toBeNull();
    expect(safeReturnUrl('/register')).toBeNull();
  });
});
