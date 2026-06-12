package dev.sabti.alumni_connect.storage;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.core.io.Resource;

// Pairs a stored file's metadata (for the response content-type / filename headers) with the
// readable bytes, so a download endpoint has everything it needs from a single load() call.
@Getter
@AllArgsConstructor
public class FileDownload {
    private final StoredFile metadata;
    private final Resource resource;
}
