import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { UserSummary } from '../../core/auth/auth.models';
import { CourseCategory, CourseDetail } from '../../shared/courses/course.models';

/** Backend record `CreateCourseRequest`. The course is always created as DRAFT, without trainer. */
export interface CreateCourseRequest {
  title: string;
  description: string | null;
  category: CourseCategory;
}

/**
 * Admin back-office for courses: creation (US-COURSE-03) and trainer assignment (US-ADMIN-07).
 * The list itself comes from GET /api/me/managed-courses (an Admin gets every course).
 */
@Injectable({ providedIn: 'root' })
export class AdminCoursesService {
  private readonly http = inject(HttpClient);

  createCourse(body: CreateCourseRequest): Observable<CourseDetail> {
    return this.http.post<CourseDetail>('/api/courses', body);
  }

  listTrainers(courseId: number): Observable<UserSummary[]> {
    return this.http.get<UserSummary[]>(`/api/courses/${courseId}/trainers`);
  }

  /** 201, no body. 409 if already assigned, 422 if the user is not a Trainer. */
  assignTrainer(courseId: number, trainerId: number): Observable<void> {
    return this.http.post<void>(`/api/courses/${courseId}/trainers/${trainerId}`, null);
  }

  /** 204. */
  unassignTrainer(courseId: number, trainerId: number): Observable<void> {
    return this.http.delete<void>(`/api/courses/${courseId}/trainers/${trainerId}`);
  }
}
