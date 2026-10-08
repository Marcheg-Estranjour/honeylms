import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { TeachingService } from './teaching.service';

describe('TeachingService', () => {
  let service: TeachingService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(TeachingService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('calls the trainer workspace endpoints', () => {
    service.getManagedCourses().subscribe();
    service.getManagedAssignments().subscribe();
    service.getStudents(10).subscribe();
    http.expectOne('/api/me/managed-courses').flush([]);
    http.expectOne('/api/me/managed-assignments').flush([]);
    http.expectOne('/api/courses/10/students').flush([]);
  });

  it('lists the submissions and sends a correction', () => {
    service.getAssignment(30).subscribe();
    service.listSubmissions(30).subscribe();
    service.correct(12, { grade: 13.5, feedback: 'Bien' }).subscribe();
    http.expectOne('/api/assignments/30').flush({});
    http.expectOne('/api/assignments/30/submissions').flush([]);
    const req = http.expectOne('/api/submissions/12/correction');
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ grade: 13.5, feedback: 'Bien' });
    req.flush({});
  });
});
