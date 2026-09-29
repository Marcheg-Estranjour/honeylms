package com.honeygroup.honeylms.trainingclass;

import com.honeygroup.honeylms.course.Course;
import com.honeygroup.honeylms.course.CourseAuthorizationService;
import com.honeygroup.honeylms.course.CourseNotFoundException;
import com.honeygroup.honeylms.course.CourseRepository;
import com.honeygroup.honeylms.course.CourseTrainerRepository;
import com.honeygroup.honeylms.trainingclass.dto.ClassMemberSummary;
import com.honeygroup.honeylms.trainingclass.dto.ClassMembersResponse;
import com.honeygroup.honeylms.trainingclass.dto.CreateClassRequest;
import com.honeygroup.honeylms.trainingclass.dto.TrainingClassDetail;
import com.honeygroup.honeylms.user.RoleCode;
import com.honeygroup.honeylms.user.UserAccount;
import com.honeygroup.honeylms.user.UserAccountRepository;
import com.honeygroup.honeylms.user.UserNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TrainingClassService {

    private final TrainingClassRepository trainingClassRepository;
    private final ClassStudentRepository classStudentRepository;
    private final ClassTrainerRepository classTrainerRepository;
    private final CourseRepository courseRepository;
    private final CourseTrainerRepository courseTrainerRepository;
    private final UserAccountRepository userAccountRepository;
    private final CourseAuthorizationService courseAuthorizationService;

    public TrainingClassService(TrainingClassRepository trainingClassRepository,
                                 ClassStudentRepository classStudentRepository,
                                 ClassTrainerRepository classTrainerRepository,
                                 CourseRepository courseRepository,
                                 CourseTrainerRepository courseTrainerRepository,
                                 UserAccountRepository userAccountRepository,
                                 CourseAuthorizationService courseAuthorizationService) {
        this.trainingClassRepository = trainingClassRepository;
        this.classStudentRepository = classStudentRepository;
        this.classTrainerRepository = classTrainerRepository;
        this.courseRepository = courseRepository;
        this.courseTrainerRepository = courseTrainerRepository;
        this.userAccountRepository = userAccountRepository;
        this.courseAuthorizationService = courseAuthorizationService;
    }

    /** US-CLASS-01 — Create class. TRAINER (own perimeter) or ADMIN. */
    @Transactional
    public TrainingClassDetail createClass(Long courseId, CreateClassRequest request, String requesterEmail) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));
        UserAccount requester = courseAuthorizationService.resolveRequester(requesterEmail);
        courseAuthorizationService.assertCanManageCourse(course, requester);

        TrainingClass trainingClass = TrainingClass.builder()
                .course(course)
                .name(request.name().trim())
                .description(request.description())
                .build();

        return toDetail(trainingClassRepository.save(trainingClass));
    }

    /**
     * US-CLASS-02 — Add student.
     * HYPOTHÈSE RETENUE (option la plus simple) : le Student n'a pas besoin d'être
     * inscrit (Enrollment) au Course de la Class pour y être ajouté - le Dossier de
     * Conception présente cette règle comme nécessitant une vérification métier
     * supplémentaire, non fermement validée ; on ne l'impose donc pas pour le MVP.
     */
    @Transactional
    public void addStudent(Long classId, Long studentId, String requesterEmail) {
        TrainingClass trainingClass = findClassOrThrow(classId);
        UserAccount requester = courseAuthorizationService.resolveRequester(requesterEmail);
        courseAuthorizationService.assertCanManageCourse(trainingClass.getCourse(), requester);

        UserAccount student = findUserOrThrow(studentId);
        if (!RoleCode.STUDENT.name().equals(student.getRole().getCode())) {
            throw new InvalidClassAssignmentException("User " + studentId + " is not a Student");
        }
        if (classStudentRepository.existsByTrainingClass_IdAndStudent_Id(classId, studentId)) {
            throw new AlreadyClassMemberException(classId, studentId);
        }

        classStudentRepository.save(ClassStudent.of(trainingClass, student));
    }

    /** US-CLASS-03 — Remove student. */
    @Transactional
    public void removeStudent(Long classId, Long studentId, String requesterEmail) {
        TrainingClass trainingClass = findClassOrThrow(classId);
        UserAccount requester = courseAuthorizationService.resolveRequester(requesterEmail);
        courseAuthorizationService.assertCanManageCourse(trainingClass.getCourse(), requester);

        ClassStudent membership = classStudentRepository.findByTrainingClass_IdAndStudent_Id(classId, studentId)
                .orElseThrow(() -> new ClassMembershipNotFoundException(classId, studentId));
        classStudentRepository.delete(membership);
    }

    /**
     * US-CLASS-04 — Add trainer. VALIDÉ : the Trainer must already be a CourseTrainer
     * of this Class's Course (Dossier de Conception §6 - intégrité ClassTrainer).
     */
    @Transactional
    public void addTrainer(Long classId, Long trainerId, String requesterEmail) {
        TrainingClass trainingClass = findClassOrThrow(classId);
        UserAccount requester = courseAuthorizationService.resolveRequester(requesterEmail);
        courseAuthorizationService.assertCanManageCourse(trainingClass.getCourse(), requester);

        UserAccount trainer = findUserOrThrow(trainerId);
        if (!RoleCode.TRAINER.name().equals(trainer.getRole().getCode())) {
            throw new InvalidClassAssignmentException("User " + trainerId + " is not a Trainer");
        }
        boolean inCoursePerimeter = courseTrainerRepository
                .existsByCourse_IdAndTrainer_Id(trainingClass.getCourse().getId(), trainerId);
        if (!inCoursePerimeter) {
            throw new InvalidClassAssignmentException(
                    "Trainer " + trainerId + " must first be assigned to course "
                            + trainingClass.getCourse().getId() + " before joining this class");
        }
        if (classTrainerRepository.existsByTrainingClass_IdAndTrainer_Id(classId, trainerId)) {
            throw new AlreadyClassMemberException(classId, trainerId);
        }

        classTrainerRepository.save(ClassTrainer.of(trainingClass, trainer));
    }

    /**
     * Remove trainer - symmetric with removeStudent. Not one of the 5 numbered
     * US-CLASS stories, but already part of the published API contract (DELETE
     * /api/classes/{id}/trainers/{trainerId}), so included for consistency.
     */
    @Transactional
    public void removeTrainer(Long classId, Long trainerId, String requesterEmail) {
        TrainingClass trainingClass = findClassOrThrow(classId);
        UserAccount requester = courseAuthorizationService.resolveRequester(requesterEmail);
        courseAuthorizationService.assertCanManageCourse(trainingClass.getCourse(), requester);

        ClassTrainer membership = classTrainerRepository.findByTrainingClass_IdAndTrainer_Id(classId, trainerId)
                .orElseThrow(() -> new ClassMembershipNotFoundException(classId, trainerId));
        classTrainerRepository.delete(membership);
    }

    /**
     * US-CLASS-05 — View class members. Reminder: this membership never implies
     * pedagogical access (Enrollment is the only source of that).
     */
    @Transactional(readOnly = true)
    public ClassMembersResponse getMembers(Long classId, String requesterEmail) {
        TrainingClass trainingClass = findClassOrThrow(classId);
        UserAccount requester = courseAuthorizationService.resolveRequester(requesterEmail);
        courseAuthorizationService.assertCanManageCourse(trainingClass.getCourse(), requester);

        List<ClassMemberSummary> students = classStudentRepository.findByTrainingClass_Id(classId).stream()
                .map(cs -> toSummary(cs.getStudent()))
                .toList();
        List<ClassMemberSummary> trainers = classTrainerRepository.findByTrainingClass_Id(classId).stream()
                .map(ct -> toSummary(ct.getTrainer()))
                .toList();

        return new ClassMembersResponse(students, trainers);
    }

    private TrainingClass findClassOrThrow(Long classId) {
        return trainingClassRepository.findById(classId)
                .orElseThrow(() -> new TrainingClassNotFoundException(classId));
    }

    private UserAccount findUserOrThrow(Long userId) {
        return userAccountRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    private ClassMemberSummary toSummary(UserAccount account) {
        return new ClassMemberSummary(account.getId(), account.getFirstName(), account.getLastName(), account.getEmail());
    }

    private TrainingClassDetail toDetail(TrainingClass trainingClass) {
        return new TrainingClassDetail(
                trainingClass.getId(),
                trainingClass.getCourse().getId(),
                trainingClass.getName(),
                trainingClass.getDescription()
        );
    }
}
