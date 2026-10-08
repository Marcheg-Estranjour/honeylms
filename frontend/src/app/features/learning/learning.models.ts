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

/** Backend record `CourseCompletions` — GET /api/courses/{id}/completions (gap G1). */
export interface CourseCompletions {
  courseId: number;
  completedLessonIds: number[];
}

/** Backend record `LessonCompletionDetail` — PUT /view and POST /completion. */
export interface LessonCompletionDetail {
  lessonId: number;
  lastViewedAt: string | null;
  completedAt: string | null;
}

/** Backend record `ResourceDetail` — GET /api/lessons/{id}/resources. */
export interface ResourceDetail {
  id: number;
  lessonId: number;
  title: string;
  displayOrder: number;
  originalFileName: string;
  mimeType: string;
  sizeBytes: number;
}

/** Backend record `AttachedFileSummary` (files attached to an assignment). */
export interface AttachedFileSummary {
  storedFileId: number;
  originalName: string;
  mimeType: string;
  sizeBytes: number;
}

/** Backend record `AssignmentDetail` — GET /api/lessons/{id}/assignments (published only for a student). */
export interface AssignmentDetail {
  id: number;
  lessonId: number;
  title: string;
  description: string | null;
  /** ISO-8601 UTC instant, or null when there is no deadline. */
  dueDate: string | null;
  status: PublicationStatus;
  files: AttachedFileSummary[];
}

/** Backend enum `SubmissionStatus`. */
export type SubmissionStatus = 'SUBMITTED' | 'CORRECTED';

/**
 * Backend record `SubmissionDetail` — the student's own submission
 * (GET /api/assignments/{id}/submissions/me, 404 when nothing submitted yet).
 * Grade and correction fields are null until a trainer corrects it, and are reset by a replacement.
 */
export interface SubmissionDetail {
  id: number;
  assignmentId: number;
  studentId: number;
  submittedAt: string;
  status: SubmissionStatus;
  /** Grade out of 20 (BigDecimal serialised as a JSON number), optional even when corrected. */
  grade: number | null;
  feedback: string | null;
  correctedByUserId: number | null;
  correctedAt: string | null;
  originalFileName: string;
  mimeType: string;
  sizeBytes: number;
}
