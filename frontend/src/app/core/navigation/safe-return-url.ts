/**
 * Validates a `returnUrl` query parameter before navigating to it after login.
 *
 * Security (open redirect): only internal paths are accepted. Anything that could
 * leave the application — `https://evil.example`, `//evil.example`, `/\evil.example`,
 * `javascript:...` — is rejected and the caller falls back to the user's home page.
 */
export function safeReturnUrl(value: string | null | undefined): string | null {
  if (!value || !value.startsWith('/')) {
    return null;
  }
  if (value.startsWith('//') || value.startsWith('/\\')) {
    return null;
  }
  // Coming back to the auth pages after logging in makes no sense.
  if (value.startsWith('/login') || value.startsWith('/register')) {
    return null;
  }
  return value;
}
