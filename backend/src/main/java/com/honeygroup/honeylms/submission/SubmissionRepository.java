package com.honeygroup.honeylms.submission;

import com.honeygroup.honeylms.common.IdCount;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    Optional<Submission> findByAssignment_IdAndStudent_Id(Long assignmentId, Long studentId);

    List<Submission> findByAssignment_IdOrderBySubmittedAtDesc(Long assignmentId);

    /** Submissions in the given status, per course (e.g. SUBMITTED = « à corriger »). */
    @Query("""
            select new com.honeygroup.honeylms.common.IdCount(c.id, count(s))
            from Submission s
            join s.assignment a
            join a.lesson l
            join l.courseModule m
            join m.course c
            where s.status = :status and c.id in :courseIds
            group by c.id""")
    List<IdCount> countByCourseAndStatus(@Param("courseIds") Collection<Long> courseIds,
                                         @Param("status") SubmissionStatus status);

    /** All submissions per assignment. */
    @Query("""
            select new com.honeygroup.honeylms.common.IdCount(s.assignment.id, count(s))
            from Submission s
            where s.assignment.id in :assignmentIds
            group by s.assignment.id""")
    List<IdCount> countByAssignment(@Param("assignmentIds") Collection<Long> assignmentIds);

    /** Submissions in the given status, per assignment. */
    @Query("""
            select new com.honeygroup.honeylms.common.IdCount(s.assignment.id, count(s))
            from Submission s
            where s.status = :status and s.assignment.id in :assignmentIds
            group by s.assignment.id""")
    List<IdCount> countByAssignmentAndStatus(@Param("assignmentIds") Collection<Long> assignmentIds,
                                             @Param("status") SubmissionStatus status);
}
