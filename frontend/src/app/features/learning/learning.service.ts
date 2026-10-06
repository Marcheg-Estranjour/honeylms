import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, Observable, of, throwError } from 'rxjs';
import { CourseProgress, LessonDetail, ModuleDetail, ResumePoint } from './learning.models';

/**
 * Student side of the learning API: modules, lessons, progress, resume point.
 * Access rules (enrollment + Course/Module/Lesson PUBLISHED) are enforced by the backend.
 */
@Injectable({ providedIn: 'root' })
export class LearningService {
  private readonly http = inject(HttpClient);

  getModule(moduleId: number): Observable<ModuleDetail> {
    return this.http.get<ModuleDetail>(`/api/modules/${moduleId}`);
  }

  getLesson(lessonId: number): Observable<LessonDetail> {
    return this.http.get<LessonDetail>(`/api/lessons/${lessonId}`);
  }

  getCourseProgress(courseId: number): Observable<CourseProgress> {
    return this.http.get<CourseProgress>(`/api/courses/${courseId}/progress`);
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
