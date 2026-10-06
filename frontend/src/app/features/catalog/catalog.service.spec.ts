import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { EnrollmentService } from '../learning/enrollment.service';
import { CatalogService } from './catalog.service';

describe('CatalogService & EnrollmentService', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lists all published courses', () => {
    TestBed.inject(CatalogService).getCourses().subscribe();
    const req = http.expectOne('/api/courses');
    expect(req.request.params.has('category')).toBe(false);
    req.flush([]);
  });

  it('filters by category when asked', () => {
    TestBed.inject(CatalogService).getCourses('EDUCTOUR').subscribe();
    http.expectOne('/api/courses?category=EDUCTOUR').flush([]);
  });

  it('reads one course', () => {
    TestBed.inject(CatalogService).getCourse(12).subscribe();
    http.expectOne('/api/courses/12').flush({});
  });

  it("reads the student's enrollments", () => {
    TestBed.inject(EnrollmentService).getMyCourses().subscribe();
    http.expectOne('/api/me/courses').flush([]);
  });
});
