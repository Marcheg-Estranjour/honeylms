package com.honeygroup.honeylms.course;

import com.honeygroup.honeylms.course.dto.ResourceDetail;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class ResourceController {

    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    /** US-FILE-01 — Upload resource. TRAINER/ADMIN only (see SecurityConfig). */
    @PostMapping(path = "/api/lessons/{lessonId}/resources", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResourceDetail> uploadResource(@PathVariable Long lessonId,
                                                           @RequestParam String title,
                                                           @RequestPart MultipartFile file,
                                                           Authentication authentication) {
        ResourceDetail created = resourceService.uploadResource(lessonId, title, file, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/api/lessons/{lessonId}/resources")
    public ResponseEntity<List<ResourceDetail>> listResources(@PathVariable Long lessonId,
                                                                Authentication authentication) {
        return ResponseEntity.ok(resourceService.listResources(lessonId, authentication.getName()));
    }

    /** US-FILE-02 — Download resource. Role-aware (see ResourceService). */
    @GetMapping("/api/resources/{resourceId}/download")
    public ResponseEntity<org.springframework.core.io.Resource> downloadResource(
            @PathVariable Long resourceId, Authentication authentication) {
        ResourceService.DownloadableResource download =
                resourceService.downloadResource(resourceId, authentication.getName());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.mimeType()))
                .header("Content-Disposition", "attachment; filename=\"" + download.originalFileName() + "\"")
                .body(download.content());
    }

    /** US-FILE-03 — Delete resource. TRAINER/ADMIN only. */
    @DeleteMapping("/api/resources/{resourceId}")
    public ResponseEntity<Void> deleteResource(@PathVariable Long resourceId, Authentication authentication) {
        resourceService.deleteResource(resourceId, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
