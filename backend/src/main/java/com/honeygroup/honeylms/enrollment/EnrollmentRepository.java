package com.honeygroup.honeylms.enrollment;

import com.honeygroup.honeylms.common.IdCount;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EnrollmentRepository extends JpaRepository<Enrollment, EnrollmentId> {

    boolean existsByStudent_IdAndCourse_Id(Long studentId, Long courseId);

    List<Enrollment> findByStudent_IdOrderByEnrolledAtDesc(Long studentId);

    /** Number of enrolled students per course (one row per course having at least one enrollment). */
    @Query("""
            select new com.honeygroup.honeylms.common.IdCount(e.course.id, count(e))
            from Enrollment e
            where e.course.id in :courseIds
            group by e.course.id""")
    List<IdCount> countByCourse(@Param("courseIds") Collection<Long> courseIds);

    /** Enrollments of a course with the student fetched, sorted by last name then first name (gap G10). */
    @Query("""
            select e from Enrollment e
            join fetch e.student s
            where e.course.id = :courseId
            order by s.lastName, s.firstName""")
    List<Enrollment> findWithStudentByCourseId(@Param("courseId") Long courseId);
}
