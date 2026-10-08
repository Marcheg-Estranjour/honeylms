import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { EnrolledStudent, ManagedAssignmentSummary, ManagedCourseSummary } from './teaching.models';

/**
 * Trainer workspace (also usable by an Admin): the backend restricts the lists to the
 * courses the requester manages (CourseTrainer) — the client never filters for security.
 */
@Injectable({ providedIn: 'root' })
export class TeachingService {
  private readonly http = inject(HttpClient);

  /** G5 — « Mes formations », sorted by title. */
  getManagedCourses(): Observable<ManagedCourseSummary[]> {
    return this.http.get<ManagedCourseSummary[]>('/api/me/managed-courses');
  }

  /** G11 — assignments with their counters, nearest deadline first. */
  getManagedAssignments(): Observable<ManagedAssignmentSummary[]> {
    return this.http.get<ManagedAssignmentSummary[]>('/api/me/managed-assignments');
  }

  /** G10 — students enrolled in a course, sorted by name. */
  getStudents(courseId: number): Observable<EnrolledStudent[]> {
    return this.http.get<EnrolledStudent[]>(`/api/courses/${courseId}/students`);
  }
}
