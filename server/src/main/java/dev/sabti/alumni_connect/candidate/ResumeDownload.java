package dev.sabti.alumni_connect.candidate;

import dev.sabti.alumni_connect.storage.FileDownload;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

// Three-outcome result for fetching a candidate's own resume — an Optional can't distinguish the
// two failures, which map to different statuses: "you're not a candidate" (403, consistent with
// the other /me endpoints) vs "you are, but have no resume" (404). The service returns this
// domain outcome and the controller maps it, keeping the service HTTP-agnostic. file is non-null
// only for FOUND.
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ResumeDownload {

    public enum Status {
        FOUND,          // -> 200 + the bytes
        NOT_CANDIDATE,  // caller has no CandidateProfile -> 403
        NO_RESUME       // candidate, but nothing uploaded -> 404
    }

    private final Status status;
    private final FileDownload file;

    public static ResumeDownload found(FileDownload file) {
        return new ResumeDownload(Status.FOUND, file);
    }

    public static ResumeDownload notCandidate() {
        return new ResumeDownload(Status.NOT_CANDIDATE, null);
    }

    public static ResumeDownload noResume() {
        return new ResumeDownload(Status.NO_RESUME, null);
    }
}
