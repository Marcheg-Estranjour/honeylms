package com.honeygroup.honeylms.file;

import java.nio.charset.StandardCharsets;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * A stored file ready to be sent to the client (content + original name + MIME type).
 * Shared by every download endpoint (resources, assignment files, submissions).
 */
public record DownloadableFile(Resource content, String originalFileName, String mimeType) {

    /**
     * 200 response with the file as an attachment.
     * The Content-Disposition header is built by Spring (RFC 6266 / RFC 5987 encoding):
     * quotes, line breaks or non-ASCII characters in the original file name can neither
     * break the header nor inject another one.
     */
    public ResponseEntity<Resource> toAttachmentResponse() {
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(originalFileName, StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mimeType))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(content);
    }
}
