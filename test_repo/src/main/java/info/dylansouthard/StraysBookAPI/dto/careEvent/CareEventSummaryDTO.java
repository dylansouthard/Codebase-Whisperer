package info.dylansouthard.StraysBookAPI.dto.careEvent;

import info.dylansouthard.StraysBookAPI.dto.user.UserSummaryMinDTO;
import info.dylansouthard.StraysBookAPI.model.enums.CareEventType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter
public class CareEventSummaryDTO {

    @Schema(
            description = "Unique ID of the Care Event",
            example = "105",
            type = "integer",
            nullable = false,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long id;

    @Schema(
            description = "Type of the care event",
            example = "FED",
            nullable = false,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private CareEventType type;

    @Schema(
            description = "Date and time the care event occurred",
            example = "2024-03-02T14:30:00",
            type = "string",
            format = "date-time",
            nullable = false,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private LocalDateTime date;

    @Schema(
            description = "New value associated with the event (e.g., a new vaccination type or updated status)",
            example = "RAB",
            nullable = true,
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private String newValue;

    @Schema(
            description = "Additional notes about the care event",
            example = "This dog was fed dry kibble",
            nullable = true,
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private String notes;

    @Schema(
            description = "User who registered the care event",
            nullable = true,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private UserSummaryMinDTO registeredBy;
}
