package com.honeygroup.honeylms.course;

import com.honeygroup.honeylms.course.dto.ResourceDetail;
import com.honeygroup.honeylms.file.FileStorageService;
import com.honeygroup.honeylms.file.StoredFile;
import com.honeygroup.honeylms.file.StoredFileRepository;
import com.honeygroup.honeylms.user.UserAccount;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ResourceService {

    private final ResourceRepository resourceRepository;
    private final LessonRepository lessonRepository;
    private final StoredFileRepository storedFileRepository;
    private final FileStorageService fileStorageService;
    private final CourseAuthorizationService courseAuthorizationService;
    private final LearningAccessService learningAccessService;

    public ResourceService(ResourceRepository resourceRepository,
                            LessonRepository lessonRepository,
                            StoredFileRepository storedFileRepository,
                            FileStorageService fileStorageService,
                            CourseAuthorizationService courseAuthorizationService,
                            LearningAccessService learningAccessService) {
        this.resourceRepository = resourceRepository;
        this.lessonRepository = lessonRepository;
        this.storedFileRepository = storedFileRepository;
        this.fileStorageService = fileStorageService;
        this.courseAuthorizationService = courseAuthorizationService;
        this.learningAccessService = learningAccessService;
    }

    /**
     * US-FILE-01 — Upload resource. TRAINER (own perimeter) or ADMIN.
     */
    @Transactional
    public ResourceDetail uploadResource(Long lessonId, String title, MultipartFile file, String requesterEmail) {
        Lesson lesson = findLessonOrThrow(lessonId);
        UserAccount requester = courseAuthorizationService.resolveRequester(requesterEmail);
        courseAuthorizationService.assertCanManageCourse(lesson.getCourseModule().getCourse(), requester);

        FileStorageService.StoredContent content = fileStorageService.store(file);

        StoredFile storedFile = storedFileRepository.save(StoredFile.builder()
                .originalName(content.originalName())
                .storageKey(content.storageKey())
                .mimeType(content.mimeType() != null ? content.mimeType() : "application/octet-stream")
                .sizeBytes(content.sizeBytes())
                .checksum(content.checksum())
                .createdBy(requester)
                .build());

        int nextOrder = (int) resourceRepository.countByLesson_Id(lessonId) + 1;

        com.honeygroup.honeylms.course.Resource resource = com.honeygroup.honeylms.course.Resource.builder()
                .lesson(lesson)
                .storedFile(storedFile)
                .title(title.trim())
                .displayOrder(nextOrder)
                .build();

        return toDetail(resourceRepository.save(resource));
    }

    /**
     * US-FILE-02 — Download resource. Role-aware: Trainer/Admin of the perimeter,
     * or a Student with access to the Lesson this resource belongs to.
     */
    @Transactional(readOnly = true)
    public DownloadableResource downloadResource(Long resourceId, String requesterEmail) {
        com.honeygroup.honeylms.course.Resource resource = findResourceOrThrow(resourceId);
        UserAccount requester = courseAuthorizationService.resolveRequester(requesterEmail);
        assertCanAccess(resource.getLesson(), requester);

        StoredFile storedFile = resource.getStoredFile();
        Resource content = fileStorageService.load(storedFile.getStorageKey());
        return new DownloadableResource(content, storedFile.getOriginalName(), storedFile.getMimeType());
    }

    /**
     * US-FILE-03 — Delete resource. TRAINER (own perimeter) or ADMIN.
     * The StoredFile row and the physical file are left in place if referenced
     * elsewhere is not a current concern (a StoredFile only ever backs one Resource
     * for the MVP) - deleted alongside for simplicity.
     */
    @Transactional
    public void deleteResource(Long resourceId, String requesterEmail) {
        com.honeygroup.honeylms.course.Resource resource = findResourceOrThrow(resourceId);
        UserAccount requester = courseAuthorizationService.resolveRequester(requesterEmail);
        courseAuthorizationService.assertCanManageCourse(resource.getLesson().getCourseModule().getCourse(), requester);

        StoredFile storedFile = resource.getStoredFile();
        resourceRepository.delete(resource);
        fileStorageService.delete(storedFile.getStorageKey());
        storedFileRepository.delete(storedFile);
    }

    @Transactional(readOnly = true)
    public List<ResourceDetail> listResources(Long lessonId, String requesterEmail) {
        Lesson lesson = findLessonOrThrow(lessonId);
        UserAccount requester = courseAuthorizationService.resolveRequester(requesterEmail);
        assertCanAccess(lesson, requester);

        return resourceRepository.findByLesson_IdOrderByDisplayOrderAsc(lessonId).stream()
                .map(this::toDetail)
                .toList();
    }

    private void assertCanAccess(Lesson lesson, UserAccount requester) {
        if (learningAccessService.isStudent(requester)) {
            learningAccessService.assertStudentCanAccessLesson(lesson, requester);
        } else {
            courseAuthorizationService.assertCanManageCourse(lesson.getCourseModule().getCourse(), requester);
        }
    }

    private Lesson findLessonOrThrow(Long lessonId) {
        return lessonRepository.findById(lessonId)
                .orElseThrow(() -> new LessonNotFoundException(lessonId));
    }

    private com.honeygroup.honeylms.course.Resource findResourceOrThrow(Long resourceId) {
        return resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException(resourceId));
    }

    private ResourceDetail toDetail(com.honeygroup.honeylms.course.Resource resource) {
        return new ResourceDetail(
                resource.getId(),
                resource.getLesson().getId(),
                resource.getTitle(),
                resource.getDisplayOrder(),
                resource.getStoredFile().getOriginalName(),
                resource.getStoredFile().getMimeType(),
                resource.getStoredFile().getSizeBytes()
        );
    }

    public record DownloadableResource(Resource content, String originalFileName, String mimeType) {
    }
}
