package info.dylansouthard.StraysBookAPI.dto.friendo;

import info.dylansouthard.StraysBookAPI.dto.ConditionDTO;
import info.dylansouthard.StraysBookAPI.dto.StatusDTO;
import info.dylansouthard.StraysBookAPI.dto.careEvent.CareEventSummaryDTO;
import info.dylansouthard.StraysBookAPI.dto.user.UserSummaryMinDTO;
import info.dylansouthard.StraysBookAPI.dto.vaccination.VaccinationSummaryDTO;
import info.dylansouthard.StraysBookAPI.model.enums.SexType;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
public class AnimalDTO extends FriendoDTO {

    @Schema(
            description = "Sex of the animal",
            example = "FEMALE",
            nullable = false,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private SexType sex;

    @Schema(
            description = "Does the animal have a collar?",
            example = "true",
            nullable = false,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private boolean hasCollar = false;

    @Schema(
            description = "Birthdate of the animal",
            example = "2021-01-01",
            type = "string",
            format = "date",
            nullable = true,
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private LocalDate born;

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

    @Schema(
            description = "Is the animal sterilized?",
            example = "false",
            nullable = true,
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private boolean sterilized;

    @ArraySchema(
            schema = @Schema(example = "{\"type\":\"RAB\", \"verificationStatus\":\"VERIFIED\"}"),
            arraySchema = @Schema(
                    description = "List of vaccinations the animal has received",
                    nullable = false,
                    requiredMode = Schema.RequiredMode.REQUIRED
            )
    )
    private List<VaccinationSummaryDTO> vaccinations = new ArrayList<>();

    @Schema(
            description = "User who is the primary caretaker of the animal",
            nullable=true,
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private UserSummaryMinDTO primaryCaretaker;

    @ArraySchema(
            schema = @Schema(example = "{\"id\":105, \"type\":\"VACCINATED\", \"newValue\":\"RAB\", \"date\":\"2020-01-01\"}"),
            arraySchema = @Schema(
                    description = "List of the most recent care events related to the animal",
                    nullable = false,
                    requiredMode = Schema.RequiredMode.REQUIRED
            )
    )
    private List<CareEventSummaryDTO> recentCareEvents = new ArrayList<>();
}
