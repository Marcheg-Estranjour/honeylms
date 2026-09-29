package com.honeygroup.honeylms.course;

import com.honeygroup.honeylms.course.dto.AssignmentDetail;
import com.honeygroup.honeylms.course.dto.AttachedFileSummary;
import com.honeygroup.honeylms.course.dto.CreateAssignmentRequest;
import com.honeygroup.honeylms.course.dto.UpdateAssignmentRequest;
import com.honeygroup.honeylms.file.FileStorageService;
import com.honeygroup.honeylms.file.StoredFile;
import com.honeygroup.honeylms.file.StoredFileRepository;
import com.honeygroup.honeylms.user.UserAccount;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final AssignmentFileRepository assignmentFileRepository;
    private final LessonRepository lessonRepository;
    private final StoredFileRepository storedFileRepository;
    private final FileStorageService fileStorageService;
    private final CourseAuthorizationService courseAuthorizationService;
    private final LearningAccessService learningAccessService;

    public AssignmentService(AssignmentRepository assignmentRepository,
                              AssignmentFileRepository assignmentFileRepository,
                              LessonRepository lessonRepository,
                              StoredFileRepository storedFileRepository,
                              FileStorageService fileStorageService,
                              CourseAuthorizationService courseAuthorizationService,
                              LearningAccessService learningAccessService) {
        this.assignmentRepository = assignmentRepository;
        this.assignmentFileRepository = assignmentFileRepository;
        this.lessonRepository = lessonRepository;
        this.storedFileRepository = storedFileRepository;
        this.fileStorageService = fileStorageService;
        this.courseAuthorizationService = courseAuthorizationService;
        this.learningAccessService = learningAccessService;
    }

    /** US-ASSIGN-01 — Create assignment. TRAINER (own perimeter) or ADMIN. Always DRAFT. */
    @Transactional
    public AssignmentDetail createAssignment(Long lessonId, CreateAssignmentRequest request, String requesterEmail) {
        Lesson lesson = findLessonOrThrow(lessonId);
        UserAccount requester = courseAuthorizationService.resolveRequester(requesterEmail);
        courseAuthorizationService.assertCanManageCourse(lesson.getCourseModule().getCourse(), requester);

        Assignment assignment = Assignment.builder()
                .lesson(lesson)
                .title(request.title().trim())
                .description(request.description())
                .dueDate(request.dueDate())
                .status(PublicationStatus.DRAFT)
                .build();

        return toDetail(assignmentRepository.save(assignment));
    }

    @Transactional
    public AssignmentDetail updateAssignment(Long assignmentId, UpdateAssignmentRequest request, String requesterEmail) {
        Assignment assignment = findAssignmentOrThrow(assignmentId);
        UserAccount requester = courseAuthorizationService.resolveRequester(requesterEmail);
        courseAuthorizationService.assertCanManageCourse(assignment.getLesson().getCourseModule().getCourse(), requester);

        assignment.setTitle(request.title().trim());
        assignment.setDescription(request.description());
        assignment.setDueDate(request.dueDate());

        return toDetail(assignmentRepository.save(assignment));
    }

    /** US-ASSIGN-02 — Publish assignment. */
    @Transactional
    public AssignmentDetail publishAssignment(Long assignmentId, String requesterEmail) {
        Assignment assignment = findAssignmentOrThrow(assignmentId);
        UserAccount requester = courseAuthorizationService.resolveRequester(requesterEmail);
        courseAuthorizationService.assertCanManageCourse(assignment.getLesson().getCourseModule().getCourse(), requester);

        assignment.setStatus(PublicationStatus.PUBLISHED);
        return toDetail(assignmentRepository.save(assignment));
    }

    /** US-ASSIGN-03 — Attach files. Several files can be attached over multiple calls. */
    @Transactional
    public AssignmentDetail attachFile(Long assignmentId, MultipartFile file, String requesterEmail) {
        Assignment assignment = findAssignmentOrThrow(assignmentId);
        UserAccount requester = courseAuthorizationService.resolveRequester(requesterEmail);
        courseAuthorizationService.assertCanManageCourse(assignment.getLesson().getCourseModule().getCourse(), requester);

        FileStorageService.StoredContent content = fileStorageService.store(file);

        StoredFile storedFile = storedFileRepository.save(StoredFile.builder()
                .originalName(content.originalName())
                .storageKey(content.storageKey())
                .mimeType(content.mimeType() != null ? content.mimeType() : "application/octet-stream")
                .sizeBytes(content.sizeBytes())
                .checksum(content.checksum())
                .createdBy(requester)
                .build());

        assignmentFileRepository.save(AssignmentFile.of(assignment, storedFile));

        return toDetail(assignment);
    }

    /**
     * US-ASSIGN-04 — View assignment. Role-aware: Trainer/Admin of the perimeter
     * (any status), or a Student with access to the Lesson AND the Assignment PUBLISHED.
     */
    @Transactional(readOnly = true)
    public AssignmentDetail getAssignment(Long assignmentId, String requesterEmail) {
        Assignment assignment = findAssignmentOrThrow(assignmentId);
        UserAccount requester = courseAuthorizationService.resolveRequester(requesterEmail);

        if (learningAccessService.isStudent(requester)) {
            learningAccessService.assertStudentCanAccessAssignment(assignment, requester);
        } else {
            courseAuthorizationService.assertCanManageCourse(assignment.getLesson().getCourseModule().getCourse(), requester);
        }
        return toDetail(assignment);
    }

    @Transactional(readOnly = true)
    public List<AssignmentDetail> listAssignments(Long lessonId, String requesterEmail) {
        Lesson lesson = findLessonOrThrow(lessonId);
        UserAccount requester = courseAuthorizationService.resolveRequester(requesterEmail);
        List<Assignment> assignments = assignmentRepository.findByLesson_IdOrderByCreatedAtAsc(lessonId);

        if (learningAccessService.isStudent(requester)) {
            learningAccessService.assertStudentCanAccessLesson(lesson, requester);
            assignments = assignments.stream()
                    .filter(a -> a.getStatus() == PublicationStatus.PUBLISHED)
                    .toList();
        } else {
            courseAuthorizationService.assertCanManageCourse(lesson.getCourseModule().getCourse(), requester);
        }

        return assignments.stream().map(this::toDetail).toList();
    }

    private Lesson findLessonOrThrow(Long lessonId) {
        return lessonRepository.findById(lessonId)
                .orElseThrow(() -> new LessonNotFoundException(lessonId));
    }

    private Assignment findAssignmentOrThrow(Long assignmentId) {
        return assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new AssignmentNotFoundException(assignmentId));
    }

    private AssignmentDetail toDetail(Assignment assignment) {
        List<AttachedFileSummary> files = assignmentFileRepository.findByAssignment_Id(assignment.getId()).stream()
                .map(af -> new AttachedFileSummary(
                        af.getStoredFile().getId(),
                        af.getStoredFile().getOriginalName(),
                        af.getStoredFile().getMimeType(),
                        af.getStoredFile().getSizeBytes()))
                .toList();

        return new AssignmentDetail(
                assignment.getId(),
                assignment.getLesson().getId(),
                assignment.getTitle(),
                assignment.getDescription(),
                assignment.getDueDate(),
                assignment.getStatus().name(),
                files
        );
    }
}
