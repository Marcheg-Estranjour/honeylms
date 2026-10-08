import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { CourseCategory, CourseDetail } from '../../shared/courses/course.models';
import { LessonDetail, ModuleDetail } from '../learning/learning.models';

/** Body of PUT /api/courses/{id} (backend record `UpdateCourseRequest`). */
export interface CourseForm {
  title: string;
  description: string | null;
  category: CourseCategory;
}

/** Body of POST/PUT for a module (`CreateModuleRequest` / `UpdateModuleRequest`). */
export interface ModuleForm {
  title: string;
  description: string | null;
}

/** Body of POST/PUT for a lesson (`CreateLessonRequest` / `UpdateLessonRequest`). */
export interface LessonForm {
  title: string;
  description: string | null;
  content: string | null;
}

/**
 * Write side of a course for its Trainer(s) or an Admin (US-COURSE-04/05/06, US-MODULE-*,
 * US-LESSON-*). The backend checks CourseTrainer on every call. New modules and lessons are
 * always created as DRAFT, at the end of the list (display order = count + 1).
 */
@Injectable({ providedIn: 'root' })
export class CourseEditorService {
  private readonly http = inject(HttpClient);

  updateCourse(courseId: number, body: CourseForm): Observable<CourseDetail> {
    return this.http.put<CourseDetail>(`/api/courses/${courseId}`, body);
  }

  publishCourse(courseId: number): Observable<CourseDetail> {
    return this.http.patch<CourseDetail>(`/api/courses/${courseId}/publish`, null);
  }

  unpublishCourse(courseId: number): Observable<CourseDetail> {
    return this.http.patch<CourseDetail>(`/api/courses/${courseId}/unpublish`, null);
  }

  createModule(courseId: number, body: ModuleForm): Observable<ModuleDetail> {
    return this.http.post<ModuleDetail>(`/api/courses/${courseId}/modules`, body);
  }

  updateModule(moduleId: number, body: ModuleForm): Observable<ModuleDetail> {
    return this.http.put<ModuleDetail>(`/api/modules/${moduleId}`, body);
  }

  publishModule(moduleId: number): Observable<ModuleDetail> {
    return this.http.patch<ModuleDetail>(`/api/modules/${moduleId}/publish`, null);
  }

  createLesson(moduleId: number, body: LessonForm): Observable<LessonDetail> {
    return this.http.post<LessonDetail>(`/api/modules/${moduleId}/lessons`, body);
  }

  updateLesson(lessonId: number, body: LessonForm): Observable<LessonDetail> {
    return this.http.put<LessonDetail>(`/api/lessons/${lessonId}`, body);
  }

  publishLesson(lessonId: number): Observable<LessonDetail> {
    return this.http.patch<LessonDetail>(`/api/lessons/${lessonId}/publish`, null);
  }
}
