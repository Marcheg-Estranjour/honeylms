package com.honeygroup.honeylms.course;

import com.honeygroup.honeylms.user.RoleCode;
import com.honeygroup.honeylms.user.UserAccount;
import com.honeygroup.honeylms.user.UserAccountRepository;
import com.honeygroup.honeylms.user.UserNotFoundException;
import com.honeygroup.honeylms.user.dto.UserSummary;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Assignment of Trainers to Courses (CourseTrainer). VALIDÉ : only the Admin
 * creates courses, so only the Admin decides which Trainers manage which course.
 * Everything here is ADMIN-only (URL rule in SecurityConfig + assertAdmin).
 */
@Service
public class CourseTrainerService {

    private final CourseTrainerRepository courseTrainerRepository;
    private final CourseRepository courseRepository;
    private final UserAccountRepository userAccountRepository;
    private final CourseAuthorizationService courseAuthorizationService;

    public CourseTrainerService(CourseTrainerRepository courseTrainerRepository,
                                 CourseRepository courseRepository,
                                 UserAccountRepository userAccountRepository,
                                 CourseAuthorizationService courseAuthorizationService) {
        this.courseTrainerRepository = courseTrainerRepository;
        this.courseRepository = courseRepository;
        this.userAccountRepository = userAccountRepository;
        this.courseAuthorizationService = courseAuthorizationService;
    }

    /** US-ADMIN-07 — Assign a Trainer to a Course. */
    @Transactional
    public void assignTrainer(Long courseId, Long trainerId, String requesterEmail) {
        assertRequesterIsAdmin(requesterEmail);
        Course course = findCourseOrThrow(courseId);

        UserAccount trainer = userAccountRepository.findById(trainerId)
                .orElseThrow(() -> new UserNotFoundException(trainerId));
        if (!RoleCode.TRAINER.name().equals(trainer.getRole().getCode())) {
            throw new InvalidTrainerAssignmentException("User " + trainerId + " is not a Trainer");
        }
        if (courseTrainerRepository.existsByCourse_IdAndTrainer_Id(courseId, trainerId)) {
            throw new TrainerAlreadyAssignedException(courseId, trainerId);
        }

        courseTrainerRepository.save(CourseTrainer.of(course, trainer));
    }

    /**
     * Unassign a Trainer from a Course. Known limitation (documented, not silent):
     * if this Trainer was also added to a Class of this Course, his ClassTrainer row
     * stays. Authorization remains correct (he can no longer manage the Class, since
     * assertCanManageCourse now rejects him) - only the members listing still shows him.
     */
    @Transactional
    public void unassignTrainer(Long courseId, Long trainerId, String requesterEmail) {
        assertRequesterIsAdmin(requesterEmail);
        findCourseOrThrow(courseId);

        CourseTrainer assignment = courseTrainerRepository
                .findById(new CourseTrainerId(courseId, trainerId))
                .orElseThrow(() -> new TrainerAssignmentNotFoundException(courseId, trainerId));
        courseTrainerRepository.delete(assignment);
    }

    @Transactional(readOnly = true)
    public List<UserSummary> listTrainers(Long courseId, String requesterEmail) {
        assertRequesterIsAdmin(requesterEmail);
        findCourseOrThrow(courseId);

        return courseTrainerRepository.findByCourse_Id(courseId).stream()
                .map(ct -> ct.getTrainer())
                .map(t -> new UserSummary(t.getId(), t.getEmail(), t.getFirstName(), t.getLastName(),
                        t.getRole().getCode()))
                .toList();
    }

    private void assertRequesterIsAdmin(String requesterEmail) {
        courseAuthorizationService.assertAdmin(courseAuthorizationService.resolveRequester(requesterEmail));
    }

    private Course findCourseOrThrow(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));
    }
}
