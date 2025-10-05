package info.dylansouthard.StraysBookAPI.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(
        name = "WatchlistUpdateResult",
        description = "Result of updating the user's watchlist for a specific animal."
)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class WatchlistUpdateResultDTO {

    @Schema(
            description = "ID of the animal that was added to or removed from the watchlist.",
            example = "12345",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    Long animalId;

    @Schema(
            description = "Whether the animal is now watched by the current user after the operation.",
            example = "true",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    boolean isWatched;
}