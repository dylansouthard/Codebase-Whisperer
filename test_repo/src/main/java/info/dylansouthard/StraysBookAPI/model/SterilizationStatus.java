package info.dylansouthard.StraysBookAPI.model;

import info.dylansouthard.StraysBookAPI.model.base.VerifiableDBEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="sterilization_statuses")
@NoArgsConstructor
@Getter @Setter
public class SterilizationStatus extends VerifiableDBEntity {

    private Boolean sterilized;

    public SterilizationStatus(Boolean sterilized) {
        this.sterilized = sterilized;
    }
}
