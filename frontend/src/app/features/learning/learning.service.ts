import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, Observable, of, throwError } from 'rxjs';
import {
  CourseCompletions,
  CourseProgress,
  LessonCompletionDetail,
  LessonDetail,
  ModuleDetail,
  ResumePoint,
} from './learning.models';

/**
 * Student side of the learning API: modules, lessons, progress, resume point.
 * Access rules (enrollment + Course/Module/Lesson PUBLISHED) are enforced by the backend.
 */
@Injectable({ providedIn: 'root' })
export class LearningService {
  private readonly http = inject(HttpClient);

  /** Published modules of the course, in display order (the backend filters for students). */
  listModules(courseId: number): Observable<ModuleDetail[]> {
    return this.http.get<ModuleDetail[]>(`/api/courses/${courseId}/modules`);
  }

  /** Published lessons of the module, in display order (the backend filters for students). */
  listLessons(moduleId: number): Observable<LessonDetail[]> {
    return this.http.get<LessonDetail[]>(`/api/modules/${moduleId}/lessons`);
  }

  getModule(moduleId: number): Observable<ModuleDetail> {
    return this.http.get<ModuleDetail>(`/api/modules/${moduleId}`);
  }

  getLesson(lessonId: number): Observable<LessonDetail> {
    return this.http.get<LessonDetail>(`/api/lessons/${lessonId}`);
  }

  getCourseProgress(courseId: number): Observable<CourseProgress> {
    return this.http.get<CourseProgress>(`/api/courses/${courseId}/progress`);
  }

  /** Ids of the accessible lessons already completed (gap G1). */
  getCompletions(courseId: number): Observable<CourseCompletions> {
    return this.http.get<CourseCompletions>(`/api/courses/${courseId}/completions`);
  }

  /** Records the visit (updates last_viewed_at, used by the resume point). No body. */
  recordView(lessonId: number): Observable<LessonCompletionDetail> {
    return this.http.put<LessonCompletionDetail>(`/api/lessons/${lessonId}/view`, null);
  }

  /** US-PROGRESS-01 — marks the lesson as completed. No body. */
  markCompleted(lessonId: number): Observable<LessonCompletionDetail> {
    return this.http.post<LessonCompletionDetail>(`/api/lessons/${lessonId}/completion`, null);
  }

  /** Last lesson viewed in the course, or null when the student has not opened any lesson yet (404). */
  getResumePoint(courseId: number): Observable<ResumePoint | null> {
    return this.http.get<ResumePoint>(`/api/courses/${courseId}/resume`).pipe(
      catchError((error: unknown) =>
        error instanceof HttpErrorResponse && error.status === 404 ? of(null) : throwError(() => error),
      ),
    );
  }
}
