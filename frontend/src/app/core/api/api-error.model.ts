import { HttpErrorResponse } from '@angular/common/http';

/**
 * Standard error envelope returned by the backend for every API error
 * (Dossier de Conception §12.3, backend record `common.ApiError`).
 */
export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
}

/** Fallback message when the backend did not send an ApiError body (network down, proxy error...). */
export const GENERIC_ERROR_MESSAGE = 'Une erreur inattendue est survenue. Veuillez réessayer.';

/** Type guard: true when the value has the shape of the backend ApiError envelope. */
export function isApiError(value: unknown): value is ApiError {
  return (
    typeof value === 'object' &&
    value !== null &&
    typeof (value as ApiError).status === 'number' &&
    typeof (value as ApiError).message === 'string'
  );
}

/** Extracts a displayable message from an HTTP error, falling back to a generic one. */
export function toErrorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse && isApiError(error.error)) {
    return error.error.message;
  }
  return GENERIC_ERROR_MESSAGE;
}
