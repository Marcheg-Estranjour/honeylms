package com.honeygroup.honeylms.enrollment;

import com.honeygroup.honeylms.course.EnrollmentChecker;
import org.springframework.stereotype.Component;

/**
 * Enrollment-side implementation of the course package's EnrollmentChecker port.
 */
@Component
public class EnrollmentAccessChecker implements EnrollmentChecker {

    private final EnrollmentRepository enrollmentRepository;

    public EnrollmentAccessChecker(EnrollmentRepository enrollmentRepository) {
        this.enrollmentRepository = enrollmentRepository;
    }

    @Override
    public boolean isStudentEnrolled(Long studentId, Long courseId) {
        return enrollmentRepository.existsByStudent_IdAndCourse_Id(studentId, courseId);
    }
}
