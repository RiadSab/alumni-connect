package dev.sabti.alumni_connect.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

// Ties the byte storage (StorageService) to its DB metadata (StoredFile): one place that knows
// both must move together. Feature services (candidate, company, ...) depend on this rather than
// on StorageService directly, so they never deal with raw bytes or orphaned metadata.
@Service
@RequiredArgsConstructor
public class StoredFileService {
    private final StorageService storageService;
    private final StoredFileRepository repository;

    // Writes the bytes, then records the metadata keyed by the generated storageId. Content-type
    // / size validation is the caller's job (it's purpose-specific: PDF for a resume, image for a
    // logo), so this stays generic.
    @Transactional
    public StoredFile store(MultipartFile file) {
        String storageId = storageService.store(file);
        StoredFile metadata = new StoredFile();
        metadata.setStorageId(storageId);
        String original = file.getOriginalFilename();
        metadata.setOriginalFilename(original != null ? StringUtils.cleanPath(original) : storageId);
        metadata.setContentType(file.getContentType());
        metadata.setSize(file.getSize());
        return repository.save(metadata);
    }

    @Transactional(readOnly = true)
    public Optional<FileDownload> load(String storageId) {
        return repository.findByStorageId(storageId)
                .map(metadata -> new FileDownload(metadata, storageService.loadAsResource(storageId)));
    }

    // Removes both the bytes and the metadata row. Used when a file is replaced, so the old one
    // doesn't orphan on disk. No-op if the id is unknown.
    @Transactional
    public void delete(String storageId) {
        repository.findByStorageId(storageId).ifPresent(metadata -> {
            storageService.delete(storageId);
            repository.delete(metadata);
        });
    }
}
