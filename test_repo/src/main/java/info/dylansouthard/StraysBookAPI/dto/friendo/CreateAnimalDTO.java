package info.dylansouthard.StraysBookAPI.dto.friendo;

import info.dylansouthard.StraysBookAPI.dto.ConditionDTO;
import info.dylansouthard.StraysBookAPI.dto.StatusDTO;
import info.dylansouthard.StraysBookAPI.model.enums.SexType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Schema(name = "CreateAnimalDTO", description = "DTO for creating an animal")
public class CreateAnimalDTO extends CreateFriendoDTO {
    @Schema(
            description = "Sex of the animal",
            example = "FEMALE",
            nullable = false,
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private SexType sex = SexType.UNKNOWN;

    @Schema(
            description = "Does the animal have a collar?",
            example = "true",
            nullable = true,
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private boolean hasCollar = false;

    @Schema(
            description = "Is the animal sterilized?",
            example = "false",
            nullable = true,
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private boolean sterilized;

    @Schema(
            description = "Condition details of the animal",
            nullable=true,
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private ConditionDTO condition;

    @Schema(
            description = "Current status of the animal",
            nullable=false,
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private StatusDTO status;
}
