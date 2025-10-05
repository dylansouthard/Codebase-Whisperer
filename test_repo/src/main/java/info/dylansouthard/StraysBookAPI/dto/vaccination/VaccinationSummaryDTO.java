package info.dylansouthard.StraysBookAPI.dto.vaccination;

import info.dylansouthard.StraysBookAPI.model.enums.VaccinationType;
import info.dylansouthard.StraysBookAPI.model.enums.VerificationStatusType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
public class VaccinationSummaryDTO {

    @Schema(
            description = "Unique ID of the vaccination",
            example = "1",
            type="integer",
            nullable = false,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long id;


    @Schema(
            description = "Type of vaccination received",
            example = "RAB",
            nullable = false,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private VaccinationType type;

    @Schema(
            description = "Verification status of the vaccination",
            example = "VERIFIED",
            nullable = false,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private VerificationStatusType verificationStatus = VerificationStatusType.UNVERIFIED;
}
