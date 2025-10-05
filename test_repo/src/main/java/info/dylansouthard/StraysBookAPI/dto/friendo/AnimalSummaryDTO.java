package info.dylansouthard.StraysBookAPI.dto.friendo;

import info.dylansouthard.StraysBookAPI.dto.ConditionDTO;
import info.dylansouthard.StraysBookAPI.dto.StatusDTO;
import info.dylansouthard.StraysBookAPI.model.enums.SexType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter
public class AnimalSummaryDTO extends FriendoSummaryDTO {
    @Schema(
            description = "Sex of the animal",
            example = "FEMALE",
            nullable = false,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private SexType sex;

    @Schema(
            description = "The last time the animal is recorded as being fed",
            example = "2021-01-01T14:30:00",
            type = "string",
            format = "date",
            nullable = true,
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private LocalDateTime lastFed;

    @Schema(
            description = "Condition details of the animal",
            nullable=false,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private ConditionDTO condition;

    @Schema(
            description = "Current status of the animal",
            nullable=false,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private StatusDTO status;
}
