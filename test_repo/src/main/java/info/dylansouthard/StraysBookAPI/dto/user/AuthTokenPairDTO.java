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
public class AuthTokenPairDTO {
    @Schema(
            description = "Newly issued access token",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private AuthTokenDTO accessToken;

    @Schema(
            description = "Newly issued refresh token",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private AuthTokenDTO refreshToken;
}
