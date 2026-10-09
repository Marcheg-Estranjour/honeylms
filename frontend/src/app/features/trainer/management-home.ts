import { inject } from '@angular/core';
import { AuthService } from '../../core/auth/auth.service';

/** Where the course / lesson editors lead back to: the editors are shared by Trainer and Admin. */
export interface ManagementHome {
  url: string;
  label: string;
}

/** Must be called in an injection context (field initializer of a component). */
export function managementHome(): ManagementHome {
  return inject(AuthService).role() === 'ADMIN'
    ? { url: '/admin/courses', label: 'Formations' }
    : { url: '/trainer/courses', label: 'Mes formations' };
}
