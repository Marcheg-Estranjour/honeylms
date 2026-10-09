import { HttpErrorResponse } from '@angular/common/http';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of, throwError } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { AdminUsersPage } from './admin-users-page';
import { AdminUser, AdminUsersService } from './admin-users.service';

describe('AdminUsersPage', () => {
  const service = { listUsers: vi.fn(), createTrainer: vi.fn(), setActive: vi.fn() };
  let harness: RouterTestingHarness;

  const user = (id: number, firstName: string, lastName: string, role: AdminUser['role'], active = true): AdminUser => ({
    id,
    email: `${firstName.toLowerCase()}@honeylms.test`,
    firstName,
    lastName,
    role,
    active,
    createdAt: '2026-09-01T08:00:00Z',
  });

  const ALICE = user(1, 'Alice', 'Bernard', 'ADMIN');
  const PAUL = user(2, 'Paul', 'Durand', 'TRAINER');
  const CAMILLE = user(4, 'Camille', 'Martin', 'STUDENT');
  const LUCAS = user(5, 'Lucas', 'Petit', 'STUDENT', false);

  async function open() {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'admin/users', component: AdminUsersPage }]),
        { provide: AdminUsersService, useValue: service },
        { provide: AuthService, useValue: { currentUser: signal(ALICE) } },
      ],
    });
    harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/admin/users', AdminUsersPage);
  }

  const root = () => harness.routeNativeElement!;
  const text = () => (root().textContent ?? '').replace(/\s+/g, ' ');
  const rows = () => Array.from(root().querySelectorAll('tbody tr'));
  const row = (name: string) => rows().find((r) => r.textContent!.includes(name))!;
  const button = (label: string, scope: ParentNode = root()) =>
    Array.from(scope.querySelectorAll<HTMLButtonElement>('button')).find((b) => b.textContent!.includes(label));

  function click(label: string, scope?: ParentNode) {
    button(label, scope)!.click();
    harness.detectChanges();
  }

  function type(name: string, value: string) {
    const input = root().querySelector<HTMLInputElement>(`input[formcontrolname=${name}]`)!;
    input.value = value;
    input.dispatchEvent(new Event('input'));
  }

  function submitCreate() {
    root().querySelector<HTMLFormElement>('form.create')!.dispatchEvent(new Event('submit'));
    harness.detectChanges();
  }

  beforeEach(() => {
    service.listUsers.mockReset().mockReturnValue(of([ALICE, PAUL, CAMILLE, LUCAS]));
    service.createTrainer.mockReset();
    service.setActive.mockReset();
  });

  it('lists the users with role, status and creation date', async () => {
    await open();
    expect(rows()).toHaveLength(4);
    expect(row('Paul Durand').textContent).toContain('Formateur');
    expect(row('Paul Durand').textContent).toContain('Actif');
    expect(row('Lucas Petit').textContent).toContain('Désactivé');
    expect(row('Lucas Petit').classList).toContain('inactive');
    expect(row('Camille Martin').textContent).toContain('camille@honeylms.test');
    expect(row('Camille Martin').textContent).toContain('2026');
  });

  it('filters by role with counters', async () => {
    await open();
    expect(button('Étudiants (2)')).toBeDefined();
    click('Étudiants');
    expect(rows()).toHaveLength(2);
    expect(text()).not.toContain('Paul Durand');
  });

  it('searches by name or email, ignoring accents and case', async () => {
    await open();
    const search = root().querySelector<HTMLInputElement>('input[type=search]')!;
    search.value = 'DURÂND';
    search.dispatchEvent(new Event('input'));
    harness.detectChanges();
    expect(rows()).toHaveLength(1);
    expect(text()).toContain('Paul Durand');

    search.value = 'personne';
    search.dispatchEvent(new Event('input'));
    harness.detectChanges();
    expect(text()).toContain('Aucun utilisateur ne correspond');
  });

  it('deactivates then reactivates an account', async () => {
    await open();
    service.setActive.mockReturnValue(of({ ...CAMILLE, active: false }));
    click('Désactiver', row('Camille Martin'));
    expect(service.setActive).toHaveBeenCalledWith(4, false);
    expect(row('Camille Martin').textContent).toContain('Désactivé');
    expect(text()).toContain('il ne peut plus se connecter');

    service.setActive.mockReturnValue(of({ ...CAMILLE, active: true }));
    click('Réactiver', row('Camille Martin'));
    expect(service.setActive).toHaveBeenLastCalledWith(4, true);
    expect(text()).toContain('est réactivé');
  });

  it('does not let the admin deactivate himself', async () => {
    await open();
    expect(row('Alice Bernard').textContent).toContain('(vous)');
    expect(button('Désactiver', row('Alice Bernard'))!.disabled).toBe(true);
  });

  it('explains a failed status change', async () => {
    await open();
    service.setActive.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
    click('Désactiver', row('Paul Durand'));
    expect(text()).toContain("Le statut de Paul Durand n'a pas pu être modifié.");
  });

  it('creates a trainer account', async () => {
    await open();
    click('Créer un compte formateur');
    type('firstName', ' Inès ');
    type('lastName', 'Bernard');
    type('email', 'ines@honeylms.test');
    type('password', 'Honey2026!');
    service.createTrainer.mockReturnValue(of(user(9, 'Inès', 'Bernard', 'TRAINER')));
    submitCreate();

    expect(service.createTrainer).toHaveBeenCalledWith({
      firstName: 'Inès',
      lastName: 'Bernard',
      email: 'ines@honeylms.test',
      password: 'Honey2026!',
    });
    expect(text()).toContain('Compte formateur créé pour Inès Bernard');
    expect(rows()).toHaveLength(5);
    expect(root().querySelector('form.create')).toBeNull();
  });

  it('validates the trainer form before calling the API', async () => {
    await open();
    click('Créer un compte formateur');
    type('email', 'pas-un-email');
    type('password', 'court');
    submitCreate();
    expect(service.createTrainer).not.toHaveBeenCalled();
    expect(text()).toContain("Cette adresse email n'est pas valide.");
    expect(text()).toContain('8 caractères minimum.');
  });

  it('explains an email already used (409)', async () => {
    await open();
    click('Créer un compte formateur');
    type('firstName', 'Paul');
    type('lastName', 'Durand');
    type('email', 'paul@honeylms.test');
    type('password', 'Honey2026!');
    service.createTrainer.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 409 })));
    submitCreate();
    expect(text()).toContain('Un compte existe déjà avec cette adresse email.');
  });

  it('offers to retry when the list cannot be loaded', async () => {
    service.listUsers.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
    await open();
    expect(text()).toContain("Les utilisateurs n'ont pas pu être chargés.");
  });
});
