import { PublicationStatus } from '../../shared/courses/course.models';

/*
 * TypeScript mirrors of the backend learning / progress DTOs.
 * Written by hand from the Java records — keep them in sync when a record changes.
 */

/** Backend record `ModuleDetail`. */
export interface ModuleDetail {
  id: number;
  courseId: number;
  title: string;
  description: string | null;
  displayOrder: number;
  status: PublicationStatus;
}

/** Backend record `LessonDetail`. */
export interface LessonDetail {
  id: number;
  moduleId: number;
  title: string;
  description: string | null;
  content: string | null;
  displayOrder: number;
  status: PublicationStatus;
}

/**
 * Backend record `CourseProgress` — GET /api/courses/{id}/progress.
 * percentage = completed / accessible × 100 (grades never count, Dossier §5 rule 12).
 */
export interface CourseProgress {
  courseId: number;
  completedLessons: number;
  accessibleLessons: number;
  percentage: number;
}

/** Backend record `ResumeResponse` — GET /api/courses/{id}/resume (404 when nothing viewed yet). */
export interface ResumePoint {
  lessonId: number;
  moduleId: number;
  lastViewedAt: string;
}
