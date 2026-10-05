package com.honeygroup.honeylms.enrollment;

import com.honeygroup.honeylms.course.Course;
import com.honeygroup.honeylms.user.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A Student's enrollment in a Course. This is the ONLY source of pedagogical
 * access for a Student - TrainingClass membership never grants access.
 * enrolledAt is set explicitly (not via JPA auditing): with an assigned composite
 * key, Spring Data treats the entity as "not new" and would skip @CreatedDate.
 */
@Entity
@Table(name = "enrollment")
@Getter
@Setter
@NoArgsConstructor
public class Enrollment {

    @EmbeddedId
    private EnrollmentId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("studentUserAccountId")
    @JoinColumn(name = "student_user_account_id")
    private UserAccount student;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("courseId")
    @JoinColumn(name = "course_id")
    private Course course;

    @Column(name = "enrolled_at", nullable = false, updatable = false)
    private Instant enrolledAt;

    public static Enrollment of(UserAccount student, Course course) {
        Enrollment enrollment = new Enrollment();
        enrollment.setId(new EnrollmentId(student.getId(), course.getId()));
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollment.setEnrolledAt(Instant.now());
        return enrollment;
    }
}
