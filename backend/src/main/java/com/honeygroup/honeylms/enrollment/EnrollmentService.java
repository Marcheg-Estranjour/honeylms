package com.honeygroup.honeylms.enrollment;

import com.honeygroup.honeylms.common.ForbiddenActionException;
import com.honeygroup.honeylms.course.Course;
import com.honeygroup.honeylms.course.CourseNotFoundException;
import com.honeygroup.honeylms.course.CourseRepository;
import com.honeygroup.honeylms.course.PublicationStatus;
import com.honeygroup.honeylms.enrollment.dto.EnrolledCourse;
import com.honeygroup.honeylms.enrollment.dto.EnrollmentResponse;
import com.honeygroup.honeylms.user.UserAccount;
import com.honeygroup.honeylms.user.UserAccountRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final UserAccountRepository userAccountRepository;

    public EnrollmentService(EnrollmentRepository enrollmentRepository,
                              CourseRepository courseRepository,
                              UserAccountRepository userAccountRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.courseRepository = courseRepository;
        this.userAccountRepository = userAccountRepository;
    }

    /**
     * US-ENROLL-01 — Enroll in course. Free and immediate (validated business rule):
     * no approval step, no Class required. One enrollment per Student and Course.
     * STUDENT role is enforced at the URL level in SecurityConfig.
     */
    @Transactional
    public EnrollmentResponse enroll(Long courseId, String studentEmail) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));

        if (course.getStatus() != PublicationStatus.PUBLISHED) {
            throw new ForbiddenActionException("This course is not open for enrollment");
        }

        UserAccount student = resolveStudent(studentEmail);

        if (enrollmentRepository.existsByStudent_IdAndCourse_Id(student.getId(), courseId)) {
            throw new AlreadyEnrolledException(courseId);
        }

        Enrollment saved = enrollmentRepository.save(Enrollment.of(student, course));
        return new EnrollmentResponse(courseId, saved.getEnrolledAt());
    }

    /**
     * US-ENROLL-02 — View my courses (most recent enrollment first).
     */
    @Transactional(readOnly = true)
    public List<EnrolledCourse> listMyCourses(String studentEmail) {
        UserAccount student = resolveStudent(studentEmail);

        return enrollmentRepository.findByStudent_IdOrderByEnrolledAtDesc(student.getId()).stream()
                .map(e -> new EnrolledCourse(
                        e.getCourse().getId(),
                        e.getCourse().getTitle(),
                        e.getCourse().getDescription(),
                        e.getCourse().getCategory().name(),
                        e.getEnrolledAt()))
                .toList();
    }

    private UserAccount resolveStudent(String email) {
        return userAccountRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated user not found in database: " + email));
    }
}
