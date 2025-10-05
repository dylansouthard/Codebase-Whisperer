package info.dylansouthard.StraysBookAPI.model;

import info.dylansouthard.StraysBookAPI.model.base.VerifiableDBEntity;
import info.dylansouthard.StraysBookAPI.model.enums.VaccinationType;
import info.dylansouthard.StraysBookAPI.model.user.User;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="vaccinations")
@NoArgsConstructor
@Getter @Setter
@EqualsAndHashCode(callSuper = true)
public class Vaccination extends VerifiableDBEntity {

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private VaccinationType type;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "care_event_id", referencedColumnName = "id")
    private CareEvent careEvent;

    public Vaccination(VaccinationType type, CareEvent careEvent) {
        this.type = type;
        this.careEvent = careEvent;
    }

    public Vaccination(VaccinationType type) {
        this.type = type;
    }

    public Vaccination(VaccinationType type, User createdBy) {
        super(createdBy);
        this.type = type;
    }
}
