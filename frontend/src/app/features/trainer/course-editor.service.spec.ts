import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { CourseEditorService } from './course-editor.service';

describe('CourseEditorService', () => {
  let service: CourseEditorService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(CourseEditorService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  function expectCall(url: string, method: string, body: unknown = null) {
    const req = http.expectOne(url);
    expect(req.request.method).toBe(method);
    expect(req.request.body).toEqual(body);
    req.flush({});
  }

  it('updates, publishes and unpublishes a course', () => {
    const body = { title: 'Anglais', description: null, category: 'LANGUAGES' as const };
    service.updateCourse(10, body).subscribe();
    expectCall('/api/courses/10', 'PUT', body);
    service.publishCourse(10).subscribe();
    expectCall('/api/courses/10/publish', 'PATCH');
    service.unpublishCourse(10).subscribe();
    expectCall('/api/courses/10/unpublish', 'PATCH');
  });

  it('creates, updates and publishes modules and lessons', () => {
    service.createModule(10, { title: 'M1', description: null }).subscribe();
    expectCall('/api/courses/10/modules', 'POST', { title: 'M1', description: null });
    service.updateModule(3, { title: 'M1 bis', description: 'd' }).subscribe();
    expectCall('/api/modules/3', 'PUT', { title: 'M1 bis', description: 'd' });
    service.publishModule(3).subscribe();
    expectCall('/api/modules/3/publish', 'PATCH');

    const lesson = { title: 'L1', description: null, content: null };
    service.createLesson(3, lesson).subscribe();
    expectCall('/api/modules/3/lessons', 'POST', lesson);
    service.updateLesson(8, lesson).subscribe();
    expectCall('/api/lessons/8', 'PUT', lesson);
    service.publishLesson(8).subscribe();
    expectCall('/api/lessons/8/publish', 'PATCH');
  });
});
