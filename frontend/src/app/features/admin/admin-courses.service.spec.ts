import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AdminCoursesService } from './admin-courses.service';

describe('AdminCoursesService', () => {
  let service: AdminCoursesService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(AdminCoursesService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('creates a course and manages its trainers', () => {
    const body = { title: 'Allemand A2', description: null, category: 'LANGUAGES' as const };
    service.createCourse(body).subscribe();
    service.listTrainers(10).subscribe();
    service.assignTrainer(10, 2).subscribe();
    service.unassignTrainer(10, 2).subscribe();

    const create = http.expectOne('/api/courses');
    expect(create.request.method).toBe('POST');
    expect(create.request.body).toEqual(body);
    create.flush({});
    http.expectOne('/api/courses/10/trainers').flush([]);
    const reqs = http.match('/api/courses/10/trainers/2');
    expect(reqs.map((r) => r.request.method)).toEqual(['POST', 'DELETE']);
    reqs.forEach((r) => r.flush(null));
  });
});
