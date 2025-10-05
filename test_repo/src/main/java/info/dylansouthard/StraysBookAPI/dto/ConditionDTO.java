package info.dylansouthard.StraysBookAPI.dto;

import info.dylansouthard.StraysBookAPI.model.enums.ConditionType;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter @Setter
@NoArgsConstructor
public class ConditionDTO {

    @ArraySchema(
            schema = @Schema(example = "HEALTHY"),
            arraySchema = @Schema(
                    description = "List of health-related conditions affecting the animal",
                    nullable = false,
                    requiredMode = Schema.RequiredMode.NOT_REQUIRED
            )
    )
    private List<ConditionType> types = new ArrayList<>();

    @Schema(
            description = "Indicates if the animal is in critical condition",
            example = "false",
            nullable = false,
            requiredMode = Schema.RequiredMode.REQUIRED,
            defaultValue = "false"
    )
    private Boolean critical = false;

    @Schema(
            description = "Overall health rating of the animal on a scale from 1 (poor) to 5 (excellent). " +
                    "If provided, it must be between 1 and 5.",
            example = "5",
            nullable = true,
            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            minimum = "1",
            maximum = "5"
    )
    private Integer rating;

    @Schema(
            description = "Additional notes about the animal's condition",
            example = "This dog is very healthy",
            nullable = true,
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private String notes;

    public ConditionDTO(List<ConditionType> types, Boolean critical, Integer rating, String notes) {
        this.types = types;
        this.critical = critical;
        this.rating = rating;
        this.notes = notes;
    }
}
