package com.honeygroup.honeylms.submission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.honeygroup.honeylms.common.ForbiddenActionException;
import com.honeygroup.honeylms.course.Assignment;
import com.honeygroup.honeylms.course.AssignmentRepository;
import com.honeygroup.honeylms.course.Course;
import com.honeygroup.honeylms.course.CourseAuthorizationService;
import com.honeygroup.honeylms.course.CourseCategory;
import com.honeygroup.honeylms.course.CourseModule;
import com.honeygroup.honeylms.course.LearningAccessService;
import com.honeygroup.honeylms.course.Lesson;
import com.honeygroup.honeylms.course.PublicationStatus;
import com.honeygroup.honeylms.file.FileStorageService;
import com.honeygroup.honeylms.file.StoredFile;
import com.honeygroup.honeylms.file.StoredFileRepository;
import com.honeygroup.honeylms.submission.dto.CorrectionRequest;
import com.honeygroup.honeylms.submission.dto.SubmissionDetail;
import com.honeygroup.honeylms.user.Role;
import com.honeygroup.honeylms.user.UserAccount;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceTest {

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private StoredFileRepository storedFileRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private CourseAuthorizationService courseAuthorizationService;

    @Mock
    private LearningAccessService learningAccessService;

    @InjectMocks
    private SubmissionService submissionService;

    private UserAccount student() {
        return UserAccount.builder().id(5L).email("student@example.com")
                .role(new Role(1L, "STUDENT", "Student")).build();
    }

    private UserAccount trainer() {
        return UserAccount.builder().id(1L).email("trainer1@example.com")
                .role(new Role(2L, "TRAINER", "Trainer")).build();
    }

    private Assignment assignment(Instant dueDate) {
        Course course = Course.builder().id(10L).category(CourseCategory.LANGUAGES).build();
        CourseModule module = CourseModule.builder().id(100L).course(course).status(PublicationStatus.PUBLISHED).build();
        Lesson lesson = Lesson.builder().id(1000L).courseModule(module).status(PublicationStatus.PUBLISHED).build();
        return Assignment.builder().id(2000L).lesson(lesson).title("Devoir")
                .status(PublicationStatus.PUBLISHED).dueDate(dueDate).build();
    }

    // ---- submit() ----

    @Test
    void submit_createsSubmission_whenAllowedAndNoExistingSubmission() {
        UserAccount student = student();
        Assignment assignment = assignment(Instant.now().plus(1, ChronoUnit.DAYS));
        MockMultipartFile file = new MockMultipartFile("file", "devoir.pdf", "application/pdf", "c".getBytes());

        when(assignmentRepository.findById(2000L)).thenReturn(Optional.of(assignment));
        when(courseAuthorizationService.resolveRequester("student@example.com")).thenReturn(student);
        when(submissionRepository.findByAssignment_IdAndStudent_Id(2000L, 5L)).thenReturn(Optional.empty());
        when(fileStorageService.store(file)).thenReturn(
                new FileStorageService.StoredContent("devoir.pdf", "uuid.pdf", "application/pdf", 1L, "abc"));
        when(storedFileRepository.save(any(StoredFile.class))).thenAnswer(inv -> {
            StoredFile sf = inv.getArgument(0);
            sf.setId(500L);
            return sf;
        });
        when(submissionRepository.save(any(Submission.class))).thenAnswer(inv -> {
            Submission s = inv.getArgument(0);
            s.setId(9000L);
            return s;
        });

        SubmissionDetail result = submissionService.submit(2000L, file, "student@example.com");

        assertThat(result.status()).isEqualTo("SUBMITTED");
        assertThat(result.assignmentId()).isEqualTo(2000L);
        assertThat(result.studentId()).isEqualTo(5L);
        verify(learningAccessService).assertStudentCanAccessAssignment(assignment, student);
    }

    @Test
    void submit_throwsAlreadySubmitted_whenSubmissionExists() {
        UserAccount student = student();
        Assignment assignment = assignment(null);
        MockMultipartFile file = new MockMultipartFile("file", "devoir.pdf", "application/pdf", "c".getBytes());
        Submission existing = Submission.builder().id(9000L).build();

        when(assignmentRepository.findById(2000L)).thenReturn(Optional.of(assignment));
        when(courseAuthorizationService.resolveRequester("student@example.com")).thenReturn(student);
        when(submissionRepository.findByAssignment_IdAndStudent_Id(2000L, 5L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> submissionService.submit(2000L, file, "student@example.com"))
                .isInstanceOf(AlreadySubmittedException.class);
        verify(submissionRepository, never()).save(any());
    }

    @Test
    void submit_throwsDeadlinePassed_whenDueDateInThePast() {
        UserAccount student = student();
        Assignment assignment = assignment(Instant.now().minus(1, ChronoUnit.DAYS));
        MockMultipartFile file = new MockMultipartFile("file", "devoir.pdf", "application/pdf", "c".getBytes());

        when(assignmentRepository.findById(2000L)).thenReturn(Optional.of(assignment));
        when(courseAuthorizationService.resolveRequester("student@example.com")).thenReturn(student);

        assertThatThrownBy(() -> submissionService.submit(2000L, file, "student@example.com"))
                .isInstanceOf(SubmissionDeadlinePassedException.class);
        verify(submissionRepository, never()).save(any());
    }

    @Test
    void submit_propagatesForbidden_whenStudentCannotAccessAssignment() {
        UserAccount student = student();
        Assignment assignment = assignment(null);
        MockMultipartFile file = new MockMultipartFile("file", "devoir.pdf", "application/pdf", "c".getBytes());

        when(assignmentRepository.findById(2000L)).thenReturn(Optional.of(assignment));
        when(courseAuthorizationService.resolveRequester("student@example.com")).thenReturn(student);
        doThrow(new ForbiddenActionException("not enrolled"))
                .when(learningAccessService).assertStudentCanAccessAssignment(assignment, student);

        assertThatThrownBy(() -> submissionService.submit(2000L, file, "student@example.com"))
                .isInstanceOf(ForbiddenActionException.class);
    }

    // ---- replace() ----

    @Test
    void replace_updatesSameRowAndResetsCorrection_whenOwnerAndBeforeDeadline() {
        UserAccount student = student();
        Assignment assignment = assignment(Instant.now().plus(1, ChronoUnit.DAYS));
        StoredFile oldFile = StoredFile.builder().id(500L).storageKey("old.pdf").build();
        Submission submission = Submission.builder().id(9000L).assignment(assignment).student(student)
                .storedFile(oldFile).status(SubmissionStatus.CORRECTED)
                .grade(new BigDecimal("15.00")).feedback("Bien").build();
        MockMultipartFile newFile = new MockMultipartFile("file", "v2.pdf", "application/pdf", "c".getBytes());

        when(submissionRepository.findById(9000L)).thenReturn(Optional.of(submission));
        when(courseAuthorizationService.resolveRequester("student@example.com")).thenReturn(student);
        when(fileStorageService.store(newFile)).thenReturn(
                new FileStorageService.StoredContent("v2.pdf", "new.pdf", "application/pdf", 1L, "def"));
        when(storedFileRepository.save(any(StoredFile.class))).thenAnswer(inv -> {
            StoredFile sf = inv.getArgument(0);
            sf.setId(501L);
            return sf;
        });
        when(submissionRepository.save(any(Submission.class))).thenAnswer(inv -> inv.getArgument(0));

        SubmissionDetail result = submissionService.replace(9000L, newFile, "student@example.com");

        assertThat(result.status()).isEqualTo("SUBMITTED");
        assertThat(result.grade()).isNull();
        assertThat(result.feedback()).isNull();
        verify(fileStorageService).delete("old.pdf");
        verify(storedFileRepository).delete(oldFile);
    }

    @Test
    void replace_throwsForbidden_whenRequesterIsNotOwner() {
        UserAccount owner = student();
        UserAccount someoneElse = UserAccount.builder().id(6L).email("other@example.com")
                .role(new Role(1L, "STUDENT", "Student")).build();
        Submission submission = Submission.builder().id(9000L).student(owner).assignment(assignment(null)).build();
        MockMultipartFile file = new MockMultipartFile("file", "v2.pdf", "application/pdf", "c".getBytes());

        when(submissionRepository.findById(9000L)).thenReturn(Optional.of(submission));
        when(courseAuthorizationService.resolveRequester("other@example.com")).thenReturn(someoneElse);

        assertThatThrownBy(() -> submissionService.replace(9000L, file, "other@example.com"))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    void replace_throwsDeadlinePassed_whenAssignmentDueDateInThePast() {
        UserAccount student = student();
        Assignment assignment = assignment(Instant.now().minus(1, ChronoUnit.HOURS));
        Submission submission = Submission.builder().id(9000L).student(student).assignment(assignment).build();
        MockMultipartFile file = new MockMultipartFile("file", "v2.pdf", "application/pdf", "c".getBytes());

        when(submissionRepository.findById(9000L)).thenReturn(Optional.of(submission));
        when(courseAuthorizationService.resolveRequester("student@example.com")).thenReturn(student);

        assertThatThrownBy(() -> submissionService.replace(9000L, file, "student@example.com"))
                .isInstanceOf(SubmissionDeadlinePassedException.class);
    }

    // ---- getSubmission() ----

    @Test
    void getSubmission_returnsIt_forOwningStudent() {
        UserAccount student = student();
        StoredFile storedFile = StoredFile.builder().id(500L).originalName("devoir.pdf").mimeType("application/pdf").build();
        Submission submission = Submission.builder().id(9000L).student(student).assignment(assignment(null))
                .storedFile(storedFile).status(SubmissionStatus.SUBMITTED).build();

        when(submissionRepository.findById(9000L)).thenReturn(Optional.of(submission));
        when(courseAuthorizationService.resolveRequester("student@example.com")).thenReturn(student);
        when(learningAccessService.isStudent(student)).thenReturn(true);

        SubmissionDetail result = submissionService.getSubmission(9000L, "student@example.com");

        assertThat(result.id()).isEqualTo(9000L);
    }

    @Test
    void getSubmission_throwsForbidden_forNonOwningStudent() {
        UserAccount owner = student();
        UserAccount someoneElse = UserAccount.builder().id(6L).email("other@example.com")
                .role(new Role(1L, "STUDENT", "Student")).build();
        Submission submission = Submission.builder().id(9000L).student(owner).assignment(assignment(null)).build();

        when(submissionRepository.findById(9000L)).thenReturn(Optional.of(submission));
        when(courseAuthorizationService.resolveRequester("other@example.com")).thenReturn(someoneElse);
        when(learningAccessService.isStudent(someoneElse)).thenReturn(true);

        assertThatThrownBy(() -> submissionService.getSubmission(9000L, "other@example.com"))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    void getSubmission_throwsSubmissionNotFound_whenIdUnknown() {
        when(submissionRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> submissionService.getSubmission(404L, "student@example.com"))
                .isInstanceOf(SubmissionNotFoundException.class);
    }

    // ---- correctSubmission() ----

    @Test
    void correctSubmission_setsGradeFeedbackAndStatus_whenAuthorized() {
        UserAccount trainer = trainer();
        StoredFile storedFile = StoredFile.builder().id(500L).originalName("devoir.pdf").mimeType("application/pdf").build();
        Submission submission = Submission.builder().id(9000L).student(student()).assignment(assignment(null))
                .storedFile(storedFile).status(SubmissionStatus.SUBMITTED).build();
        CorrectionRequest request = new CorrectionRequest(new BigDecimal("16.00"), "Très bon travail");

        when(submissionRepository.findById(9000L)).thenReturn(Optional.of(submission));
        when(courseAuthorizationService.resolveRequester("trainer1@example.com")).thenReturn(trainer);
        when(submissionRepository.save(any(Submission.class))).thenAnswer(inv -> inv.getArgument(0));

        SubmissionDetail result = submissionService.correctSubmission(9000L, request, "trainer1@example.com");

        assertThat(result.status()).isEqualTo("CORRECTED");
        assertThat(result.grade()).isEqualByComparingTo("16.00");
        assertThat(result.feedback()).isEqualTo("Très bon travail");
        assertThat(result.correctedByUserId()).isEqualTo(1L);
    }

    @Test
    void correctSubmission_propagatesForbidden_whenTrainerOutsidePerimeter() {
        UserAccount trainer = trainer();
        Submission submission = Submission.builder().id(9000L).student(student()).assignment(assignment(null)).build();
        CorrectionRequest request = new CorrectionRequest(null, "feedback");

        when(submissionRepository.findById(9000L)).thenReturn(Optional.of(submission));
        when(courseAuthorizationService.resolveRequester("trainer1@example.com")).thenReturn(trainer);
        doThrow(new ForbiddenActionException("nope"))
                .when(courseAuthorizationService).assertCanManageCourse(
                        submission.getAssignment().getLesson().getCourseModule().getCourse(), trainer);

        assertThatThrownBy(() -> submissionService.correctSubmission(9000L, request, "trainer1@example.com"))
                .isInstanceOf(ForbiddenActionException.class);
    }

    // ---- listSubmissions() ----

    @Test
    void listSubmissions_returnsAll_whenAuthorized() {
        UserAccount trainer = trainer();
        Assignment assignment = assignment(null);
        StoredFile storedFile = StoredFile.builder().id(500L).originalName("devoir.pdf").mimeType("application/pdf").build();
        Submission submission = Submission.builder().id(9000L).student(student()).assignment(assignment)
                .storedFile(storedFile).status(SubmissionStatus.SUBMITTED).build();

        when(assignmentRepository.findById(2000L)).thenReturn(Optional.of(assignment));
        when(courseAuthorizationService.resolveRequester("trainer1@example.com")).thenReturn(trainer);
        when(submissionRepository.findByAssignment_IdOrderBySubmittedAtDesc(2000L)).thenReturn(List.of(submission));

        List<SubmissionDetail> result = submissionService.listSubmissions(2000L, "trainer1@example.com");

        assertThat(result).hasSize(1);
    }
}
