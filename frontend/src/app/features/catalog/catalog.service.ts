import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { CourseCategory, CourseDetail, CourseSummary } from '../../shared/courses/course.models';

/** Read access to the course catalogue (US-COURSE-01 / US-COURSE-02). */
@Injectable({ providedIn: 'root' })
export class CatalogService {
  private readonly http = inject(HttpClient);

  /** Published courses only (the backend never returns drafts here). */
  getCourses(category?: CourseCategory): Observable<CourseSummary[]> {
    const params = category ? new HttpParams().set('category', category) : undefined;
    return this.http.get<CourseSummary[]>('/api/courses', { params });
  }

  getCourse(courseId: number): Observable<CourseDetail> {
    return this.http.get<CourseDetail>(`/api/courses/${courseId}`);
  }
}
