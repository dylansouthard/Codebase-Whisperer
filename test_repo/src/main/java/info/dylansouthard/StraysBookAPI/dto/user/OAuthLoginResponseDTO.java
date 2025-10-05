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
public class OAuthLoginResponseDTO {
    @Schema(
            description = "Newly issued access and refresh tokens",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private AuthTokenPairDTO authTokens;

    @Schema(
            description = "User data",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private UserPrivateDTO user;

}
