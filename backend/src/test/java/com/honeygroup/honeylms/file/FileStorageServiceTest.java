package com.honeygroup.honeylms.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    private FileStorageService newService() {
        FileStorageProperties properties = new FileStorageProperties(
                tempDir.toString(), 52428800L, Set.of("pdf", "jpg", "png"));
        return new FileStorageService(properties);
    }

    @Test
    void store_savesFileAndComputesChecksum_whenValid() {
        FileStorageService service = newService();
        MockMultipartFile file = new MockMultipartFile(
                "file", "cours.pdf", "application/pdf", "contenu du cours".getBytes());

        FileStorageService.StoredContent result = service.store(file);

        assertThat(result.originalName()).isEqualTo("cours.pdf");
        assertThat(result.storageKey()).endsWith(".pdf");
        assertThat(result.sizeBytes()).isEqualTo(file.getSize());
        assertThat(result.checksum()).isNotBlank();
        assertThat(tempDir.resolve(result.storageKey())).exists();
    }

    @Test
    void store_throwsInvalidFile_whenExtensionNotAllowed() {
        FileStorageService service = newService();
        MockMultipartFile file = new MockMultipartFile(
                "file", "virus.exe", "application/octet-stream", "x".getBytes());

        assertThatThrownBy(() -> service.store(file))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("exe");
    }

    @Test
    void store_throwsInvalidFile_whenFileIsEmpty() {
        FileStorageService service = newService();
        MockMultipartFile file = new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);

        assertThatThrownBy(() -> service.store(file))
                .isInstanceOf(InvalidFileException.class);
    }

    @Test
    void store_throwsInvalidFile_whenExceedsMaxSize() {
        FileStorageProperties properties = new FileStorageProperties(tempDir.toString(), 10L, Set.of("pdf"));
        FileStorageService service = new FileStorageService(properties);
        MockMultipartFile file = new MockMultipartFile(
                "file", "big.pdf", "application/pdf", "this is more than ten bytes".getBytes());

        assertThatThrownBy(() -> service.store(file))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("size");
    }

    @Test
    void delete_doesNotThrow_whenFileDoesNotExist() {
        FileStorageService service = newService();

        service.delete("does-not-exist.pdf"); // no exception expected (best-effort)
    }
}
