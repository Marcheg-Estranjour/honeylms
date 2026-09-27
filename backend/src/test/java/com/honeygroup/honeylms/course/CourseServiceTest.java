package com.honeygroup.honeylms.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.honeygroup.honeylms.course.dto.CourseSummary;
import com.honeygroup.honeylms.user.Role;
import com.honeygroup.honeylms.user.UserAccount;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private CourseService courseService;

    private Course course(String title, CourseCategory category) {
        UserAccount trainer = UserAccount.builder()
                .id(1L)
                .email("trainer@example.com")
                .role(new Role(2L, "TRAINER", "Trainer"))
                .build();

        return Course.builder()
                .id(10L)
                .title(title)
                .description("desc")
                .status(PublicationStatus.PUBLISHED)
                .category(category)
                .createdBy(trainer)
                .build();
    }

    @Test
    void getPublishedCourses_returnsAllPublished_whenNoCategoryGiven() {
        when(courseRepository.findByStatus(PublicationStatus.PUBLISHED))
                .thenReturn(List.of(course("Anglais", CourseCategory.LANGUAGES)));

        List<CourseSummary> result = courseService.getPublishedCourses(Optional.empty());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("Anglais");
        assertThat(result.get(0).category()).isEqualTo("LANGUAGES");
        verify(courseRepository).findByStatus(PublicationStatus.PUBLISHED);
        verifyNoMoreInteractions(courseRepository);
    }

    @Test
    void getPublishedCourses_filtersByCategory_whenCategoryGiven() {
        when(courseRepository.findByStatusAndCategory(PublicationStatus.PUBLISHED, CourseCategory.OFFICE_AUTOMATION))
                .thenReturn(List.of(course("Excel", CourseCategory.OFFICE_AUTOMATION)));

        List<CourseSummary> result = courseService.getPublishedCourses(Optional.of(CourseCategory.OFFICE_AUTOMATION));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("Excel");
        verify(courseRepository).findByStatusAndCategory(PublicationStatus.PUBLISHED, CourseCategory.OFFICE_AUTOMATION);
        verifyNoMoreInteractions(courseRepository);
    }
}
