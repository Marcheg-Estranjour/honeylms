package com.honeygroup.honeylms.course;

import com.honeygroup.honeylms.course.dto.CreateModuleRequest;
import com.honeygroup.honeylms.course.dto.ModuleDetail;
import com.honeygroup.honeylms.course.dto.UpdateModuleRequest;
import com.honeygroup.honeylms.user.UserAccount;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseModuleService {

    private final CourseModuleRepository courseModuleRepository;
    private final CourseRepository courseRepository;
    private final CourseAuthorizationService courseAuthorizationService;
    private final LearningAccessService learningAccessService;

    public CourseModuleService(CourseModuleRepository courseModuleRepository,
                                CourseRepository courseRepository,
                                CourseAuthorizationService courseAuthorizationService,
                                LearningAccessService learningAccessService) {
        this.courseModuleRepository = courseModuleRepository;
        this.courseRepository = courseRepository;
        this.courseAuthorizationService = courseAuthorizationService;
        this.learningAccessService = learningAccessService;
    }

    /**
     * US-LEARNING-01 — Create module. TRAINER (assigned to the Course) or ADMIN.
     */
    @Transactional
    public ModuleDetail createModule(Long courseId, CreateModuleRequest request, String requesterEmail) {
        Course course = findCourseOrThrow(courseId);
        UserAccount requester = courseAuthorizationService.resolveRequester(requesterEmail);
        courseAuthorizationService.assertCanManageCourse(course, requester);

        int nextOrder = (int) courseModuleRepository.countByCourse_Id(courseId) + 1;

        CourseModule module = CourseModule.builder()
                .course(course)
                .title(request.title().trim())
                .description(request.description())
                .displayOrder(nextOrder)
                .status(PublicationStatus.DRAFT)
                .build();

        return toDetail(courseModuleRepository.save(module));
    }

    /**
     * US-LEARNING-02 — Update module.
     */
    @Transactional
    public ModuleDetail updateModule(Long moduleId, UpdateModuleRequest request, String requesterEmail) {
        CourseModule module = findModuleOrThrow(moduleId);
        UserAccount requester = courseAuthorizationService.resolveRequester(requesterEmail);
        courseAuthorizationService.assertCanManageCourse(module.getCourse(), requester);

        module.setTitle(request.title().trim());
        module.setDescription(request.description());

        return toDetail(courseModuleRepository.save(module));
    }

    /**
     * US-LEARNING-03 — Publish module.
     */
    @Transactional
    public ModuleDetail publishModule(Long moduleId, String requesterEmail) {
        CourseModule module = findModuleOrThrow(moduleId);
        UserAccount requester = courseAuthorizationService.resolveRequester(requesterEmail);
        courseAuthorizationService.assertCanManageCourse(module.getCourse(), requester);

        module.setStatus(PublicationStatus.PUBLISHED);
        return toDetail(courseModuleRepository.save(module));
    }

    /**
     * Role-aware read. A STUDENT needs Enrollment + Course/Module PUBLISHED (US-LEARNING-07);
     * a TRAINER/ADMIN gets the management view (any status, within their perimeter).
     */
    @Transactional(readOnly = true)
    public ModuleDetail getModule(Long moduleId, String requesterEmail) {
        CourseModule module = findModuleOrThrow(moduleId);
        UserAccount requester = courseAuthorizationService.resolveRequester(requesterEmail);

        if (learningAccessService.isStudent(requester)) {
            learningAccessService.assertStudentCanAccessModule(module, requester);
        } else {
            courseAuthorizationService.assertCanManageCourse(module.getCourse(), requester);
        }
        return toDetail(module);
    }

    /**
     * Role-aware listing. A STUDENT only sees PUBLISHED modules of a Course they are
     * enrolled in; a TRAINER/ADMIN sees every module of a Course in their perimeter.
     */
    @Transactional(readOnly = true)
    public List<ModuleDetail> listModules(Long courseId, String requesterEmail) {
        Course course = findCourseOrThrow(courseId);
        UserAccount requester = courseAuthorizationService.resolveRequester(requesterEmail);
        List<CourseModule> modules = courseModuleRepository.findByCourse_IdOrderByDisplayOrderAsc(courseId);

        if (learningAccessService.isStudent(requester)) {
            learningAccessService.assertStudentCanAccessCourse(course, requester);
            modules = modules.stream()
                    .filter(m -> m.getStatus() == PublicationStatus.PUBLISHED)
                    .toList();
        } else {
            courseAuthorizationService.assertCanManageCourse(course, requester);
        }

        return modules.stream().map(this::toDetail).toList();
    }

    private Course findCourseOrThrow(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));
    }

    private CourseModule findModuleOrThrow(Long moduleId) {
        return courseModuleRepository.findById(moduleId)
                .orElseThrow(() -> new ModuleNotFoundException(moduleId));
    }

    private ModuleDetail toDetail(CourseModule module) {
        return new ModuleDetail(
                module.getId(),
                module.getCourse().getId(),
                module.getTitle(),
                module.getDescription(),
                module.getDisplayOrder(),
                module.getStatus().name()
        );
    }
}
