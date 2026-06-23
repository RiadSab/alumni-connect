package dev.sabti.alumni_connect.storage;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {

    private final Path root;

    public LocalStorageService(@Value("${storage.local.dir}") String dir) {
        this.root = Paths.get(dir).toAbsolutePath().normalize();
    }

    @PostConstruct
    void init() {
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new StorageException("Could not initialise storage directory: " + root, e);
        }
    }

    @Override
    public String store(MultipartFile file) {
        String storageId = UUID.randomUUID().toString();
        Path destination = resolveWithinRoot(storageId);
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new StorageException("Failed to store file " + storageId, e);
        }
        return storageId;
    }

    @Override
    public String copy(String sourceStorageId) {
        Path source = resolveWithinRoot(sourceStorageId);
        String newStorageId = UUID.randomUUID().toString();
        try {
            Files.copy(source, resolveWithinRoot(newStorageId), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new StorageException("Failed to copy file " + sourceStorageId, e);
        }
        return newStorageId;
    }

    @Override
    public Resource loadAsResource(String storageId) {
        Path file = resolveWithinRoot(storageId);
        try {
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new StorageException("File not found or not readable: " + storageId);
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new StorageException("Could not read file " + storageId, e);
        }
    }

    @Override
    public void delete(String storageId) {
        try {
            Files.deleteIfExists(resolveWithinRoot(storageId));
        } catch (IOException e) {
            throw new StorageException("Failed to delete file " + storageId, e);
        }
    }

    // Resolves storageId against root and refuses anything that would escape root — a UUID never
    // would, but this keeps the guarantee even if an untrusted id reaches loadAsResource/delete.
    private Path resolveWithinRoot(String storageId) {
        Path resolved = root.resolve(storageId).normalize();
        if (!resolved.getParent().equals(root)) {
            throw new StorageException("Invalid storage id: " + storageId);
        }
        return resolved;
    }
}
