package info.dylansouthard.StraysBookAPI.controller;

import info.dylansouthard.StraysBookAPI.constants.ApiRoutes;
import info.dylansouthard.StraysBookAPI.dto.user.OAuthLoginResponseDTO;
import info.dylansouthard.StraysBookAPI.errors.ErrorFactory;
import info.dylansouthard.StraysBookAPI.model.enums.OAuthProviderType;
import info.dylansouthard.StraysBookAPI.service.OAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Validated
public class OauthController {

    @Autowired
    OAuthService oAuthService;

    @GetMapping(ApiRoutes.AUTH.OAUTH)
    public ResponseEntity<Void> handleAuthRequest(
            @PathVariable final String provider,
            @RequestParam("device_id") final String deviceId,
            @RequestParam final String platform
    ) {
        if (deviceId == null || deviceId.isEmpty()) throw ErrorFactory.missingDeviceId();
        OAuthProviderType providerType = OAuthProviderType.getProvider(provider);
        if (providerType == null) throw ErrorFactory.unknownProvider();
        String redirectUri = oAuthService.buildRedirectUri(providerType, deviceId, platform);

        return ResponseEntity.status(302)
                .header("Location", redirectUri)
                .build();
    }

    @GetMapping(value=ApiRoutes.AUTH.OAUTH_CALLBACK)
    public ResponseEntity<OAuthLoginResponseDTO> handleOAuthCallback(
            @PathVariable final String provider,
            @RequestParam final String code,
            @RequestParam final String state
    ) {
        OAuthLoginResponseDTO res = oAuthService.processOAuthCallback(provider, code, state);
        return ResponseEntity.ok(res);
    }
}
