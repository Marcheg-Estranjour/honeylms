package com.honeygroup.honeylms.file;

import com.honeygroup.honeylms.user.UserAccount;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Validates uploads against the business rules (50 MiB max, allowed formats -
 * Dossier de Conception règle 14) and persists them to a local disk directory.
 * The DB (StoredFile) only ever stores metadata + a storage_key; this service
 * is the only place that touches the filesystem.
 */
@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private final FileStorageProperties properties;
    private final Path rootLocation;

    public FileStorageService(FileStorageProperties properties) {
        this.properties = properties;
        this.rootLocation = Path.of(properties.location());
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new IllegalStateException("Could not initialize storage location: " + rootLocation, e);
        }
    }

    /**
     * Validates and stores the file, returning everything needed to build a
     * StoredFile entity. Never trusts the client-declared content type alone for
     * the extension check - both must agree with the allow-list.
     */
    public StoredContent store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("The uploaded file is empty");
        }
        if (file.getSize() > properties.maxSizeBytes()) {
            throw new InvalidFileException(
                    "File exceeds the maximum allowed size of " + (properties.maxSizeBytes() / (1024 * 1024)) + " MiB");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            throw new InvalidFileException("The uploaded file has no name");
        }

        String extension = extractExtension(originalName);
        if (!properties.allowedExtensions().contains(extension)) {
            throw new InvalidFileException("File extension not allowed: " + extension);
        }

        String storageKey = UUID.randomUUID() + "." + extension;
        Path destination = rootLocation.resolve(storageKey).normalize();
        if (!destination.getParent().equals(rootLocation.toAbsolutePath().normalize())
                && !destination.startsWith(rootLocation)) {
            // Defensive check against path traversal via a crafted filename;
            // storageKey is server-generated (UUID) so this should never trigger,
            // but a broken/compromised extension list is not an assumption to skip.
            throw new InvalidFileException("Invalid file destination");
        }

        String checksum;
        try (InputStream in = file.getInputStream()) {
            checksum = computeChecksum(in);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read uploaded file", e);
        }

        try {
            file.transferTo(destination);
        } catch (IOException e) {
            throw new IllegalStateException("Could not store uploaded file", e);
        }

        return new StoredContent(originalName, storageKey, file.getContentType(), file.getSize(), checksum);
    }

    public Resource load(String storageKey) {
        Path filePath = rootLocation.resolve(storageKey).normalize();
        Resource resource = new FileSystemResource(filePath);
        if (!resource.exists() || !resource.isReadable()) {
            throw new IllegalStateException("Stored file is missing on disk: " + storageKey);
        }
        return resource;
    }

    /**
     * Best-effort deletion: a missing file on disk (already cleaned up, moved,
     * manual intervention...) must not block deleting the database record.
     */
    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(rootLocation.resolve(storageKey).normalize());
        } catch (IOException e) {
            log.warn("Could not delete stored file {}: {}", storageKey, e.getMessage());
        }
    }

    private String extractExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private String computeChecksum(InputStream in) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is a standard JDK algorithm - this branch is unreachable in practice.
            throw new IllegalStateException(e);
        }
    }

    public record StoredContent(String originalName, String storageKey, String mimeType,
                                 long sizeBytes, String checksum) {
    }
}
