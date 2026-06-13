package dev.sabti.alumni_connect.candidate;

import dev.sabti.alumni_connect.storage.FileDownload;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

// Three-outcome result for fetching a candidate's own profile photo — the sibling of ResumeDownload.
// An Optional can't distinguish the two failures, which map to different statuses: "you're not a
// candidate" (403, consistent with the other /me endpoints) vs "you are, but have no photo" (404).
// The service returns this domain outcome and the controller maps it, keeping the service
// HTTP-agnostic. file is non-null only for FOUND.
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PhotoDownload {

    public enum Status {
        FOUND,          // -> 200 + the bytes
        NOT_CANDIDATE,  // caller has no CandidateProfile -> 403
        NO_PHOTO        // candidate, but nothing uploaded -> 404
    }

    private final Status status;
    private final FileDownload file;

    public static PhotoDownload found(FileDownload file) {
        return new PhotoDownload(Status.FOUND, file);
    }

    public static PhotoDownload notCandidate() {
        return new PhotoDownload(Status.NOT_CANDIDATE, null);
    }

    public static PhotoDownload noPhoto() {
        return new PhotoDownload(Status.NO_PHOTO, null);
    }
}
