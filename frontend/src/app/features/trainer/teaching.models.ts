import { CourseCategory, PublicationStatus } from '../../shared/courses/course.models';

/*
 * TypeScript mirrors of the backend « teaching » DTOs (Trainer workspace, gaps G5, G10, G11).
 * Written by hand from the Java records — keep them in sync when a record changes.
 */

/** Backend record `ManagedCourseSummary` — GET /api/me/managed-courses (DRAFT included). */
export interface ManagedCourseSummary {
  id: number;
  title: string;
  description: string | null;
  category: CourseCategory;
  status: PublicationStatus;
  enrolledStudents: number;
  /** Submissions waiting for a correction (status SUBMITTED). */
  submissionsToCorrect: number;
}

/** Backend record `ManagedAssignmentSummary` — GET /api/me/managed-assignments. */
export interface ManagedAssignmentSummary {
  id: number;
  title: string;
  /** ISO-8601 UTC instant, or null when there is no deadline. */
  dueDate: string | null;
  status: PublicationStatus;
  courseId: number;
  courseTitle: string;
  lessonId: number;
  lessonTitle: string;
  enrolledStudents: number;
  submissions: number;
  submissionsToCorrect: number;
}

/** Backend record `EnrolledStudent` — GET /api/courses/{id}/students. */
export interface EnrolledStudent {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  enrolledAt: string;
}

/** Backend record `CorrectionRequest` — PATCH /api/submissions/{id}/correction. */
export interface CorrectionRequest {
  /** 0 to 20, at most 2 decimals (DECIMAL(4,2)); null = no grade. */
  grade: number | null;
  feedback: string | null;
}
