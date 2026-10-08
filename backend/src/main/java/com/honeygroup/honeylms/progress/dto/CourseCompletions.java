package com.honeygroup.honeylms.progress.dto;

import java.util.List;

/**
 * Lessons of a course the Student has completed (gap G1, used for the check marks of the
 * lesson view). Only ACCESSIBLE lessons are listed (Course + Module + Lesson PUBLISHED),
 * consistently with the progress calculation.
 */
public record CourseCompletions(
        Long courseId,
        List<Long> completedLessonIds
) {
}
