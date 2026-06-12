package dev.sabti.alumni_connect.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

// Raw byte storage, deliberately backend-agnostic: the rest of the app only ever holds the
// opaque storageId this returns, never a path or URL. The current implementation writes to the
// local filesystem (LocalStorageService); swapping to S3/MinIO later is a new implementation of
// this interface with no change to callers, since storageId stays the only contract.
public interface StorageService {

    // Persists the bytes under a freshly generated, unguessable storageId, which it returns.
    String store(MultipartFile file);

    // Duplicates an existing file under a new storageId, returned. Used to snapshot a candidate's
    // profile resume onto an application so the two are independent (a later profile replace,
    // which deletes the profile's file, can't break the application's copy).
    String copy(String sourceStorageId);

    Resource loadAsResource(String storageId);

    void delete(String storageId);
}
