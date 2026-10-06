import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { EnrolledCourse } from '../../shared/courses/course.models';

/** Student enrollments (US-ENROLL-01 / US-ENROLL-02). */
@Injectable({ providedIn: 'root' })
export class EnrollmentService {
  private readonly http = inject(HttpClient);

  /** Courses the logged-in student is enrolled in. */
  getMyCourses(): Observable<EnrolledCourse[]> {
    return this.http.get<EnrolledCourse[]>('/api/me/courses');
  }
}
