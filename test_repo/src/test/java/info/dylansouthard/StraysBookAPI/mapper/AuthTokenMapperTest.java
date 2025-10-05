package info.dylansouthard.StraysBookAPI.mapper;

import info.dylansouthard.StraysBookAPI.dto.user.AuthTokenDTO;
import info.dylansouthard.StraysBookAPI.model.enums.AuthTokenType;
import info.dylansouthard.StraysBookAPI.model.user.AuthToken;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class AuthTokenMapperTest {
    private final AuthTokenMapper authTokenMapper = Mappers.getMapper(AuthTokenMapper.class);

    @Test
    public void When_MappingToAuthTokenDTO_Then_ReturnAuthTokenDTO() {
        // Arrange
        AuthTokenType type = AuthTokenType.ACCESS;
        String token = "sample.jwt.token.string";
        LocalDateTime issuedAt = LocalDateTime.now();
        LocalDateTime expiresAt = issuedAt.plusMinutes(15);
        String deviceId = "device-123";

        AuthToken authToken = new AuthToken(type, token, issuedAt, expiresAt, deviceId);

        // Act
        AuthTokenDTO dto = authTokenMapper.toAuthTokenDTO(authToken);

        // Assert
        assertNotNull(dto, "DTO should not be null");
        assertEquals(type, dto.getType(), "DTO type should match the AuthToken type");
        assertEquals(token, dto.getToken(), "DTO token should match the AuthToken token string");
        assertEquals(issuedAt, dto.getIssuedAt(), "DTO issuedAt should match the AuthToken issuedAt time");
        assertEquals(expiresAt, dto.getExpiresAt(), "DTO expiresAt should match the AuthToken expiresAt time");
    }
}
