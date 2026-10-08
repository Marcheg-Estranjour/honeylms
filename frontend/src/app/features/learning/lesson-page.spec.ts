import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of, throwError } from 'rxjs';
import { CourseOutlineService } from './course-outline.service';
import { LearningService } from './learning.service';
import { L11, L12, L21, L22, lessonOf, OUTLINE } from './lesson-test-data';
import { LessonPage } from './lesson-page';

describe('LessonPage', () => {
  const load = vi.fn();
  const learning = {
    getLesson: vi.fn(),
    getCompletions: vi.fn(),
    getCourseProgress: vi.fn(),
    recordView: vi.fn(),
    markCompleted: vi.fn(),
  };
  let harness: RouterTestingHarness;

  const lessons = new Map([L11, L12, L21, L22].map((l) => [l.id, l]));

  async function open(lessonId: number) {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'courses/:courseId/lessons/:lessonId', component: LessonPage }]),
        { provide: CourseOutlineService, useValue: { load } },
        { provide: LearningService, useValue: learning },
      ],
    });
    harness = await RouterTestingHarness.create();
    await harness.navigateByUrl(`/courses/1/lessons/${lessonId}`, LessonPage);
  }

  const root = () => harness.routeNativeElement!;
  const text = () => root().textContent ?? '';
  const sidebarLinks = () => Array.from(root().querySelectorAll<HTMLAnchorElement>('.lesson-link'));

  beforeEach(() => {
    load.mockReset().mockReturnValue(of(OUTLINE));
    learning.getLesson.mockReset().mockImplementation((id: number) => of(lessons.get(id)));
    learning.getCompletions.mockReset().mockReturnValue(of({ courseId: 1, completedLessonIds: [101] }));
    learning.getCourseProgress
      .mockReset()
      .mockReturnValue(of({ courseId: 1, completedLessons: 1, accessibleLessons: 4, percentage: 25 }));
    learning.recordView.mockReset().mockReturnValue(of({ lessonId: 0, lastViewedAt: null, completedAt: null }));
    learning.markCompleted.mockReset();
  });

  it('shows the outline with module counters, progress and the current lesson', async () => {
    await open(102);
    expect(text()).toContain('Module 1 · Se présenter');
    expect(text()).toContain('1/2');
    expect(text()).toContain('0/2');
    expect(text()).toContain('25 %');
    const current = root().querySelector('.lesson-link.current')!;
    expect(current.textContent).toContain('Présenter son entreprise');
    expect(current.getAttribute('aria-current')).toBe('page');
    expect(sidebarLinks()[0].textContent).toContain('(terminée)');
  });

  it('shows the lesson with its breadcrumb and records the visit', async () => {
    await open(101);
    expect(root().querySelector('h1')!.textContent).toContain('Se présenter');
    expect(text()).toContain('Module 1');
    expect(text()).toContain('Leçon 1');
    expect(text()).toContain('Hello, my name is Camille.');
    expect(learning.recordView).toHaveBeenCalledWith(101);
  });

  it('displays the content as plain text, never as HTML (XSS)', async () => {
    lessons.set(102, lessonOf(102, 10, 2, 'Piège', '<img src=x onerror="alert(1)"><b>gras</b>'));
    await open(102);
    const content = root().querySelector('.content .text')!;
    expect(content.querySelector('img')).toBeNull();
    expect(content.querySelector('b')).toBeNull();
    expect(content.textContent).toContain('<b>gras</b>');
    lessons.set(102, L12);
  });

  it('offers previous and next lessons across modules', async () => {
    await open(102);
    const links = Array.from(root().querySelectorAll<HTMLAnchorElement>('.actions a'));
    expect(links[0].getAttribute('href')).toBe('/courses/1/lessons/101');
    expect(links[1].getAttribute('href')).toBe('/courses/1/lessons/201');
  });

  it('offers « Retour à mes cours » after the last lesson', async () => {
    await open(202);
    const links = Array.from(root().querySelectorAll<HTMLAnchorElement>('.actions a'));
    expect(links.at(-1)!.getAttribute('href')).toBe('/my-courses');
  });

  it('shows « Leçon terminée » for a completed lesson', async () => {
    await open(101);
    expect(text()).toContain('Leçon terminée');
    expect(root().querySelector('.actions button')).toBeNull();
  });

  it('marks the lesson as completed and refreshes the progress', async () => {
    await open(102);
    learning.markCompleted.mockReturnValue(of({ lessonId: 102, lastViewedAt: 'x', completedAt: 'x' }));
    learning.getCourseProgress.mockReturnValue(
      of({ courseId: 1, completedLessons: 2, accessibleLessons: 4, percentage: 50 }),
    );

    root().querySelector<HTMLButtonElement>('.actions button')!.click();
    harness.detectChanges();

    expect(learning.markCompleted).toHaveBeenCalledWith(102);
    expect(text()).toContain('Leçon terminée');
    expect(text()).toContain('50 %');
    expect(text()).toContain('2/2');
  });

  it('keeps the button and explains when the completion fails', async () => {
    await open(102);
    learning.markCompleted.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
    root().querySelector<HTMLButtonElement>('.actions button')!.click();
    harness.detectChanges();

    expect(text()).toContain("n'a pas pu être marquée comme terminée");
    expect(root().querySelector('.actions button')).not.toBeNull();
  });

  it('still shows the lesson when the visit cannot be recorded', async () => {
    learning.recordView.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
    await open(101);
    expect(root().querySelector('h1')!.textContent).toContain('Se présenter');
  });

  it('explains a 403 on the lesson', async () => {
    learning.getLesson.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 403 })));
    await open(101);
    expect(text()).toContain("Cette leçon n'est pas accessible");
    expect(learning.recordView).not.toHaveBeenCalled();
  });
});
