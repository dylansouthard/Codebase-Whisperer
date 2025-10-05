package info.dylansouthard.StraysBookAPI.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import info.dylansouthard.StraysBookAPI.config.OAuthProperties;
import info.dylansouthard.StraysBookAPI.dto.user.*;
import info.dylansouthard.StraysBookAPI.errors.ErrorFactory;
import info.dylansouthard.StraysBookAPI.mapper.OauthProfileMapper;
import info.dylansouthard.StraysBookAPI.mapper.UserMapper;
import info.dylansouthard.StraysBookAPI.model.enums.OAuthProviderType;
import info.dylansouthard.StraysBookAPI.model.user.User;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Objects;

@Service
public class OAuthService {

    @Autowired
    private OAuthProperties oAuthProperties;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OauthProfileMapper oauthProfileMapper;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AuthTokenService authTokenService;

    public String buildRedirectUri(OAuthProviderType provider, String deviceId, String platform) {
        String clientId = oAuthProperties.getClientId(provider);
        String redirectUri = oAuthProperties.getRedirectUri(provider);
        String scope = oAuthProperties.getScope(provider);
        String authUri = oAuthProperties.getAuthorizationUri(provider);
        String stateJson = String.format("{\"device_id\":\"%s\",\"platform\":\"%s\"}", deviceId, platform);
        String encodedState = java.net.URLEncoder.encode(stateJson, java.nio.charset.StandardCharsets.UTF_8);
       return String.format("%s?client_id=%s&redirect_uri=%s&response_type=code&scope=%s&state=%s",
                authUri,
                java.net.URLEncoder.encode(clientId, java.nio.charset.StandardCharsets.UTF_8),
                java.net.URLEncoder.encode(redirectUri, java.nio.charset.StandardCharsets.UTF_8),
                java.net.URLEncoder.encode(scope, java.nio.charset.StandardCharsets.UTF_8),
                encodedState
        );

    }

    private ResponseEntity<OAuthTokenResponseDTO> getAuthToken(OAuthProviderType provider, String code) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", code);
        body.add("grant_type", "authorization_code");
        body.add("redirect_uri", oAuthProperties.getRedirectUri(provider));
        body.add("client_id", oAuthProperties.getClientId(provider));
        body.add("client_secret", oAuthProperties.getSecret(provider));

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        return restTemplate.postForEntity(oAuthProperties.getTokenUri(provider), request, OAuthTokenResponseDTO.class);
    }

    private OAuthProfileDTO getProfile(String accessToken, OAuthProviderType provider) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        HttpEntity<String> request = new HttpEntity<>(headers);

        ResponseEntity<byte[]> response = restTemplate.exchange(
                oAuthProperties.getUserInfoUri(provider),
                HttpMethod.GET,
                request,
                byte[].class
        );
        try {
            String responseBody = new String(Objects.requireNonNull(response.getBody()), java.nio.charset.StandardCharsets.UTF_8);
            JsonNode resJson = objectMapper.readTree(responseBody);
            return oauthProfileMapper.map(provider, resJson);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Transactional
    public OAuthLoginResponseDTO processOAuthCallback(String provider, String code, String state) {
        OAuthProviderType providerType = OAuthProviderType.getProvider(provider);
        if (providerType == null) throw ErrorFactory.invalidParams();
        try {
            OAuthStateDTO oauthState = objectMapper.readValue(state, OAuthStateDTO.class);
            OAuthTokenResponseDTO res = getAuthToken(providerType, code).getBody();

            assert res != null;
            OAuthProfileDTO profile = getProfile(res.getAccessToken(), providerType);

            User user = userService.fetchOrCreateUserFromOAuthProfile(profile, providerType);

            AuthTokenPairDTO authTokens = authTokenService.generateAuthTokens(user, oauthState.getDeviceId());

            UserPrivateDTO userDto = userMapper.toUserPrivateDTO(user);

            return new OAuthLoginResponseDTO(authTokens, userDto);

        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw ErrorFactory.internalServerError();
        }

    }
}








