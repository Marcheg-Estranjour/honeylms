import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of, throwError } from 'rxjs';
import { managedAssignment, WORD } from './teaching-test-data';
import { TeachingService } from './teaching.service';
import { TrainerSubmissionsPage } from './trainer-submissions-page';

describe('TrainerSubmissionsPage', () => {
  const getManagedAssignments = vi.fn();
  let harness: RouterTestingHarness;

  const ASSIGNMENTS = [
    managedAssignment(1, { dueDate: '2020-01-01T10:00:00Z', submissions: 2, submissionsToCorrect: 2 }),
    managedAssignment(2, { submissions: 3, submissionsToCorrect: 0 }),
    managedAssignment(3, {
      courseId: WORD.id,
      courseTitle: WORD.title,
      dueDate: null,
      status: 'DRAFT',
      enrolledStudents: 0,
    }),
  ];

  async function open(url = '/trainer/submissions') {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'trainer/submissions', component: TrainerSubmissionsPage }], withComponentInputBinding()),
        { provide: TeachingService, useValue: { getManagedAssignments } },
      ],
    });
    harness = await RouterTestingHarness.create();
    await harness.navigateByUrl(url, TrainerSubmissionsPage);
  }

  const root = () => harness.routeNativeElement!;
  const text = () => root().textContent ?? '';
  const rows = () => Array.from(root().querySelectorAll('.row'));
  const chips = () => Array.from(root().querySelectorAll<HTMLAnchorElement>('.chips a'));

  beforeEach(() => getManagedAssignments.mockReset().mockReturnValue(of(ASSIGNMENTS)));

  it('lists the assignments with course, lesson, deadline and counters', async () => {
    await open();
    expect(rows()).toHaveLength(3);
    const first = rows()[0].textContent!;
    expect(first).toContain('Devoir 1');
    expect(first).toContain('Anglais professionnel — B1 · Leçon 1');
    expect(first).toContain('(dépassée)');
    expect(first).toContain('2/3');
    expect(first).toContain('2 à corriger');
    expect(rows()[1].textContent).toContain('Tout est corrigé');
    expect(rows()[2].textContent).toContain('Brouillon');
    expect(rows()[2].textContent).toContain('Pas de date limite');
    expect(rows()[0].querySelector('a')!.getAttribute('href')).toBe('/trainer/assignments/1');
  });

  it('shows the total to correct', async () => {
    await open();
    expect(root().querySelector('.summary')!.textContent).toContain('2 dépôts à corriger');
  });

  it('filters on the course given in the URL', async () => {
    await open('/trainer/submissions?course=12');
    expect(rows()).toHaveLength(1);
    expect(rows()[0].textContent).toContain('Devoir 3');
    expect(text()).toContain('Aucun dépôt à corriger.');
    const active = chips().find((c) => c.classList.contains('active'))!;
    expect(active.textContent).toContain('Word — les bases');
  });

  it('offers one filter per course, sorted by title, plus « Toutes »', async () => {
    await open();
    expect(chips().map((c) => c.textContent!.trim())).toEqual([
      'Toutes',
      'Anglais professionnel — B1',
      'Word — les bases',
    ]);
    expect(chips()[2].getAttribute('href')).toBe('/trainer/submissions?course=12');
  });

  it('can show only the assignments to correct', async () => {
    await open();
    const box = root().querySelector<HTMLInputElement>('.toggle input')!;
    box.checked = true;
    box.dispatchEvent(new Event('change'));
    harness.detectChanges();
    expect(rows()).toHaveLength(1);
    expect(rows()[0].textContent).toContain('Devoir 1');
  });

  it('explains when the filter matches nothing', async () => {
    await open('/trainer/submissions?course=12');
    const box = root().querySelector<HTMLInputElement>('.toggle input')!;
    box.checked = true;
    box.dispatchEvent(new Event('change'));
    harness.detectChanges();
    expect(text()).toContain('Aucun devoir ne correspond à ce filtre.');
  });

  it('explains when there is no assignment at all', async () => {
    getManagedAssignments.mockReturnValue(of([]));
    await open();
    expect(text()).toContain('Aucun devoir dans vos formations');
  });

  it('offers to retry after an error', async () => {
    getManagedAssignments.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
    await open();
    expect(text()).toContain("n'ont pas pu être chargés");
  });
});
