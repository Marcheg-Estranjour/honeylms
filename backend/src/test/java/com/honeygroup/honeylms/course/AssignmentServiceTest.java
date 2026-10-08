package com.honeygroup.honeylms.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.honeygroup.honeylms.common.ForbiddenActionException;
import com.honeygroup.honeylms.course.dto.AssignmentDetail;
import com.honeygroup.honeylms.course.dto.CreateAssignmentRequest;
import com.honeygroup.honeylms.file.DownloadableFile;
import com.honeygroup.honeylms.file.FileStorageService;
import com.honeygroup.honeylms.file.StoredFile;
import com.honeygroup.honeylms.file.StoredFileRepository;
import com.honeygroup.honeylms.user.Role;
import com.honeygroup.honeylms.user.UserAccount;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class AssignmentServiceTest {

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private AssignmentFileRepository assignmentFileRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private StoredFileRepository storedFileRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private CourseAuthorizationService courseAuthorizationService;

    @Mock
    private LearningAccessService learningAccessService;

    @InjectMocks
    private AssignmentService assignmentService;

    private UserAccount trainer() {
        return UserAccount.builder().id(1L).email("trainer1@example.com")
                .role(new Role(2L, "TRAINER", "Trainer")).build();
    }

    private Lesson lesson() {
        Course course = Course.builder().id(10L).category(CourseCategory.LANGUAGES).build();
        CourseModule module = CourseModule.builder().id(100L).course(course).status(PublicationStatus.PUBLISHED).build();
        return Lesson.builder().id(1000L).courseModule(module).title("Greetings")
                .status(PublicationStatus.PUBLISHED).build();
    }

    @Test
    void createAssignment_savesAsDraft_whenAuthorized() {
        UserAccount trainer = trainer();
        Lesson lesson = lesson();
        CreateAssignmentRequest request = new CreateAssignmentRequest("Describe your family", "desc", null);

        when(lessonRepository.findById(1000L)).thenReturn(Optional.of(lesson));
        when(courseAuthorizationService.resolveRequester("trainer1@example.com")).thenReturn(trainer);
        when(assignmentRepository.save(any(Assignment.class))).thenAnswer(inv -> {
            Assignment a = inv.getArgument(0);
            a.setId(2000L);
            return a;
        });
        when(assignmentFileRepository.findByAssignment_Id(2000L)).thenReturn(List.of());

        AssignmentDetail result = assignmentService.createAssignment(1000L, request, "trainer1@example.com");

        assertThat(result.status()).isEqualTo("DRAFT");
        assertThat(result.title()).isEqualTo("Describe your family");
        assertThat(result.files()).isEmpty();
    }

    @Test
    void createAssignment_propagatesForbidden_whenPerimeterCheckFails() {
        UserAccount trainer = trainer();
        Lesson lesson = lesson();
        CreateAssignmentRequest request = new CreateAssignmentRequest("x", "y", null);

        when(lessonRepository.findById(1000L)).thenReturn(Optional.of(lesson));
        when(courseAuthorizationService.resolveRequester("trainer1@example.com")).thenReturn(trainer);
        doThrow(new ForbiddenActionException("nope"))
                .when(courseAuthorizationService).assertCanManageCourse(lesson.getCourseModule().getCourse(), trainer);

        assertThatThrownBy(() -> assignmentService.createAssignment(1000L, request, "trainer1@example.com"))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    void publishAssignment_setsStatusToPublished() {
        UserAccount trainer = trainer();
        Assignment assignment = Assignment.builder().id(2000L).lesson(lesson()).title("x")
                .status(PublicationStatus.DRAFT).build();

        when(assignmentRepository.findById(2000L)).thenReturn(Optional.of(assignment));
        when(courseAuthorizationService.resolveRequester("trainer1@example.com")).thenReturn(trainer);
        when(assignmentRepository.save(any(Assignment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(assignmentFileRepository.findByAssignment_Id(2000L)).thenReturn(List.of());

        AssignmentDetail result = assignmentService.publishAssignment(2000L, "trainer1@example.com");

        assertThat(result.status()).isEqualTo("PUBLISHED");
    }

    @Test
    void attachFile_addsFileToAssignment_whenAuthorized() {
        UserAccount trainer = trainer();
        Assignment assignment = Assignment.builder().id(2000L).lesson(lesson()).title("x")
                .status(PublicationStatus.DRAFT).build();
        MockMultipartFile file = new MockMultipartFile("file", "sujet.pdf", "application/pdf", "c".getBytes());
        StoredFile storedFile = StoredFile.builder().id(500L).originalName("sujet.pdf")
                .mimeType("application/pdf").sizeBytes(1L).build();

        when(assignmentRepository.findById(2000L)).thenReturn(Optional.of(assignment));
        when(courseAuthorizationService.resolveRequester("trainer1@example.com")).thenReturn(trainer);
        when(fileStorageService.store(file)).thenReturn(
                new FileStorageService.StoredContent("sujet.pdf", "uuid.pdf", "application/pdf", 1L, "abc"));
        when(storedFileRepository.save(any(StoredFile.class))).thenReturn(storedFile);
        when(assignmentFileRepository.findByAssignment_Id(2000L))
                .thenReturn(List.of(AssignmentFile.of(assignment, storedFile)));

        AssignmentDetail result = assignmentService.attachFile(2000L, file, "trainer1@example.com");

        assertThat(result.files()).hasSize(1);
        assertThat(result.files().get(0).originalName()).isEqualTo("sujet.pdf");
    }

    @Test
    void getAssignment_throwsAssignmentNotFound_whenIdUnknown() {
        when(assignmentRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assignmentService.getAssignment(404L, "trainer1@example.com"))
                .isInstanceOf(AssignmentNotFoundException.class);
    }

    @Test
    void getAssignment_forStudent_propagatesForbidden_whenNotPublished() {
        UserAccount student = UserAccount.builder().id(5L).email("student@example.com")
                .role(new Role(1L, "STUDENT", "Student")).build();
        Assignment assignment = Assignment.builder().id(2000L).lesson(lesson()).title("x")
                .status(PublicationStatus.DRAFT).build();

        when(assignmentRepository.findById(2000L)).thenReturn(Optional.of(assignment));
        when(courseAuthorizationService.resolveRequester("student@example.com")).thenReturn(student);
        when(learningAccessService.isStudent(student)).thenReturn(true);
        doThrow(new ForbiddenActionException("not published"))
                .when(learningAccessService).assertStudentCanAccessAssignment(assignment, student);

        assertThatThrownBy(() -> assignmentService.getAssignment(2000L, "student@example.com"))
                .isInstanceOf(ForbiddenActionException.class);
    }

    // ---- Gap G4: download a file attached to an assignment ----

    private UserAccount student() {
        return UserAccount.builder().id(5L).email("student@example.com")
                .role(new Role(1L, "STUDENT", "Student")).build();
    }

    private Assignment publishedAssignment() {
        return Assignment.builder().id(2000L).lesson(lesson()).title("Devoir")
                .status(PublicationStatus.PUBLISHED).build();
    }

    @Test
    void downloadAttachedFile_returnsTheFile_forAnEnrolledStudent() {
        UserAccount student = student();
        Assignment assignment = publishedAssignment();
        StoredFile file = StoredFile.builder().id(700L).originalName("consigne.docx").storageKey("uuid.docx")
                .mimeType("application/vnd.openxmlformats-officedocument.wordprocessingml.document").sizeBytes(3L).build();

        when(assignmentRepository.findById(2000L)).thenReturn(Optional.of(assignment));
        when(courseAuthorizationService.resolveRequester("student@example.com")).thenReturn(student);
        when(learningAccessService.isStudent(student)).thenReturn(true);
        when(assignmentFileRepository.findByAssignment_IdAndStoredFile_Id(2000L, 700L))
                .thenReturn(Optional.of(AssignmentFile.of(assignment, file)));
        when(fileStorageService.load("uuid.docx")).thenReturn(new ByteArrayResource("x".getBytes()));

        DownloadableFile result = assignmentService.downloadAttachedFile(2000L, 700L, "student@example.com");

        assertThat(result.originalFileName()).isEqualTo("consigne.docx");
    }

    @Test
    void downloadAttachedFile_throwsNotFound_whenTheFileBelongsToAnotherAssignment() {
        UserAccount student = student();
        Assignment assignment = publishedAssignment();

        when(assignmentRepository.findById(2000L)).thenReturn(Optional.of(assignment));
        when(courseAuthorizationService.resolveRequester("student@example.com")).thenReturn(student);
        when(learningAccessService.isStudent(student)).thenReturn(true);
        when(assignmentFileRepository.findByAssignment_IdAndStoredFile_Id(2000L, 999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assignmentService.downloadAttachedFile(2000L, 999L, "student@example.com"))
                .isInstanceOf(AssignmentFileNotFoundException.class);
    }

    @Test
    void downloadAttachedFile_propagatesForbidden_whenStudentHasNoAccess() {
        UserAccount student = student();
        Assignment assignment = publishedAssignment();

        when(assignmentRepository.findById(2000L)).thenReturn(Optional.of(assignment));
        when(courseAuthorizationService.resolveRequester("student@example.com")).thenReturn(student);
        when(learningAccessService.isStudent(student)).thenReturn(true);
        doThrow(new ForbiddenActionException("not enrolled"))
                .when(learningAccessService).assertStudentCanAccessAssignment(assignment, student);

        assertThatThrownBy(() -> assignmentService.downloadAttachedFile(2000L, 700L, "student@example.com"))
                .isInstanceOf(ForbiddenActionException.class);
    }
}
