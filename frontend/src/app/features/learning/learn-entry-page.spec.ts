import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of, throwError } from 'rxjs';
import { CourseOutlineService } from './course-outline.service';
import { LearnEntryPage } from './learn-entry-page';
import { LearningService } from './learning.service';
import { OUTLINE } from './lesson-test-data';

describe('LearnEntryPage', () => {
  const load = vi.fn();
  const getResumePoint = vi.fn();
  let router: Router;
  let harness: RouterTestingHarness;

  async function open() {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'courses/:courseId/learn', component: LearnEntryPage }]),
        { provide: CourseOutlineService, useValue: { load } },
        { provide: LearningService, useValue: { getResumePoint } },
      ],
    });
    router = TestBed.inject(Router);
    // The harness navigates with navigateByUrl: spying on navigate only catches the page's redirect.
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/courses/1/learn', LearnEntryPage);
  }

  beforeEach(() => {
    load.mockReset().mockReturnValue(of(OUTLINE));
    getResumePoint.mockReset();
  });

  it('goes to the last lesson viewed (resume point)', async () => {
    getResumePoint.mockReturnValue(of({ lessonId: 202, moduleId: 20, lastViewedAt: '2026-10-06T08:00:00Z' }));
    await open();
    expect(router.navigate).toHaveBeenCalledWith(['/courses', 1, 'lessons', 202], { replaceUrl: true });
  });

  it('goes to the first lesson when nothing was viewed yet', async () => {
    getResumePoint.mockReturnValue(of(null));
    await open();
    expect(router.navigate).toHaveBeenCalledWith(['/courses', 1, 'lessons', 101], { replaceUrl: true });
  });

  it('ignores a resume point that is no longer accessible (unpublished lesson)', async () => {
    getResumePoint.mockReturnValue(of({ lessonId: 999, moduleId: 30, lastViewedAt: '2026-10-06T08:00:00Z' }));
    await open();
    expect(router.navigate).toHaveBeenCalledWith(['/courses', 1, 'lessons', 101], { replaceUrl: true });
  });

  it('explains when the course has no published lesson', async () => {
    load.mockReturnValue(of({ ...OUTLINE, modules: [], lessons: [] }));
    getResumePoint.mockReturnValue(of(null));
    await open();
    expect(router.navigate).not.toHaveBeenCalled();
    expect(harness.routeNativeElement!.textContent).toContain("Aucune leçon n'est encore publiée");
  });

  it('asks to enroll when the backend answers 403', async () => {
    load.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 403 })));
    getResumePoint.mockReturnValue(of(null));
    await open();
    expect(harness.routeNativeElement!.textContent).toContain('Inscrivez-vous à ce cours');
  });
});
