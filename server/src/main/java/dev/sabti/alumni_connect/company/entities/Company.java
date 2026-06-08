package dev.sabti.alumni_connect.company.entities;

import dev.sabti.alumni_connect.auth.entities.Fields;
import dev.sabti.alumni_connect.shared.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;

@Entity
@Table(name = "companies")
@Getter @Setter
@AllArgsConstructor @NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Company extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Email
    @Column(nullable = false, unique = true)
    private String email;

    private String phone;

    @Enumerated(EnumType.STRING)
    private Fields field;

    @Column(length = 2000)
    private String description;

    private String website;
    private String address;
    private String logoId;
    private String videoPresentationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CompanyStatus status = CompanyStatus.PENDING;

    @Enumerated(EnumType.STRING)
    private CompanySize size;

    // nullable, set by an admin when rejecting/suspending — mirrors User.statusChangeReason
    private String statusChangeReason;
}
