import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { CourseSummary, EnrolledCourse } from '../../shared/courses/course.models';
import { EnrollmentService } from '../learning/enrollment.service';
import { CatalogPage } from './catalog-page';
import { CatalogService } from './catalog.service';

describe('CatalogPage', () => {
  const getCourses = vi.fn();
  const getMyCourses = vi.fn();

  const courses: CourseSummary[] = [
    { id: 1, title: 'Anglais professionnel — B1', description: 'Communiquer.', category: 'LANGUAGES' },
    { id: 2, title: 'Espagnol — Débutant', description: null, category: 'LANGUAGES' },
    { id: 3, title: 'Excel — Les essentiels', description: 'Formules.', category: 'OFFICE_AUTOMATION' },
  ];
  const enrolled: EnrolledCourse[] = [
    { courseId: 1, title: 'Anglais professionnel — B1', description: null, category: 'LANGUAGES', enrolledAt: '2026-10-01T08:00:00Z' },
  ];

  function render() {
    TestBed.configureTestingModule({
      imports: [CatalogPage],
      providers: [
        provideRouter([]),
        { provide: CatalogService, useValue: { getCourses } },
        { provide: EnrollmentService, useValue: { getMyCourses } },
      ],
    });
    const fixture = TestBed.createComponent(CatalogPage);
    fixture.detectChanges();
    return fixture;
  }

  const el = (fixture: ReturnType<typeof render>) => fixture.nativeElement as HTMLElement;
  const sectionTitles = (root: HTMLElement) =>
    Array.from(root.querySelectorAll('section h2')).map((h) => h.textContent?.trim());
  const clickChip = (fixture: ReturnType<typeof render>, label: string) => {
    const chip = Array.from(el(fixture).querySelectorAll<HTMLButtonElement>('.chip')).find(
      (b) => b.textContent?.trim() === label,
    )!;
    chip.click();
    fixture.detectChanges();
  };

  beforeEach(() => {
    getCourses.mockReset().mockReturnValue(of(courses));
    getMyCourses.mockReset().mockReturnValue(of(enrolled));
  });

  it('groups the courses by domain and hides empty domains', () => {
    const root = el(render());
    expect(sectionTitles(root)).toEqual(['Langues', 'Bureautique']);
    expect(root.querySelectorAll('.card').length).toBe(3);
    expect(root.textContent).toContain('2 cours');
  });

  it('links each card to the course detail page', () => {
    const link = el(render()).querySelector<HTMLAnchorElement>('.card')!;
    expect(link.getAttribute('href')).toBe('/courses/1');
  });

  it('shows the « Inscrit » badge only on enrolled courses', () => {
    const cards = Array.from(el(render()).querySelectorAll('.card'));
    expect(cards[0].textContent).toContain('Inscrit');
    expect(cards[1].textContent).toContain('Découvrir');
  });

  it('filters by domain without calling the API again', () => {
    const fixture = render();
    clickChip(fixture, 'Bureautique');
    expect(sectionTitles(el(fixture))).toEqual(['Bureautique']);

    clickChip(fixture, 'EDUCTOUR');
    expect(el(fixture).textContent).toContain('Aucune formation dans ce domaine');

    clickChip(fixture, 'Tous');
    expect(sectionTitles(el(fixture))).toEqual(['Langues', 'Bureautique']);
    expect(getCourses).toHaveBeenCalledOnce();
  });

  it('still shows the catalogue when the enrollments cannot be loaded', () => {
    getMyCourses.mockReturnValue(throwError(() => new Error('boom')));
    const root = el(render());
    expect(root.querySelectorAll('.card').length).toBe(3);
    expect(root.textContent).not.toContain('Inscrit');
  });

  it('shows an error with a retry button when the catalogue cannot be loaded', () => {
    getCourses.mockReturnValue(throwError(() => new Error('boom')));
    const fixture = render();
    expect(el(fixture).textContent).toContain("Le catalogue n'a pas pu être chargé.");

    getCourses.mockReturnValue(of(courses));
    el(fixture).querySelector<HTMLButtonElement>('.message button')!.click();
    fixture.detectChanges();
    expect(el(fixture).querySelectorAll('.card').length).toBe(3);
  });

  it('shows an empty state when nothing is published', () => {
    getCourses.mockReturnValue(of([]));
    expect(el(render()).textContent).toContain("Aucune formation n'est publiée pour le moment.");
  });
});
