/**
 * TypeScript mirrors of the backend course / enrollment DTOs.
 * Written by hand from the Java records — keep them in sync when a record changes.
 */

/** Course.category (Dossier §12.6). Fixed list, no Category table. */
export type CourseCategory = 'LANGUAGES' | 'OFFICE_AUTOMATION' | 'EDUCTOUR';

export type PublicationStatus = 'DRAFT' | 'PUBLISHED';

/** Backend record `CourseSummary` — GET /api/courses (published courses only). */
export interface CourseSummary {
  id: number;
  title: string;
  description: string | null;
  category: CourseCategory;
}

/** Backend record `CourseDetail` — GET /api/courses/{id}. */
export interface CourseDetail extends CourseSummary {
  status: PublicationStatus;
  createdByUserId: number;
}

/** Backend record `EnrolledCourse` — GET /api/me/courses. Dates are ISO-8601 UTC instants. */
export interface EnrolledCourse {
  courseId: number;
  title: string;
  description: string | null;
  category: CourseCategory;
  enrolledAt: string;
}

/** Backend record `EnrollmentResponse` — POST /api/courses/{id}/enrollment. */
export interface EnrollmentResponse {
  courseId: number;
  enrolledAt: string;
}
