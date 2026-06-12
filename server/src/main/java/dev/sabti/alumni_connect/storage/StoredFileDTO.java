package dev.sabti.alumni_connect.storage;

import lombok.Data;

// The metadata returned after an upload — notably the storageId, which is the handle callers use
// to fetch the file later. The bytes themselves are never in an API response body except on download.
@Data
public class StoredFileDTO {
    private String storageId;
    private String originalFilename;
    private String contentType;
    private long size;

    public static StoredFileDTO from(StoredFile file) {
        StoredFileDTO dto = new StoredFileDTO();
        dto.storageId = file.getStorageId();
        dto.originalFilename = file.getOriginalFilename();
        dto.contentType = file.getContentType();
        dto.size = file.getSize();
        return dto;
    }
}
