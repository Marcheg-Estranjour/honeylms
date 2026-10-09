import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Role } from '../../core/auth/auth.models';
import { AuthService } from '../../core/auth/auth.service';
import { AdminUser, AdminUsersService } from './admin-users.service';

type RoleFilter = Role | 'ALL';

/** Same rule as the backend (CreateTrainerRequest: @Size(min = 8)) and the register form. */
const PASSWORD_MIN_LENGTH = 8;

export const ROLE_LABELS: Record<Role, string> = {
  STUDENT: 'Étudiant',
  TRAINER: 'Formateur',
  ADMIN: 'Admin',
};

/**
 * « Utilisateurs » (Admin) — US-ADMIN-01 (list), US-AUTH-03 (activate / deactivate),
 * US-ADMIN-06 (create a Trainer account). Route: /admin/users.
 *
 * - Filters (role, text search on name and email) are applied on the client: the list is small
 *   (one training organisation) and comes in a single call.
 * - The Admin cannot deactivate his own account: the button is disabled here and the backend
 *   refuses it too (403).
 * - HYPOTHÈSE (validée côté backend) : the Admin sets the initial password of a Trainer and
 *   gives it to him; there is no invitation email in the MVP.
 */
@Component({
  selector: 'app-admin-users-page',
  imports: [DatePipe, ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatButtonModule],
  templateUrl: './admin-users-page.html',
  styleUrl: './admin-users-page.scss',
})
export class AdminUsersPage {
  private readonly service = inject(AdminUsersService);
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly auth = inject(AuthService);

  protected readonly roleLabels = ROLE_LABELS;
  protected readonly filters: { value: RoleFilter; label: string }[] = [
    { value: 'ALL', label: 'Tous' },
    { value: 'STUDENT', label: 'Étudiants' },
    { value: 'TRAINER', label: 'Formateurs' },
    { value: 'ADMIN', label: 'Admins' },
  ];
  protected readonly minLength = PASSWORD_MIN_LENGTH;

  protected readonly state = signal<'loading' | 'ready' | 'error'>('loading');
  protected readonly users = signal<AdminUser[]>([]);
  protected readonly roleFilter = signal<RoleFilter>('ALL');
  protected readonly search = signal('');
  protected readonly busyId = signal<number | null>(null);
  protected readonly notice = signal<string | null>(null);
  protected readonly error = signal<string | null>(null);

  protected readonly creating = signal(false);
  protected readonly saving = signal(false);
  protected readonly createError = signal<string | null>(null);
  protected readonly trainerForm = this.fb.group({
    firstName: ['', Validators.required],
    lastName: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(PASSWORD_MIN_LENGTH)]],
  });

  protected readonly currentUserId = computed(() => this.auth.currentUser()?.id ?? null);

  protected readonly visible = computed(() => {
    const role = this.roleFilter();
    const query = normalize(this.search());
    return this.users().filter(
      (u) =>
        (role === 'ALL' || u.role === role) &&
        (query === '' || normalize(`${u.firstName} ${u.lastName} ${u.email}`).includes(query)),
    );
  });

  protected readonly counts = computed(() => {
    const counts: Record<RoleFilter, number> = { ALL: 0, STUDENT: 0, TRAINER: 0, ADMIN: 0 };
    for (const u of this.users()) {
      counts.ALL++;
      counts[u.role]++;
    }
    return counts;
  });

  constructor() {
    this.load();
  }

  protected load(): void {
    this.state.set('loading');
    this.service.listUsers().subscribe({
      next: (users) => {
        this.users.set(users);
        this.state.set('ready');
      },
      error: () => this.state.set('error'),
    });
  }

  protected onSearch(event: Event): void {
    this.search.set((event.target as HTMLInputElement).value);
  }

  protected toggleActive(user: AdminUser): void {
    if (this.busyId() !== null) return;
    const name = `${user.firstName} ${user.lastName}`;
    this.busyId.set(user.id);
    this.notice.set(null);
    this.error.set(null);
    this.service.setActive(user.id, !user.active).subscribe({
      next: (saved) => {
        this.busyId.set(null);
        this.replace(saved);
        this.notice.set(saved.active ? `Le compte de ${name} est réactivé.` : `Le compte de ${name} est désactivé : il ne peut plus se connecter.`);
      },
      error: (error: unknown) => {
        this.busyId.set(null);
        this.error.set(
          error instanceof HttpErrorResponse && error.status === 403
            ? 'Vous ne pouvez pas désactiver votre propre compte.'
            : `Le statut de ${name} n'a pas pu être modifié. Réessayez.`,
        );
      },
    });
  }

  // ---- Trainer creation ----

  protected openCreate(): void {
    this.trainerForm.reset();
    this.createError.set(null);
    this.notice.set(null);
    this.creating.set(true);
  }

  protected cancelCreate(): void {
    this.creating.set(false);
  }

  protected createTrainer(): void {
    if (this.trainerForm.invalid) {
      this.trainerForm.markAllAsTouched();
      return;
    }
    const { firstName, lastName, email, password } = this.trainerForm.getRawValue();
    this.saving.set(true);
    this.createError.set(null);
    this.service
      .createTrainer({ firstName: firstName.trim(), lastName: lastName.trim(), email: email.trim(), password })
      .subscribe({
        next: (user) => {
          this.saving.set(false);
          this.creating.set(false);
          this.users.update((list) =>
            [...list, user].sort((a, b) => `${a.lastName} ${a.firstName}`.localeCompare(`${b.lastName} ${b.firstName}`, 'fr')),
          );
          this.notice.set(
            `Compte formateur créé pour ${user.firstName} ${user.lastName}. Communiquez-lui son mot de passe initial.`,
          );
        },
        error: (error: unknown) => {
          this.saving.set(false);
          this.createError.set(createErrorMessage(error));
        },
      });
  }

  private replace(user: AdminUser): void {
    this.users.update((list) => list.map((u) => (u.id === user.id ? user : u)));
  }
}

/** Lower case without accents, for the search. */
function normalize(text: string): string {
  return text.normalize('NFD').replace(/\p{Diacritic}/gu, '').toLowerCase().trim();
}

function createErrorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 409) return 'Un compte existe déjà avec cette adresse email.';
    if (error.status === 400) return 'Vérifiez les champs : email valide et mot de passe de 8 caractères minimum.';
  }
  return "Le compte n'a pas pu être créé. Réessayez.";
}
