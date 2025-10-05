package info.dylansouthard.StraysBookAPI.service;

import info.dylansouthard.StraysBookAPI.common.BaseDBTest;
import info.dylansouthard.StraysBookAPI.config.DummyTestData;
import info.dylansouthard.StraysBookAPI.dto.user.AuthTokenDTO;
import info.dylansouthard.StraysBookAPI.dto.user.AuthTokenPairDTO;
import info.dylansouthard.StraysBookAPI.errors.ErrorFactory;
import info.dylansouthard.StraysBookAPI.model.enums.AuthTokenType;
import info.dylansouthard.StraysBookAPI.model.user.AuthToken;
import info.dylansouthard.StraysBookAPI.model.user.User;
import info.dylansouthard.StraysBookAPI.testutils.ExceptionAssertionRunner;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class AuthTokenServiceIT extends BaseDBTest {

    @Autowired
    private AuthTokenService authTokenService;

    @Test
    public void When_GeneratingAccessTokenForValidUser_Expect_AccessTokenGenerated() {
        User user = userRepository.save(DummyTestData.createUser());
        AuthTokenDTO dto = authTokenService.generateAccessToken(user, "abc123");
        assertEquals(AuthTokenType.ACCESS, dto.getType(), "Should be of type access");
    }

    @Test
    public void When_GeneratingRefreshTokenForValidUser_Expect_RefreshTokenGenerated() {
        User user = userRepository.save(DummyTestData.createUser());
        AuthTokenDTO dto = authTokenService.generateRefreshToken(user, "abc123", null);
        assertEquals(AuthTokenType.REFRESH, dto.getType(), "Should be of type refresh");
    }

    @Test
    public void When_ValidRefreshTokenProvided_Expect_NewAccessToken() {
        User user = userRepository.save(DummyTestData.createUser());
        AuthTokenDTO refreshToken = authTokenService.generateRefreshToken(user, "device123", null);

        AuthTokenPairDTO tokenPair = authTokenService.refreshAccessToken(refreshToken.getToken(), "device123");

        assertAll("Regenerated Auth Token Pair",
                () -> assertNotNull(tokenPair, "Token pair should not be null"),
                () -> assertNotNull(tokenPair.getAccessToken(), "Access token should not be null"),
                () -> assertEquals(AuthTokenType.ACCESS, tokenPair.getAccessToken().getType(), "Should be of type ACCESS"),
                () -> assertTrue(tokenPair.getAccessToken().getExpiresAt().isAfter(LocalDateTime.now()), "Access token should be valid"),
                () -> assertEquals(refreshToken.getToken(), tokenPair.getRefreshToken().getToken(), "Refresh token should match original"),
                () -> assertEquals(AuthTokenType.REFRESH, tokenPair.getRefreshToken().getType(), "Refresh token should be of type REFRESH")
        );
    }

    @Test
    public void When_ExpiredRefreshTokenProvided_Expect_AuthForbidden() {
        User user = userRepository.save(DummyTestData.createUser());
        AuthToken expired = DummyTestData.createExpiredRefreshToken(user, "device123");

        authTokenRepository.save(expired);

        ExceptionAssertionRunner.assertThrowsExceptionOfType(
                () -> authTokenService.refreshAccessToken(expired.getToken(),
                        "device123"), ErrorFactory.invalidToken(),
                "Expired refresh token"
        );
    }

    @Test
    public void When_RevokeAllTokensCalled_Expect_UserTokensCleared() {
        User user = userRepository.save(DummyTestData.createUser());
        authTokenService.generateRefreshToken(user, "device1", null);
        authTokenService.generateRefreshToken(user, "device2", null);

        authTokenService.revokeAllTokensForUser(user);
        // Ensure tokens are removed from DB
        List<AuthToken> tokens = authTokenRepository.findByUserId(user.getId());
        assertTrue(tokens.isEmpty(), "Auth tokens should be removed from the database");

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertTrue(updated.getAuthTokens().isEmpty(), "All tokens should be revoked");
    }

    @Test
    public void When_RevokeTokensForDevice_Expect_TokenRemovedFromUserAndDB() {
        User user = userRepository.save(DummyTestData.createUser());
        AuthTokenDTO token1 = authTokenService.generateRefreshToken(user, "device1", null);
        AuthTokenDTO token2 = authTokenService.generateRefreshToken(user, "device2", null);

        authTokenService.revokeTokensForDevice(user, "device1");

        // Fetch the user again
        User updatedUser = userRepository.findById(user.getId()).orElseThrow();

        // Check user's tokens no longer include token1
        boolean stillHasDevice1 = updatedUser.getAuthTokens().stream()
                .anyMatch(token -> token.getDeviceId().equals("device1"));
        boolean stillHasDevice2 = updatedUser.getAuthTokens().stream()
                .anyMatch(token -> token.getDeviceId().equals("device2"));

        assertFalse(stillHasDevice1, "User should not have token for device1");
        assertTrue(stillHasDevice2, "User should still have token for device2");

        // Check DB no longer contains token1
        List<AuthToken> tokensInDb = authTokenRepository.findByUserId(user.getId());
        boolean dbHasDevice1 = tokensInDb.stream()
                .anyMatch(token -> token.getDeviceId().equals("device1"));
        boolean dbHasDevice2 = tokensInDb.stream()
                .anyMatch(token -> token.getDeviceId().equals("device2"));

        assertFalse(dbHasDevice1, "DB should not have token for device1");
        assertTrue(dbHasDevice2, "DB should still have token for device2");
    }
}
