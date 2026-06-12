package dev.sabti.alumni_connect.storage;

// Unchecked failure of the storage backend itself (disk I/O, missing file, bad path). Distinct
// from a validation problem with the upload — those are caller errors handled before we get here.
public class StorageException extends RuntimeException {
    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
