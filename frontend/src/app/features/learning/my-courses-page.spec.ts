import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { EnrolledCourse } from '../../shared/courses/course.models';
import { EnrollmentService } from './enrollment.service';
import { CourseProgress, ResumePoint } from './learning.models';
import { LearningService } from './learning.service';
import { MyCoursesPage } from './my-courses-page';

describe('MyCoursesPage', () => {
  const getMyCourses = vi.fn();
  const getCourseProgress = vi.fn();
  const getResumePoint = vi.fn();
  const getModule = vi.fn();
  const getLesson = vi.fn();

  const enrolled = (courseId: number, title: string, category: EnrolledCourse['category']): EnrolledCourse => ({
    courseId,
    title,
    description: null,
    category,
    enrolledAt: '2026-09-20T08:00:00Z',
  });
  const progress = (courseId: number, completed: number, accessible: number): CourseProgress => ({
    courseId,
    completedLessons: completed,
    accessibleLessons: accessible,
    percentage: accessible ? Math.floor((completed * 100) / accessible) : 0,
  });

  const english = enrolled(1, 'Anglais professionnel — B1', 'LANGUAGES');
  const excel = enrolled(4, 'Excel — Les essentiels', 'OFFICE_AUTOMATION');
  const welcome = enrolled(6, 'Accueil et conseil voyageur', 'EDUCTOUR');

  const progressById: Record<number, CourseProgress> = {
    1: progress(1, 4, 6),
    4: progress(4, 0, 4),
    6: progress(6, 2, 2),
  };
  const resumeById: Record<number, ResumePoint | null> = {
    1: { lessonId: 5, moduleId: 2, lastViewedAt: '2026-10-06T08:00:00Z' },
    4: null,
    6: { lessonId: 20, moduleId: 9, lastViewedAt: '2026-09-25T08:00:00Z' },
  };

  function render() {
    TestBed.configureTestingModule({
      imports: [MyCoursesPage],
      providers: [
        provideRouter([]),
        { provide: EnrollmentService, useValue: { getMyCourses } },
        { provide: LearningService, useValue: { getCourseProgress, getResumePoint, getModule, getLesson } },
        { provide: AuthService, useValue: { currentUser: signal({ firstName: 'Camille' }) } },
      ],
    });
    const fixture = TestBed.createComponent(MyCoursesPage);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  beforeEach(() => {
    getMyCourses.mockReset().mockReturnValue(of([english, excel, welcome]));
    getCourseProgress.mockReset().mockImplementation((id: number) => of(progressById[id]));
    getResumePoint.mockReset().mockImplementation((id: number) => of(resumeById[id]));
    getModule.mockReset().mockReturnValue(of({ id: 2, displayOrder: 2, title: 'Écrire des emails' }));
    getLesson.mockReset().mockReturnValue(of({ id: 5, title: 'Le ton et les formules de politesse' }));
  });

  it('greets the student', () => {
    expect(render().textContent).toContain('BONJOUR CAMILLE');
  });

  it('shows one card per course with its progress', () => {
    const root = render();
    const cards = Array.from(root.querySelectorAll('.card'));
    expect(cards.length).toBe(3);
    expect(cards[0].textContent).toContain('4 leçons sur 6 terminées');
    expect(cards[0].textContent).toContain('66 %');
    expect(cards[0].textContent).toContain('Continuer');
    expect(cards[1].textContent).toContain('4 leçons à découvrir');
    expect(cards[1].textContent).toContain('Commencer');
    expect(cards[2].textContent).toContain('Toutes les leçons terminées');
    expect(cards[2].textContent).toContain('Revoir le cours');
  });

  it('links every card to the course entry point', () => {
    const link = render().querySelector<HTMLAnchorElement>('.card .action')!;
    expect(link.getAttribute('href')).toBe('/courses/1/learn');
  });

  it('builds the resume banner from the most recently viewed course', () => {
    const banner = render().querySelector('.resume')!;
    expect(banner.textContent).toContain('Anglais professionnel — B1');
    expect(banner.textContent).toContain('Module 2 · Le ton et les formules de politesse');
    expect(getLesson).toHaveBeenCalledWith(5);
    expect(banner.querySelector('a')!.getAttribute('href')).toBe('/courses/1/learn');
  });

  it('shows no banner when no lesson was ever opened', () => {
    getResumePoint.mockReturnValue(of(null));
    expect(render().querySelector('.resume')).toBeNull();
  });

  it('keeps the banner without location if the lesson cannot be read', () => {
    getLesson.mockReturnValue(throwError(() => new Error('403')));
    const banner = render().querySelector('.resume')!;
    expect(banner.textContent).toContain('Anglais professionnel — B1');
    expect(banner.querySelector('.resume-location')).toBeNull();
  });

  it('still shows a card when its progress cannot be loaded', () => {
    getCourseProgress.mockImplementation((id: number) =>
      id === 4 ? throwError(() => new Error('boom')) : of(progressById[id]),
    );
    const cards = render().querySelectorAll('.card');
    expect(cards.length).toBe(3);
    expect(cards[1].textContent).toContain('Progression indisponible');
  });

  it('invites to the catalogue when the student has no course', () => {
    getMyCourses.mockReturnValue(of([]));
    const root = render();
    expect(root.querySelectorAll('.card').length).toBe(0);
    expect(root.textContent).toContain("Vous n'êtes inscrit à aucun cours.");
    expect(root.querySelector<HTMLAnchorElement>('.more a')!.getAttribute('href')).toBe('/catalog');
  });

  it('shows an error with retry when the enrollments cannot be loaded', () => {
    getMyCourses.mockReturnValue(throwError(() => new Error('boom')));
    expect(render().textContent).toContain("Vos cours n'ont pas pu être chargés.");
  });
});
