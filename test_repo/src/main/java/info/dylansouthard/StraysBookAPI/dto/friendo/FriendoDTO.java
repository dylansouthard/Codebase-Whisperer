package info.dylansouthard.StraysBookAPI.dto.friendo;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import info.dylansouthard.StraysBookAPI.dto.user.UserSummaryMinDTO;
import info.dylansouthard.StraysBookAPI.model.shared.GeoSchema;
import info.dylansouthard.StraysBookAPI.serializers.GeoSchemaSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
public abstract class FriendoDTO extends FriendoSummaryDTO {

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
            description = "User who registered the animal/litter. " +
                    "This can be null if the user has been deleted from the system.",
            nullable = true,
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private UserSummaryMinDTO registeredBy;

    @Schema(
            description = "Timestamp when the animal/litter was first registered",
            example = "2024-03-02T14:30:00",
            type = "string",
            format = "date-time",
            nullable = false,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private LocalDateTime registeredAt;

    @Schema(
            description = "Timestamp of the last update to the animal/litter's data",
            example = "2024-03-02T14:30:00",
            type = "string",
            format = "date-time",
            nullable = false,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private LocalDateTime lastUpdated;

    @Schema(
            description = "Additional notes about the animal/litter",
            example = "This is a very friendly dog",
            nullable = true,
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private String notes;

    @JsonSerialize(using = GeoSchemaSerializer.class)
    public GeoSchema getLocation() {
        return location;
    }
}

