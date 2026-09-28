package com.honeygroup.honeylms.course;

import com.honeygroup.honeylms.common.ForbiddenActionException;
import com.honeygroup.honeylms.user.RoleCode;
import com.honeygroup.honeylms.user.UserAccount;
import com.honeygroup.honeylms.user.UserAccountRepository;
import org.springframework.stereotype.Service;

/**
 * Central perimeter check reused by every Course-scoped resource: Course itself,
 * CourseModule, Lesson, and future Resource/Assignment. Extracted here rather than
 * duplicated in each service, so the rule "a Trainer must be assigned via
 * CourseTrainer to manage anything under a Course" lives in exactly one place.
 */
@Service
public class CourseAuthorizationService {

    private final CourseTrainerRepository courseTrainerRepository;
    private final UserAccountRepository userAccountRepository;

    public CourseAuthorizationService(CourseTrainerRepository courseTrainerRepository,
                                       UserAccountRepository userAccountRepository) {
        this.courseTrainerRepository = courseTrainerRepository;
        this.userAccountRepository = userAccountRepository;
    }

    public UserAccount resolveRequester(String email) {
        return userAccountRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated user not found in database: " + email));
    }

    /**
     * ADMIN always passes. A TRAINER must be explicitly assigned to the Course
     * via CourseTrainer - simply having role TRAINER is not enough.
     */
    public void assertCanManageCourse(Course course, UserAccount requester) {
        if (RoleCode.ADMIN.name().equals(requester.getRole().getCode())) {
            return;
        }

        boolean isAssignedTrainer = courseTrainerRepository
                .existsByCourse_IdAndTrainer_Id(course.getId(), requester.getId());

        if (!isAssignedTrainer) {
            throw new ForbiddenActionException("You are not allowed to manage this course");
        }
    }
}
