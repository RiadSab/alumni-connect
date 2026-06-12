package dev.sabti.alumni_connect.storage;

import dev.sabti.alumni_connect.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Metadata registry for a stored file. Deliberately lean: it carries what's needed to serve the
// bytes back (content type, original name) but NOT a back-pointer to its owner — the owning
// entity (CandidateProfile.resumeId, Company.logoId, ...) already references this via storageId,
// so an ownerId/ownerType here would be a redundant second source of truth. storageId is the
// external handle and the on-disk filename; the Long id stays internal.
@Entity
@Table(name = "stored_files")
@Getter @Setter
@AllArgsConstructor @NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class StoredFile extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String storageId;

    @Column(nullable = false)
    private String originalFilename;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private long size;
}
