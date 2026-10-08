import { inject, Injectable } from '@angular/core';
import { forkJoin, map, Observable, of, switchMap } from 'rxjs';
import { CourseDetail } from '../../shared/courses/course.models';
import { CatalogService } from '../catalog/catalog.service';
import { LessonDetail, ModuleDetail } from './learning.models';
import { LearningService } from './learning.service';

export interface OutlineModule {
  module: ModuleDetail;
  lessons: LessonDetail[];
}

/** What a student can follow in a course: published modules and their published lessons. */
export interface CourseOutline {
  course: CourseDetail;
  modules: OutlineModule[];
  /** Every lesson in reading order (module order, then lesson order). */
  lessons: LessonDetail[];
}

/**
 * Builds the course outline: course + modules + lessons of each module.
 * The backend only returns PUBLISHED modules and lessons to a student and answers 403
 * when the student is not enrolled: the outline is therefore exactly what is accessible.
 */
@Injectable({ providedIn: 'root' })
export class CourseOutlineService {
  private readonly catalog = inject(CatalogService);
  private readonly learning = inject(LearningService);

  load(courseId: number): Observable<CourseOutline> {
    return forkJoin({
      course: this.catalog.getCourse(courseId),
      modules: this.learning.listModules(courseId),
    }).pipe(
      switchMap(({ course, modules }) => {
        const sorted = [...modules].sort((a, b) => a.displayOrder - b.displayOrder);
        const lessonsPerModule = sorted.length
          ? forkJoin(sorted.map((m) => this.learning.listLessons(m.id)))
          : of([] as LessonDetail[][]);
        return lessonsPerModule.pipe(
          map((lessonLists) => {
            const outlineModules = sorted.map((module, i) => ({
              module,
              lessons: [...lessonLists[i]].sort((a, b) => a.displayOrder - b.displayOrder),
            }));
            return {
              course,
              modules: outlineModules,
              lessons: outlineModules.flatMap((m) => m.lessons),
            };
          }),
        );
      }),
    );
  }
}
