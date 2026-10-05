package com.honeygroup.honeylms.progress;

import com.honeygroup.honeylms.course.Course;
import com.honeygroup.honeylms.course.CourseAuthorizationService;
import com.honeygroup.honeylms.course.CourseModule;
import com.honeygroup.honeylms.course.CourseModuleRepository;
import com.honeygroup.honeylms.course.CourseNotFoundException;
import com.honeygroup.honeylms.course.CourseRepository;
import com.honeygroup.honeylms.course.LearningAccessService;
import com.honeygroup.honeylms.course.Lesson;
import com.honeygroup.honeylms.course.LessonNotFoundException;
import com.honeygroup.honeylms.course.LessonRepository;
import com.honeygroup.honeylms.course.ModuleNotFoundException;
import com.honeygroup.honeylms.progress.dto.CourseProgress;
import com.honeygroup.honeylms.progress.dto.LessonCompletionDetail;
import com.honeygroup.honeylms.progress.dto.ModuleProgress;
import com.honeygroup.honeylms.progress.dto.ResumeResponse;
import com.honeygroup.honeylms.user.UserAccount;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProgressService {

    private final LessonCompletionRepository completionRepository;
    private final LessonRepository lessonRepository;
    private final CourseModuleRepository courseModuleRepository;
    private final CourseRepository courseRepository;
    private final CourseAuthorizationService courseAuthorizationService;
    private final LearningAccessService learningAccessService;

    public ProgressService(LessonCompletionRepository completionRepository,
                            LessonRepository lessonRepository,
                            CourseModuleRepository courseModuleRepository,
                            CourseRepository courseRepository,
                            CourseAuthorizationService courseAuthorizationService,
                            LearningAccessService learningAccessService) {
        this.completionRepository = completionRepository;
        this.lessonRepository = lessonRepository;
        this.courseModuleRepository = courseModuleRepository;
        this.courseRepository = courseRepository;
        this.courseAuthorizationService = courseAuthorizationService;
        this.learningAccessService = learningAccessService;
    }

    /** Records that the Student viewed this Lesson (used for "resume", US-PROGRESS-04). */
    @Transactional
    public LessonCompletionDetail recordView(Long lessonId, String studentEmail) {
        Lesson lesson = findLessonOrThrow(lessonId);
        UserAccount student = courseAuthorizationService.resolveRequester(studentEmail);
        learningAccessService.assertStudentCanAccessLesson(lesson, student);

        LessonCompletion completion = findOrCreate(student, lesson);
        completion.setLastViewedAt(Instant.now());
        return toDetail(completionRepository.save(completion));
    }

    /** US-PROGRESS-01 — Complete lesson. Also refreshes lastViewedAt. */
    @Transactional
    public LessonCompletionDetail markCompleted(Long lessonId, String studentEmail) {
        Lesson lesson = findLessonOrThrow(lessonId);
        UserAccount student = courseAuthorizationService.resolveRequester(studentEmail);
        learningAccessService.assertStudentCanAccessLesson(lesson, student);

        LessonCompletion completion = findOrCreate(student, lesson);
        Instant now = Instant.now();
        completion.setLastViewedAt(now);
        completion.setCompletedAt(now);
        return toDetail(completionRepository.save(completion));
    }

    /**
     * US-PROGRESS-02 — View course progress.
     * completed accessible lessons / total accessible lessons x 100 (grades never enter this).
     */
    @Transactional(readOnly = true)
    public CourseProgress getCourseProgress(Long courseId, String studentEmail) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));
        UserAccount student = courseAuthorizationService.resolveRequester(studentEmail);
        learningAccessService.assertStudentCanAccessCourse(course, student);

        List<Lesson> accessible = lessonRepository.findAccessibleLessonsByCourse(courseId);
        long completed = accessible.isEmpty() ? 0
                : completionRepository.countByStudent_IdAndLesson_InAndCompletedAtIsNotNull(student.getId(), accessible);

        return new CourseProgress(courseId, completed, accessible.size(), percentage(completed, accessible.size()));
    }

    /** US-PROGRESS-03 — View module progress. Same formula, scoped to one Module. */
    @Transactional(readOnly = true)
    public ModuleProgress getModuleProgress(Long moduleId, String studentEmail) {
        CourseModule module = courseModuleRepository.findById(moduleId)
                .orElseThrow(() -> new ModuleNotFoundException(moduleId));
        UserAccount student = courseAuthorizationService.resolveRequester(studentEmail);
        learningAccessService.assertStudentCanAccessModule(module, student);

        List<Lesson> accessible = lessonRepository.findAccessibleLessonsByModule(moduleId);
        long completed = accessible.isEmpty() ? 0
                : completionRepository.countByStudent_IdAndLesson_InAndCompletedAtIsNotNull(student.getId(), accessible);

        return new ModuleProgress(moduleId, completed, accessible.size(), percentage(completed, accessible.size()));
    }

    /**
     * US-PROGRESS-04 — Resume learning. Derived from LessonCompletion.lastViewedAt
     * alone - no dedicated "last lesson" column anywhere (Dossier de Conception §12).
     */
    @Transactional(readOnly = true)
    public ResumeResponse getResumePoint(Long courseId, String studentEmail) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));
        UserAccount student = courseAuthorizationService.resolveRequester(studentEmail);
        learningAccessService.assertStudentCanAccessCourse(course, student);

        List<LessonCompletion> mostRecent = completionRepository
                .findByStudent_IdAndLesson_CourseModule_Course_IdOrderByLastViewedAtDesc(
                        student.getId(), courseId, PageRequest.of(0, 1));

        if (mostRecent.isEmpty()) {
            throw new NoResumePointException(courseId);
        }

        LessonCompletion last = mostRecent.get(0);
        return new ResumeResponse(last.getLesson().getId(), last.getLesson().getCourseModule().getId(), last.getLastViewedAt());
    }

    // ---- helpers ----

    private LessonCompletion findOrCreate(UserAccount student, Lesson lesson) {
        return completionRepository.findByStudent_IdAndLesson_Id(student.getId(), lesson.getId())
                .orElseGet(() -> LessonCompletion.newFor(student, lesson));
    }

    private Lesson findLessonOrThrow(Long lessonId) {
        return lessonRepository.findById(lessonId)
                .orElseThrow(() -> new LessonNotFoundException(lessonId));
    }

    private int percentage(long completed, long total) {
        if (total == 0) {
            return 0;
        }
        return (int) Math.round(completed * 100.0 / total);
    }

    private LessonCompletionDetail toDetail(LessonCompletion completion) {
        return new LessonCompletionDetail(
                completion.getLesson().getId(),
                completion.getLastViewedAt(),
                completion.getCompletedAt()
        );
    }
}
