import { HttpErrorResponse } from '@angular/common/http';
import { GENERIC_ERROR_MESSAGE } from '../../core/api/api-error.model';

/*
 * CHOIX TECHNIQUE : the backend messages are written in English (code language rule)
 * and are meant for developers and logs. The interface is in French, so the auth pages
 * map the HTTP status to a French message instead of displaying `ApiError.message`.
 */

const SERVER_UNREACHABLE = 'Le serveur est injoignable. Vérifiez votre connexion puis réessayez.';

export function loginErrorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    switch (error.status) {
      case 0:
        return SERVER_UNREACHABLE;
      case 401:
        // Same message whatever the cause: never reveal whether the email exists.
        return 'Email ou mot de passe incorrect.';
      case 403:
        return 'Ce compte est désactivé. Contactez un administrateur.';
    }
  }
  return GENERIC_ERROR_MESSAGE;
}

export function registerErrorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    switch (error.status) {
      case 0:
        return SERVER_UNREACHABLE;
      case 400:
        return 'Certaines informations sont invalides. Vérifiez le formulaire.';
      case 409:
        return 'Un compte existe déjà avec cette adresse email.';
    }
  }
  return GENERIC_ERROR_MESSAGE;
}
