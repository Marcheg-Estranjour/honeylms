import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of, throwError } from 'rxjs';
import { ENGLISH, WORD } from '../trainer/teaching-test-data';
import { TeachingService } from '../trainer/teaching.service';
import { AdminCoursesPage } from './admin-courses-page';
import { AdminCoursesService } from './admin-courses.service';
import { AdminUser, AdminUsersService } from './admin-users.service';

describe('AdminCoursesPage', () => {
  const getManagedCourses = vi.fn();
  const listUsers = vi.fn();
  const admin = { createCourse: vi.fn(), listTrainers: vi.fn(), assignTrainer: vi.fn(), unassignTrainer: vi.fn() };
  let harness: RouterTestingHarness;

  const user = (id: number, firstName: string, lastName: string, role: AdminUser['role'], active = true): AdminUser => ({
    id,
    email: `${firstName.toLowerCase()}@honeylms.test`,
    firstName,
    lastName,
    role,
    active,
    createdAt: null,
  });
  const PAUL = user(2, 'Paul', 'Durand', 'TRAINER');
  const SOFIA = user(3, 'Sofia', 'Garcia', 'TRAINER');
  const OLD = user(8, 'Marc', 'Ancien', 'TRAINER', false);
  const CAMILLE = user(4, 'Camille', 'Martin', 'STUDENT');

  async function open() {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'admin/courses', component: AdminCoursesPage }]),
        { provide: TeachingService, useValue: { getManagedCourses } },
        { provide: AdminUsersService, useValue: { listUsers } },
        { provide: AdminCoursesService, useValue: admin },
      ],
    });
    harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/admin/courses', AdminCoursesPage);
  }

  const root = () => harness.routeNativeElement!;
  const text = () => (root().textContent ?? '').replace(/\s+/g, ' ');
  const courses = () => Array.from(root().querySelectorAll('.course'));
  const course = (title: string) => courses().find((c) => c.textContent!.includes(title))!;
  const button = (label: string, scope: ParentNode = root()) =>
    Array.from(scope.querySelectorAll<HTMLButtonElement>('button')).find((b) => b.textContent!.includes(label));

  function click(label: string, scope?: ParentNode) {
    button(label, scope)!.click();
    harness.detectChanges();
  }

  function selectTrainer(id: number) {
    const select = root().querySelector<HTMLSelectElement>('.assign select')!;
    select.value = String(id);
    select.dispatchEvent(new Event('change'));
    harness.detectChanges();
  }

  beforeEach(() => {
    getManagedCourses.mockReset().mockReturnValue(of([ENGLISH, WORD]));
    listUsers.mockReset().mockReturnValue(of([PAUL, SOFIA, OLD, CAMILLE]));
    Object.values(admin).forEach((fn) => fn.mockReset());
    admin.listTrainers.mockReturnValue(of([PAUL]));
  });

  it('lists every course with domain, status and enrolled students', async () => {
    await open();
    expect(courses()).toHaveLength(2);
    expect(course('Anglais').textContent).toContain('Langues · 3 inscrits');
    expect(course('Anglais').textContent).toContain('Publiée');
    expect(course('Word').textContent).toContain('Brouillon');
    expect(course('Word').querySelector('a')!.getAttribute('href')).toBe('/trainer/courses/12');
  });

  it('shows the trainers of a course when its panel is opened', async () => {
    await open();
    click('Formateurs', course('Anglais'));
    expect(admin.listTrainers).toHaveBeenCalledWith(10);
    expect(course('Anglais').textContent).toContain('Paul Durand');
    // Only active trainers not yet assigned are offered.
    const options = Array.from(root().querySelectorAll('.assign option')).map((o) => o.textContent!.trim());
    expect(options).toEqual(['Choisir un formateur…', 'Sofia Garcia']);
  });

  it('assigns a trainer', async () => {
    await open();
    click('Formateurs', course('Anglais'));
    selectTrainer(3);
    admin.assignTrainer.mockReturnValue(of(undefined));
    click('Attribuer');
    expect(admin.assignTrainer).toHaveBeenCalledWith(10, 3);
    expect(course('Anglais').querySelector('.trainers')!.textContent).toContain('Sofia Garcia');
    expect(text()).toContain('Sofia Garcia gère maintenant « Anglais professionnel — B1 »');
    expect(text()).toContain('Tous les formateurs actifs sont attribués');
  });

  it('explains a refused assignment', async () => {
    await open();
    click('Formateurs', course('Anglais'));
    selectTrainer(3);
    admin.assignTrainer.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 409 })));
    click('Attribuer');
    expect(text()).toContain('déjà attribué');
  });

  it('removes a trainer', async () => {
    await open();
    click('Formateurs', course('Anglais'));
    admin.unassignTrainer.mockReturnValue(of(undefined));
    click('Retirer');
    expect(admin.unassignTrainer).toHaveBeenCalledWith(10, 2);
    expect(text()).toContain('Aucun formateur : personne ne peut gérer le contenu');
  });

  it('creates a course in draft and opens its trainers panel', async () => {
    await open();
    click('Créer une formation');
    const form = root().querySelector<HTMLFormElement>('form.create')!;
    const title = form.querySelector('input')!;
    title.value = ' Allemand A2 ';
    title.dispatchEvent(new Event('input'));
    admin.createCourse.mockReturnValue(
      of({ id: 20, title: 'Allemand A2', description: null, category: 'LANGUAGES', status: 'DRAFT', createdByUserId: 1 }),
    );
    admin.listTrainers.mockReturnValue(of([]));
    form.dispatchEvent(new Event('submit'));
    harness.detectChanges();

    expect(admin.createCourse).toHaveBeenCalledWith({ title: 'Allemand A2', category: 'LANGUAGES', description: null });
    expect(courses()).toHaveLength(3);
    expect(courses()[0].textContent).toContain('Allemand A2');
    expect(text()).toContain('est créée en brouillon');
    expect(admin.listTrainers).toHaveBeenCalledWith(20);
  });

  it('refuses a blank title', async () => {
    await open();
    click('Créer une formation');
    root().querySelector<HTMLFormElement>('form.create')!.dispatchEvent(new Event('submit'));
    harness.detectChanges();
    expect(admin.createCourse).not.toHaveBeenCalled();
    expect(text()).toContain('Le titre est obligatoire.');
  });

  it('keeps the list usable when the trainers cannot be loaded', async () => {
    listUsers.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
    await open();
    expect(text()).toContain("l'attribution est indisponible");
    click('Formateurs', course('Anglais'));
    expect(root().querySelector('.assign')).toBeNull();
  });

  it('offers to retry when the courses cannot be loaded', async () => {
    getManagedCourses.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
    await open();
    expect(text()).toContain("Les formations n'ont pas pu être chargées.");
  });
});
