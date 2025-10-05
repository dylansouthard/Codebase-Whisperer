package info.dylansouthard.StraysBookAPI.dto.friendo;

import info.dylansouthard.StraysBookAPI.model.enums.AnimalType;
import info.dylansouthard.StraysBookAPI.model.enums.FriendoType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
public abstract class FriendoSummaryDTO extends FriendoSummaryMinDTO {
    @Schema(
            description = "Type of friendo",
            example = "ANIMAL",
            nullable = false,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private FriendoType friendoType;

    @Schema(
            description = "Type of animal (e.g., DOG, CAT, LITTER)",
            example = "DOG",
            nullable = false,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private AnimalType type;

    @Schema(
            description = "Description of the animal/litter",
            example = "A cat with nine tails",
            nullable = true,
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private String description;
}

