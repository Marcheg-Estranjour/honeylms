package com.honeygroup.honeylms.enrollment;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentRepository extends JpaRepository<Enrollment, EnrollmentId> {

    boolean existsByStudent_IdAndCourse_Id(Long studentId, Long courseId);

    List<Enrollment> findByStudent_IdOrderByEnrolledAtDesc(Long studentId);
}
