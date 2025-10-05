package info.dylansouthard.StraysBookAPI.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import info.dylansouthard.StraysBookAPI.common.BaseDBTest;
import info.dylansouthard.StraysBookAPI.config.DummyTestData;
import info.dylansouthard.StraysBookAPI.constants.ApiRoutes;
import info.dylansouthard.StraysBookAPI.dto.user.AuthTokenDTO;
import info.dylansouthard.StraysBookAPI.model.user.User;
import info.dylansouthard.StraysBookAPI.service.AuthTokenService;
import info.dylansouthard.StraysBookAPI.testutils.TestSecurityUtil;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
@AutoConfigureMockMvc
public class AuthTokenControllerIT extends BaseDBTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private AuthTokenService authTokenService;

    @Test
    public void When_StatusCalledWithValidToken_Expect_AuthenticatedTrue() throws Exception {
        User user = userRepository.save(DummyTestData.createUser());

        // Generate token and simulate login
        AuthTokenDTO tokenDTO = authTokenService.generateAccessToken(user, "test-device");
        String bearerToken = "Bearer " + tokenDTO.getToken();

        mockMvc.perform(get(ApiRoutes.AUTH.STATUS)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.user.id").value(user.getId()));
    }

    @Test
    public void When_StatusCalledWithoutToken_Expect_AuthenticatedFalse() throws Exception {
        mockMvc.perform(get(ApiRoutes.AUTH.STATUS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(false))
                .andExpect(jsonPath("$.user").doesNotExist());
    }

        @Test
        public void When_RefreshToken_Expect_NewAccessTokenReturned() throws Exception {
            User user = userRepository.save(DummyTestData.createUser());
            String deviceId = "device123";
            AuthTokenDTO refreshToken = authTokenService.generateRefreshToken(user, deviceId, null);

            mockMvc.perform(post(ApiRoutes.AUTH.REFRESH)
                            .param("device_id", deviceId)
                            .param("refresh_token", refreshToken.getToken()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").exists())
                    .andExpect(jsonPath("$.accessToken.type").value("ACCESS"))
                    .andExpect(jsonPath("$.accessToken.token").isNotEmpty())
                    .andExpect(jsonPath("$.refreshToken").exists())
                    .andExpect(jsonPath("$.refreshToken.token").value(refreshToken.getToken()))
                    .andExpect(jsonPath("$.refreshToken.type").value("REFRESH"));
        }

        @Test
        public void When_RevokeAllTokens_Expect_NoTokensRemain() throws Exception {
            User user = userRepository.save(DummyTestData.createUser());
            authTokenService.generateRefreshToken(user, "device1", null);
            authTokenService.generateRefreshToken(user, "device2", null);

            TestSecurityUtil.authenticateTestUser(user);

            mockMvc.perform(delete(ApiRoutes.AUTH.REVOKE_ALL)
                            .with(TestSecurityUtil.testUser(user)))
                    .andExpect(status().isNoContent());
        }

        @Test
        public void When_RevokeTokenForDevice_Expect_TokenRemoved() throws Exception {
            User user = userRepository.save(DummyTestData.createUser());
            authTokenService.generateRefreshToken(user, "device123", null);

            TestSecurityUtil.authenticateTestUser(user);

            mockMvc.perform(delete(ApiRoutes.AUTH.REVOKE)
                            .param("deviceId", "device123")
                            .with(TestSecurityUtil.testUser(user)))
                    .andExpect(status().isNoContent());
        }
    }



