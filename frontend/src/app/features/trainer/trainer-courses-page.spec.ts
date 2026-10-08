import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of, throwError } from 'rxjs';
import { ENGLISH, WORD } from './teaching-test-data';
import { TeachingService } from './teaching.service';
import { TrainerCoursesPage } from './trainer-courses-page';

describe('TrainerCoursesPage', () => {
  const getManagedCourses = vi.fn();
  let harness: RouterTestingHarness;

  async function open() {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'trainer/courses', component: TrainerCoursesPage }]),
        { provide: TeachingService, useValue: { getManagedCourses } },
      ],
    });
    harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/trainer/courses', TrainerCoursesPage);
  }

  const root = () => harness.routeNativeElement!;
  const text = () => root().textContent ?? '';
  const cards = () => Array.from(root().querySelectorAll('.card'));

  beforeEach(() => getManagedCourses.mockReset().mockReturnValue(of([ENGLISH, WORD])));

  it('lists the managed courses with domain, status and enrolled students', async () => {
    await open();
    expect(cards()).toHaveLength(2);
    expect(cards()[0].textContent).toContain('Anglais professionnel — B1');
    expect(cards()[0].textContent).toContain('Langues');
    expect(cards()[0].textContent).toContain('Publiée');
    expect(cards()[0].textContent).toContain('3 étudiants inscrits');
    expect(cards()[1].textContent).toContain('Brouillon');
    expect(cards()[1].textContent).toContain('0 étudiant inscrit');
    expect(cards()[0].classList).toContain('cat-languages');
  });

  it('links the submissions to correct to « Corrections » filtered on the course', async () => {
    await open();
    const link = cards()[0].querySelector<HTMLAnchorElement>('.to-correct')!;
    expect(link.textContent).toContain('2 dépôts à corriger');
    expect(link.getAttribute('href')).toBe('/trainer/submissions?course=10');
    expect(cards()[1].textContent).toContain('Aucun dépôt à corriger');
  });

  it('shows the total to correct in the header', async () => {
    await open();
    expect(root().querySelector('.head a')!.textContent).toContain('2 dépôts à corriger');
  });

  it('links each course to its management page', async () => {
    await open();
    expect(cards()[1].querySelector('a.action')!.getAttribute('href')).toBe('/trainer/courses/12');
  });

  it('explains when no course is assigned', async () => {
    getManagedCourses.mockReturnValue(of([]));
    await open();
    expect(text()).toContain('Aucune formation ne vous est attribuée');
    expect(root().querySelector('.head a')).toBeNull();
  });

  it('offers to retry after an error', async () => {
    getManagedCourses.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
    await open();
    expect(text()).toContain("n'ont pas pu être chargées");

    getManagedCourses.mockReturnValue(of([ENGLISH]));
    root().querySelector<HTMLButtonElement>('button')!.click();
    harness.detectChanges();
    expect(cards()).toHaveLength(1);
  });
});
