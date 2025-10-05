package info.dylansouthard.StraysBookAPI.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AuthStatusDTO {

    @Schema(
            description = "Indicates if the user is authenticated",
            example = "true",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private boolean isAuthenticated;

    @Schema(
            description = "Details of the authenticated user",
            implementation = UserPrivateDTO.class
    )
    private UserPrivateDTO user;
}
