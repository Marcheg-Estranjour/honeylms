package com.honeygroup.honeylms.course;

import com.honeygroup.honeylms.course.dto.CreateLessonRequest;
import com.honeygroup.honeylms.course.dto.LessonDetail;
import com.honeygroup.honeylms.course.dto.UpdateLessonRequest;
import com.honeygroup.honeylms.user.UserAccount;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LessonService {

    private final LessonRepository lessonRepository;
    private final CourseModuleRepository courseModuleRepository;
    private final CourseAuthorizationService courseAuthorizationService;
    private final LearningAccessService learningAccessService;

    public LessonService(LessonRepository lessonRepository,
                          CourseModuleRepository courseModuleRepository,
                          CourseAuthorizationService courseAuthorizationService,
                          LearningAccessService learningAccessService) {
        this.lessonRepository = lessonRepository;
        this.courseModuleRepository = courseModuleRepository;
        this.courseAuthorizationService = courseAuthorizationService;
        this.learningAccessService = learningAccessService;
    }

    /**
     * US-LEARNING-04 — Create lesson. Authorization is checked against the
     * Lesson's Module's Course - same CourseAuthorizationService as everywhere else.
     */
    @Transactional
    public LessonDetail createLesson(Long moduleId, CreateLessonRequest request, String requesterEmail) {
        CourseModule module = findModuleOrThrow(moduleId);
        UserAccount requester = courseAuthorizationService.resolveRequester(requesterEmail);
        courseAuthorizationService.assertCanManageCourse(module.getCourse(), requester);

        int nextOrder = (int) lessonRepository.countByCourseModule_Id(moduleId) + 1;

        Lesson lesson = Lesson.builder()
                .courseModule(module)
                .title(request.title().trim())
                .description(request.description())
                .content(request.content())
                .displayOrder(nextOrder)
                .status(PublicationStatus.DRAFT)
                .build();

        return toDetail(lessonRepository.save(lesson));
    }

    /**
     * US-LEARNING-05 — Update lesson.
     */
    @Transactional
    public LessonDetail updateLesson(Long lessonId, UpdateLessonRequest request, String requesterEmail) {
        Lesson lesson = findLessonOrThrow(lessonId);
        UserAccount requester = courseAuthorizationService.resolveRequester(requesterEmail);
        courseAuthorizationService.assertCanManageCourse(lesson.getCourseModule().getCourse(), requester);

        lesson.setTitle(request.title().trim());
        lesson.setDescription(request.description());
        lesson.setContent(request.content());

        return toDetail(lessonRepository.save(lesson));
    }

    /**
     * US-LEARNING-06 — Publish lesson.
     */
    @Transactional
    public LessonDetail publishLesson(Long lessonId, String requesterEmail) {
        Lesson lesson = findLessonOrThrow(lessonId);
        UserAccount requester = courseAuthorizationService.resolveRequester(requesterEmail);
        courseAuthorizationService.assertCanManageCourse(lesson.getCourseModule().getCourse(), requester);

        lesson.setStatus(PublicationStatus.PUBLISHED);
        return toDetail(lessonRepository.save(lesson));
    }

    /**
     * US-LEARNING-07 — Access lesson. Role-aware: a STUDENT needs Enrollment and
     * Course + Module + Lesson all PUBLISHED (TrainingClass never counts); a
     * TRAINER/ADMIN gets the management view (any status, within their perimeter).
     */
    @Transactional(readOnly = true)
    public LessonDetail getLesson(Long lessonId, String requesterEmail) {
        Lesson lesson = findLessonOrThrow(lessonId);
        UserAccount requester = courseAuthorizationService.resolveRequester(requesterEmail);

        if (learningAccessService.isStudent(requester)) {
            learningAccessService.assertStudentCanAccessLesson(lesson, requester);
        } else {
            courseAuthorizationService.assertCanManageCourse(lesson.getCourseModule().getCourse(), requester);
        }
        return toDetail(lesson);
    }

    /**
     * Role-aware listing. A STUDENT only sees PUBLISHED lessons of a PUBLISHED module
     * of a Course they are enrolled in.
     */
    @Transactional(readOnly = true)
    public List<LessonDetail> listLessons(Long moduleId, String requesterEmail) {
        CourseModule module = findModuleOrThrow(moduleId);
        UserAccount requester = courseAuthorizationService.resolveRequester(requesterEmail);
        List<Lesson> lessons = lessonRepository.findByCourseModule_IdOrderByDisplayOrderAsc(moduleId);

        if (learningAccessService.isStudent(requester)) {
            learningAccessService.assertStudentCanAccessModule(module, requester);
            lessons = lessons.stream()
                    .filter(l -> l.getStatus() == PublicationStatus.PUBLISHED)
                    .toList();
        } else {
            courseAuthorizationService.assertCanManageCourse(module.getCourse(), requester);
        }

        return lessons.stream().map(this::toDetail).toList();
    }

    private CourseModule findModuleOrThrow(Long moduleId) {
        return courseModuleRepository.findById(moduleId)
                .orElseThrow(() -> new ModuleNotFoundException(moduleId));
    }

    private Lesson findLessonOrThrow(Long lessonId) {
        return lessonRepository.findById(lessonId)
                .orElseThrow(() -> new LessonNotFoundException(lessonId));
    }

    private LessonDetail toDetail(Lesson lesson) {
        return new LessonDetail(
                lesson.getId(),
                lesson.getCourseModule().getId(),
                lesson.getTitle(),
                lesson.getDescription(),
                lesson.getContent(),
                lesson.getDisplayOrder(),
                lesson.getStatus().name()
        );
    }
}
