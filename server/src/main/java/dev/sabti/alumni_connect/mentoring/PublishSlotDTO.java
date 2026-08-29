package dev.sabti.alumni_connect.mentoring;

import dev.sabti.alumni_connect.auth.entities.Fields;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class PublishSlotDTO {
    @NotNull(message = "Field 'startsAt' is required")
    private OffsetDateTime startsAt;

    // Null falls back to the configured session length.
    private Integer durationMinutes;

    @NotNull(message = "Field 'track' is required")
    private MentoringTrack track;

    // Null means any filière may book it.
    private Fields targetField;

    @NotBlank(message = "Field 'meetingLink' is required")
    private String meetingLink;
}
