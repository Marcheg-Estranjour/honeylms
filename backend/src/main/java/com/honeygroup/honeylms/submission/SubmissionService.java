package com.honeygroup.honeylms.submission;

import com.honeygroup.honeylms.common.ForbiddenActionException;
import com.honeygroup.honeylms.course.Assignment;
import com.honeygroup.honeylms.course.AssignmentNotFoundException;
import com.honeygroup.honeylms.course.AssignmentRepository;
import com.honeygroup.honeylms.course.Course;
import com.honeygroup.honeylms.course.CourseAuthorizationService;
import com.honeygroup.honeylms.course.LearningAccessService;
import com.honeygroup.honeylms.file.FileStorageService;
import com.honeygroup.honeylms.file.StoredFile;
import com.honeygroup.honeylms.file.StoredFileRepository;
import com.honeygroup.honeylms.submission.dto.CorrectionRequest;
import com.honeygroup.honeylms.submission.dto.SubmissionDetail;
import com.honeygroup.honeylms.user.UserAccount;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final AssignmentRepository assignmentRepository;
    private final StoredFileRepository storedFileRepository;
    private final FileStorageService fileStorageService;
    private final CourseAuthorizationService courseAuthorizationService;
    private final LearningAccessService learningAccessService;

    public SubmissionService(SubmissionRepository submissionRepository,
                              AssignmentRepository assignmentRepository,
                              StoredFileRepository storedFileRepository,
                              FileStorageService fileStorageService,
                              CourseAuthorizationService courseAuthorizationService,
                              LearningAccessService learningAccessService) {
        this.submissionRepository = submissionRepository;
        this.assignmentRepository = assignmentRepository;
        this.storedFileRepository = storedFileRepository;
        this.fileStorageService = fileStorageService;
        this.courseAuthorizationService = courseAuthorizationService;
        this.learningAccessService = learningAccessService;
    }

    /**
     * US-SUB-01 — Submit assignment. STUDENT only (enforced in SecurityConfig).
     * Requires: enrolled, Assignment accessible (Course/Module/Lesson published)
     * and PUBLISHED itself, deadline not passed, no existing submission yet
     * (a second call must go through replace() / PUT, not this endpoint again).
     */
    @Transactional
    public SubmissionDetail submit(Long assignmentId, MultipartFile file, String studentEmail) {
        Assignment assignment = findAssignmentOrThrow(assignmentId);
        UserAccount student = courseAuthorizationService.resolveRequester(studentEmail);

        learningAccessService.assertStudentCanAccessAssignment(assignment, student);
        assertDeadlineNotPassed(assignment);

        if (submissionRepository.findByAssignment_IdAndStudent_Id(assignmentId, student.getId()).isPresent()) {
            throw new AlreadySubmittedException(assignmentId);
        }

        StoredFile storedFile = storeFile(file, student);

        Submission submission = Submission.builder()
                .assignment(assignment)
                .student(student)
                .storedFile(storedFile)
                .submittedAt(LocalDateTime.now())
                .status(SubmissionStatus.SUBMITTED)
                .build();

        return toDetail(submissionRepository.save(submission));
    }

    /**
     * US-SUB-02 — Replace submission. Same row updated (new file, new submittedAt).
     * CHOIX TECHNIQUE : replacing resets any prior correction (grade/feedback/
     * correctedBy/correctedAt -> null, status back to SUBMITTED) - a new file
     * deserves a fresh correction rather than keeping a grade for a file that no
     * longer exists. The old StoredFile is deleted (DB row + physical file), since
     * the schema keeps only the current version (no version history for the MVP).
     */
    @Transactional
    public SubmissionDetail replace(Long submissionId, MultipartFile file, String studentEmail) {
        Submission submission = findSubmissionOrThrow(submissionId);
        UserAccount student = courseAuthorizationService.resolveRequester(studentEmail);
        assertOwner(submission, student);
        assertDeadlineNotPassed(submission.getAssignment());

        StoredFile oldFile = submission.getStoredFile();
        StoredFile newFile = storeFile(file, student);

        submission.setStoredFile(newFile);
        submission.setSubmittedAt(LocalDateTime.now());
        submission.setStatus(SubmissionStatus.SUBMITTED);
        submission.setGrade(null);
        submission.setFeedback(null);
        submission.setCorrectedBy(null);
        submission.setCorrectedAt(null);

        Submission saved = submissionRepository.save(submission);

        fileStorageService.delete(oldFile.getStorageKey());
        storedFileRepository.delete(oldFile);

        return toDetail(saved);
    }

    /**
     * US-SUB-03 / part of US-SUB-04 — View submission. The owning Student, or a
     * Trainer/Admin within the perimeter of the Assignment's Course, may view it.
     */
    @Transactional(readOnly = true)
    public SubmissionDetail getSubmission(Long submissionId, String requesterEmail) {
        Submission submission = findSubmissionOrThrow(submissionId);
        UserAccount requester = courseAuthorizationService.resolveRequester(requesterEmail);

        if (learningAccessService.isStudent(requester)) {
            assertOwner(submission, requester);
        } else {
            courseAuthorizationService.assertCanManageCourse(courseOf(submission.getAssignment()), requester);
        }
        return toDetail(submission);
    }

    /**
     * US-SUB-04 — View submissions. TRAINER/ADMIN of the perimeter only.
     */
    @Transactional(readOnly = true)
    public List<SubmissionDetail> listSubmissions(Long assignmentId, String requesterEmail) {
        Assignment assignment = findAssignmentOrThrow(assignmentId);
        UserAccount requester = courseAuthorizationService.resolveRequester(requesterEmail);
        courseAuthorizationService.assertCanManageCourse(courseOf(assignment), requester);

        return submissionRepository.findByAssignment_IdOrderBySubmittedAtDesc(assignmentId).stream()
                .map(this::toDetail)
                .toList();
    }

    /**
     * US-SUB-05/06/07 — Correct, grade (optional, /20) and give feedback, in one call.
     * TRAINER/ADMIN of the perimeter only.
     */
    @Transactional
    public SubmissionDetail correctSubmission(Long submissionId, CorrectionRequest request, String requesterEmail) {
        Submission submission = findSubmissionOrThrow(submissionId);
        UserAccount requester = courseAuthorizationService.resolveRequester(requesterEmail);
        courseAuthorizationService.assertCanManageCourse(courseOf(submission.getAssignment()), requester);

        submission.setGrade(request.grade());
        submission.setFeedback(request.feedback());
        submission.setCorrectedBy(requester);
        submission.setCorrectedAt(LocalDateTime.now());
        submission.setStatus(SubmissionStatus.CORRECTED);

        return toDetail(submissionRepository.save(submission));
    }

    // ---- helpers ----

    private void assertDeadlineNotPassed(Assignment assignment) {
        if (assignment.getDueDate() != null && LocalDateTime.now().isAfter(assignment.getDueDate())) {
            throw new SubmissionDeadlinePassedException();
        }
    }

    private void assertOwner(Submission submission, UserAccount requester) {
        if (!submission.getStudent().getId().equals(requester.getId())) {
            throw new ForbiddenActionException("This submission does not belong to you");
        }
    }

    private Course courseOf(Assignment assignment) {
        return assignment.getLesson().getCourseModule().getCourse();
    }

    private StoredFile storeFile(MultipartFile file, UserAccount uploader) {
        FileStorageService.StoredContent content = fileStorageService.store(file);
        return storedFileRepository.save(StoredFile.builder()
                .originalName(content.originalName())
                .storageKey(content.storageKey())
                .mimeType(content.mimeType() != null ? content.mimeType() : "application/octet-stream")
                .sizeBytes(content.sizeBytes())
                .checksum(content.checksum())
                .createdBy(uploader)
                .build());
    }

    private Assignment findAssignmentOrThrow(Long assignmentId) {
        return assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new AssignmentNotFoundException(assignmentId));
    }

    private Submission findSubmissionOrThrow(Long submissionId) {
        return submissionRepository.findById(submissionId)
                .orElseThrow(() -> new SubmissionNotFoundException(submissionId));
    }

    private SubmissionDetail toDetail(Submission s) {
        return new SubmissionDetail(
                s.getId(),
                s.getAssignment().getId(),
                s.getStudent().getId(),
                s.getSubmittedAt(),
                s.getStatus().name(),
                s.getGrade(),
                s.getFeedback(),
                s.getCorrectedBy() != null ? s.getCorrectedBy().getId() : null,
                s.getCorrectedAt(),
                s.getStoredFile().getOriginalName(),
                s.getStoredFile().getMimeType(),
                s.getStoredFile().getSizeBytes()
        );
    }
}
