package com.honeygroup.honeylms.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.honeygroup.honeylms.common.ForbiddenActionException;
import com.honeygroup.honeylms.course.dto.ResourceDetail;
import com.honeygroup.honeylms.file.FileStorageService;
import com.honeygroup.honeylms.file.StoredFile;
import com.honeygroup.honeylms.file.StoredFileRepository;
import com.honeygroup.honeylms.user.Role;
import com.honeygroup.honeylms.user.UserAccount;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class ResourceServiceTest {

    @Mock
    private ResourceRepository resourceRepository;

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
    private ResourceService resourceService;

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
    void uploadResource_storesFileAndCreatesResource_whenAuthorized() {
        UserAccount trainer = trainer();
        Lesson lesson = lesson();
        MockMultipartFile file = new MockMultipartFile("file", "voc.pdf", "application/pdf", "content".getBytes());

        when(lessonRepository.findById(1000L)).thenReturn(Optional.of(lesson));
        when(courseAuthorizationService.resolveRequester("trainer1@example.com")).thenReturn(trainer);
        when(fileStorageService.store(file)).thenReturn(
                new FileStorageService.StoredContent("voc.pdf", "uuid.pdf", "application/pdf", 7L, "abc123"));
        when(storedFileRepository.save(any(StoredFile.class))).thenAnswer(inv -> {
            StoredFile sf = inv.getArgument(0);
            sf.setId(500L);
            return sf;
        });
        when(resourceRepository.countByLesson_Id(1000L)).thenReturn(0L);
        when(resourceRepository.save(any(Resource.class))).thenAnswer(inv -> {
            Resource r = inv.getArgument(0);
            r.setId(9000L);
            return r;
        });

        ResourceDetail result = resourceService.uploadResource(1000L, "Vocabulaire", file, "trainer1@example.com");

        assertThat(result.title()).isEqualTo("Vocabulaire");
        assertThat(result.displayOrder()).isEqualTo(1);
        assertThat(result.originalFileName()).isEqualTo("voc.pdf");
        verify(courseAuthorizationService).assertCanManageCourse(lesson.getCourseModule().getCourse(), trainer);
    }

    @Test
    void uploadResource_propagatesForbidden_whenPerimeterCheckFails() {
        UserAccount trainer = trainer();
        Lesson lesson = lesson();
        MockMultipartFile file = new MockMultipartFile("file", "voc.pdf", "application/pdf", "content".getBytes());

        when(lessonRepository.findById(1000L)).thenReturn(Optional.of(lesson));
        when(courseAuthorizationService.resolveRequester("trainer1@example.com")).thenReturn(trainer);
        doThrow(new ForbiddenActionException("nope"))
                .when(courseAuthorizationService).assertCanManageCourse(lesson.getCourseModule().getCourse(), trainer);

        assertThatThrownBy(() -> resourceService.uploadResource(1000L, "Vocabulaire", file, "trainer1@example.com"))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    void uploadResource_throwsLessonNotFound_whenLessonUnknown() {
        when(lessonRepository.findById(404L)).thenReturn(Optional.empty());
        MockMultipartFile file = new MockMultipartFile("file", "voc.pdf", "application/pdf", "content".getBytes());

        assertThatThrownBy(() -> resourceService.uploadResource(404L, "x", file, "trainer1@example.com"))
                .isInstanceOf(LessonNotFoundException.class);
    }

    @Test
    void deleteResource_deletesDbRowAndPhysicalFile_whenAuthorized() {
        UserAccount trainer = trainer();
        Lesson lesson = lesson();
        StoredFile storedFile = StoredFile.builder().id(500L).storageKey("uuid.pdf").originalName("voc.pdf").build();
        Resource resource = Resource.builder().id(9000L).lesson(lesson).storedFile(storedFile).title("Vocabulaire").displayOrder(1).build();

        when(resourceRepository.findById(9000L)).thenReturn(Optional.of(resource));
        when(courseAuthorizationService.resolveRequester("trainer1@example.com")).thenReturn(trainer);

        resourceService.deleteResource(9000L, "trainer1@example.com");

        verify(resourceRepository).delete(resource);
        verify(fileStorageService).delete("uuid.pdf");
        verify(storedFileRepository).delete(storedFile);
    }

    @Test
    void deleteResource_throwsResourceNotFound_whenIdUnknown() {
        when(resourceRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resourceService.deleteResource(404L, "trainer1@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
