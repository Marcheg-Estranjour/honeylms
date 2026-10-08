import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { AssignmentDetail, SubmissionDetail } from '../learning/learning.models';
import { CorrectionRequest, EnrolledStudent, ManagedAssignmentSummary, ManagedCourseSummary } from './teaching.models';

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

  /** Assignment detail (instructions, deadline, attached files); 403 outside the trainer's courses. */
  getAssignment(assignmentId: number): Observable<AssignmentDetail> {
    return this.http.get<AssignmentDetail>(`/api/assignments/${assignmentId}`);
  }

  /** US-SUB-05 — submissions of an assignment, most recent first. */
  listSubmissions(assignmentId: number): Observable<SubmissionDetail[]> {
    return this.http.get<SubmissionDetail[]>(`/api/assignments/${assignmentId}/submissions`);
  }

  /** US-SUB-06/07 — grade (optional, 0–20) and feedback; correcting again overwrites. */
  correct(submissionId: number, body: CorrectionRequest): Observable<SubmissionDetail> {
    return this.http.patch<SubmissionDetail>(`/api/submissions/${submissionId}/correction`, body);
  }
}
