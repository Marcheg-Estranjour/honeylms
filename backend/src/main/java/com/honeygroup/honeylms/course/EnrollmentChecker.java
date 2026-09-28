package com.honeygroup.honeylms.course;

/**
 * Port defined by the course package and implemented by the enrollment package
 * (dependency inversion): course code needs to know "is this Student enrolled?",
 * but must not depend on the enrollment package - enrollment already depends on
 * course (Enrollment references Course), so the reverse would create a cycle.
 */
public interface EnrollmentChecker {

    boolean isStudentEnrolled(Long studentId, Long courseId);
}
