package info.dylansouthard.StraysBookAPI.model.base;

import info.dylansouthard.StraysBookAPI.model.enums.VerificationStatusType;
import info.dylansouthard.StraysBookAPI.model.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@MappedSuperclass
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public abstract class VerifiableDBEntity extends UserRegisteredDBEntity {
    @Column
    private VerificationStatusType verificationStatus = VerificationStatusType.UNVERIFIED;

    @Column
    private String verificationProofUrl;

    @ManyToOne
    private User verifiedBy;

    public void verify(User verifiedBy) {
        this.verifiedBy = verifiedBy;
        this.verificationStatus = VerificationStatusType.VERIFIED;
    }

    public VerifiableDBEntity(User createdBy) {
        super(createdBy);

    }
}
