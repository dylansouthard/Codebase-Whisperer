package info.dylansouthard.StraysBookAPI.dto.friendo;

import info.dylansouthard.StraysBookAPI.model.shared.GeoSchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
public abstract class CreateFriendoDTO extends FriendoSummaryDTO {

    @Schema(
            description = "Indicates if the animal/litter is considered dangerous",
            example = "false",
            nullable = false,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private boolean isDangerous = false;

    @Schema(
            description = "Geolocation of the animal/litter",
            nullable = false,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private GeoSchema location;

    @Schema(
            description = "Additional notes about the animal/litter",
            example = "This is a very friendly dog",
            nullable = true,
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private String notes;
}

