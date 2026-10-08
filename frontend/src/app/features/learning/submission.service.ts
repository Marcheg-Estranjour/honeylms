import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, Observable, of, throwError } from 'rxjs';
import { AssignmentDetail, SubmissionDetail } from './learning.models';

/**
 * Student side of assignments and submissions (US-ASSIGN-04, US-SUB-01/02/03).
 * Rules (enrollment, publication, deadline, one submission per student) are enforced by the backend.
 */
@Injectable({ providedIn: 'root' })
export class SubmissionService {
  private readonly http = inject(HttpClient);

  /** 403 when the assignment is not accessible (not enrolled, not published). */
  getAssignment(assignmentId: number): Observable<AssignmentDetail> {
    return this.http.get<AssignmentDetail>(`/api/assignments/${assignmentId}`);
  }

  /** The student's own submission, or null when nothing has been submitted yet (404, gap G3). */
  getMySubmission(assignmentId: number): Observable<SubmissionDetail | null> {
    return this.http.get<SubmissionDetail>(`/api/assignments/${assignmentId}/submissions/me`).pipe(
      catchError((error: unknown) =>
        error instanceof HttpErrorResponse && error.status === 404 ? of(null) : throwError(() => error),
      ),
    );
  }

  /** US-SUB-01 — first submission (multipart part « file »). 409 if one already exists. */
  submit(assignmentId: number, file: File): Observable<SubmissionDetail> {
    return this.http.post<SubmissionDetail>(`/api/assignments/${assignmentId}/submissions`, toFormData(file));
  }

  /** US-SUB-02 — replaces the file; the backend resets grade, feedback and status to SUBMITTED. */
  replace(submissionId: number, file: File): Observable<SubmissionDetail> {
    return this.http.put<SubmissionDetail>(`/api/submissions/${submissionId}`, toFormData(file));
  }
}

/** The browser sets the multipart Content-Type (with its boundary) itself: never set it by hand. */
function toFormData(file: File): FormData {
  const body = new FormData();
  body.append('file', file, file.name);
  return body;
}
