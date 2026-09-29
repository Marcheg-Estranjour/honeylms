package com.honeygroup.honeylms.file;

import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bound from `app.storage.*`. See Dossier de Conception §12.5 : local disk
 * storage for the MVP, revisited later if S3/MinIO is ever needed.
 */
@ConfigurationProperties(prefix = "app.storage")
public record FileStorageProperties(String location, long maxSizeBytes, Set<String> allowedExtensions) {
}
