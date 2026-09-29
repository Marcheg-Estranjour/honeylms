package com.honeygroup.honeylms.course;

import com.honeygroup.honeylms.common.ForbiddenActionException;
import com.honeygroup.honeylms.user.RoleCode;
import com.honeygroup.honeylms.user.UserAccount;
import org.springframework.stereotype.Service;

/**
 * The Student access rule (Dossier de Conception §23):
 * Enrollment exists AND Course PUBLISHED AND Module PUBLISHED AND Lesson PUBLISHED.
 * TrainingClass membership never appears here - Enrollment is the only source of access.
 * Depends on the EnrollmentChecker port, not on the enrollment package (no package cycle).
 */
@Service
public class LearningAccessService {

    private final EnrollmentChecker enrollmentChecker;

    public LearningAccessService(EnrollmentChecker enrollmentChecker) {
        this.enrollmentChecker = enrollmentChecker;
    }

    public boolean isStudent(UserAccount user) {
        return RoleCode.STUDENT.name().equals(user.getRole().getCode());
    }

    public void assertStudentCanAccessCourse(Course course, UserAccount student) {
        boolean enrolled = enrollmentChecker.isStudentEnrolled(student.getId(), course.getId());
        if (course.getStatus() != PublicationStatus.PUBLISHED || !enrolled) {
            throw new ForbiddenActionException("You do not have access to this course");
        }
    }

    public void assertStudentCanAccessModule(CourseModule module, UserAccount student) {
        assertStudentCanAccessCourse(module.getCourse(), student);
        if (module.getStatus() != PublicationStatus.PUBLISHED) {
            throw new ForbiddenActionException("This module is not published");
        }
    }

    public void assertStudentCanAccessLesson(Lesson lesson, UserAccount student) {
        assertStudentCanAccessModule(lesson.getCourseModule(), student);
        if (lesson.getStatus() != PublicationStatus.PUBLISHED) {
            throw new ForbiddenActionException("This lesson is not published");
        }
    }

    /**
     * US-ASSIGN-04 — same chain as a Lesson, plus the Assignment itself must be PUBLISHED.
     */
    public void assertStudentCanAccessAssignment(Assignment assignment, UserAccount student) {
        assertStudentCanAccessLesson(assignment.getLesson(), student);
        if (assignment.getStatus() != PublicationStatus.PUBLISHED) {
            throw new ForbiddenActionException("This assignment is not published");
        }
    }
}
