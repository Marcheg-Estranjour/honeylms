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
});
