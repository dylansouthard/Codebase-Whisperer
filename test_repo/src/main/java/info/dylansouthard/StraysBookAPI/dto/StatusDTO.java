package info.dylansouthard.StraysBookAPI.dto;

import info.dylansouthard.StraysBookAPI.model.enums.StatusType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
public class StatusDTO {

    @Schema(
            description = "Current status of the animal",
            example = "STRAY",
            nullable = false,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private StatusType type = StatusType.STRAY;

    @Schema(
            description = "Additional notes about the animal's status",
            example = "This dog was found in the street",
            nullable = true,
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private String notes;
}

