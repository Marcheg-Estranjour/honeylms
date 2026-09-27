package com.honeygroup.honeylms.course;

/**
 * Shared publication status for Course, CourseModule and Lesson.
 * Only PUBLISHED content is visible to Students (see Dossier de Conception §5, règle 9).
 */
public enum PublicationStatus {
    DRAFT,
    PUBLISHED
}
