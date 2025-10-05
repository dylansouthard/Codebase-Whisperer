package info.dylansouthard.StraysBookAPI.dto.user;

import info.dylansouthard.StraysBookAPI.model.enums.AuthTokenType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AuthTokenDTO {

    @Schema(
            description = "Type of the token (ACCESS or REFRESH)",
            example = "ACCESS",
            requiredMode = Schema.RequiredMode.REQUIRED,
            nullable = false
    )
    private AuthTokenType type;

    @Schema(
            description = "JWT string representing the token",
            example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
            requiredMode = Schema.RequiredMode.REQUIRED,
            nullable = false
    )
    private String token;

    @Schema(
            description = "Time when the token was issued",
            example = "2025-03-16T14:30:00",
            format = "date-time",
            requiredMode = Schema.RequiredMode.REQUIRED,
            nullable = false
    )
    private LocalDateTime issuedAt;

    @Schema(
            description = "Token expiration time",
            example = "2025-03-16T15:30:00",
            format = "date-time",
            requiredMode = Schema.RequiredMode.REQUIRED,
            nullable = false
    )
    private LocalDateTime expiresAt;
}