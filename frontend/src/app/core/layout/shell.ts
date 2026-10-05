import { Component, computed, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { Role } from '../auth/auth.models';
import { AuthService } from '../auth/auth.service';

export interface NavItem {
  label: string;
  path: string;
}

/** Main navigation per role (wireframes « Student », « Trainer », « Admin »). */
export const NAV_BY_ROLE: Record<Role, NavItem[]> = {
  STUDENT: [
    { label: 'Catalogue', path: '/catalog' },
    { label: 'Mes cours', path: '/my-courses' },
  ],
  TRAINER: [
    { label: 'Mes formations', path: '/trainer/courses' },
    { label: 'Corrections', path: '/trainer/submissions' },
  ],
  ADMIN: [
    { label: 'Utilisateurs', path: '/admin/users' },
    { label: 'Formations', path: '/admin/courses' },
  ],
};

const ROLE_LABEL: Record<Role, string> = {
  STUDENT: 'Student',
  TRAINER: 'Trainer',
  ADMIN: 'Admin',
};

/** Application frame for logged-in users: header (logo, role navigation, user, logout) + page. */
@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatButtonModule],
  templateUrl: './shell.html',
  styleUrl: './shell.scss',
})
export class Shell {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly user = this.auth.currentUser;
  protected readonly navItems = computed(() => {
    const role = this.auth.role();
    return role ? NAV_BY_ROLE[role] : [];
  });
  protected readonly roleLabel = computed(() => {
    const role = this.auth.role();
    return role ? ROLE_LABEL[role] : '';
  });
  protected readonly initials = computed(() => {
    const user = this.user();
    return user ? `${user.firstName.charAt(0)}${user.lastName.charAt(0)}`.toUpperCase() : '';
  });

  protected logout(): void {
    this.auth.logout();
    void this.router.navigateByUrl('/login');
  }
}
