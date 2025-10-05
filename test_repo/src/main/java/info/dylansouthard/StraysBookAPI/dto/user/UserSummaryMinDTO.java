package info.dylansouthard.StraysBookAPI.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserSummaryMinDTO {
    @Schema(
            description = "Unique ID of the user",
            example = "1",
            type="integer",
            nullable = false,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long id;

    @Schema(
            description = "Unique display name of the user",
            example = "Jimbo Jambo III",
            nullable = false,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String displayName;
}
