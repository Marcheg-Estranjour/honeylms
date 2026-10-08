import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { LearningService } from './learning.service';

describe('LearningService', () => {
  let service: LearningService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(LearningService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('calls the module, lesson and progress endpoints', () => {
    service.getModule(3).subscribe();
    service.getLesson(8).subscribe();
    service.getCourseProgress(1).subscribe();
    http.expectOne('/api/modules/3').flush({});
    http.expectOne('/api/lessons/8').flush({});
    http.expectOne('/api/courses/1/progress').flush({});
  });

  it('calls the outline, completion and visit endpoints', () => {
    service.listModules(1).subscribe();
    service.listLessons(3).subscribe();
    service.getCompletions(1).subscribe();
    service.recordView(8).subscribe();
    service.markCompleted(8).subscribe();
    http.expectOne('/api/courses/1/modules').flush([]);
    http.expectOne('/api/modules/3/lessons').flush([]);
    http.expectOne('/api/courses/1/completions').flush({ courseId: 1, completedLessonIds: [] });
    const view = http.expectOne('/api/lessons/8/view');
    expect(view.request.method).toBe('PUT');
    view.flush({});
    const completion = http.expectOne('/api/lessons/8/completion');
    expect(completion.request.method).toBe('POST');
    completion.flush({});
  });

  it('returns the resume point', () => {
    let result: unknown;
    service.getResumePoint(1).subscribe((r) => (result = r));
    http.expectOne('/api/courses/1/resume').flush({ lessonId: 5, moduleId: 2, lastViewedAt: '2026-10-06T08:00:00Z' });
    expect(result).toEqual({ lessonId: 5, moduleId: 2, lastViewedAt: '2026-10-06T08:00:00Z' });
  });

  it('maps « no resume point yet » (404) to null', () => {
    let result: unknown = 'untouched';
    service.getResumePoint(1).subscribe((r) => (result = r));
    http.expectOne('/api/courses/1/resume').flush(null, { status: 404, statusText: 'Not Found' });
    expect(result).toBeNull();
  });

  it('still fails on other errors', () => {
    let status: number | undefined;
    service.getResumePoint(1).subscribe({ error: (e) => (status = e.status) });
    http.expectOne('/api/courses/1/resume').flush(null, { status: 403, statusText: 'Forbidden' });
    expect(status).toBe(403);
  });
});
