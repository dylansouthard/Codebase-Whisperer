package info.dylansouthard.StraysBookAPI.dto.careEvent;

import info.dylansouthard.StraysBookAPI.dto.friendo.AnimalSummaryDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CareEventDTO extends CareEventSummaryDTO {

    @Schema(
            description = "ID of the entity associated with this care event (e.g., Vaccination, Treatment). Depends on the event type.",
            example = "10",
            nullable = false,
            requiredMode = Schema.RequiredMode.REQUIRED,
            type = "integer"
    )
    private long associatedId;

    @Schema(
            description = "Set of animals involved in the care event",
            nullable = false,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Set<AnimalSummaryDTO> animals = new HashSet<>();

}
