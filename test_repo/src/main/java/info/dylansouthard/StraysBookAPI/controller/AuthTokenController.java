package info.dylansouthard.StraysBookAPI.controller;

import info.dylansouthard.StraysBookAPI.constants.ApiRoutes;
import info.dylansouthard.StraysBookAPI.dto.user.AuthStatusDTO;
import info.dylansouthard.StraysBookAPI.dto.user.AuthTokenPairDTO;
import info.dylansouthard.StraysBookAPI.dto.user.UserPrivateDTO;
import info.dylansouthard.StraysBookAPI.errors.ErrorCodes;
import info.dylansouthard.StraysBookAPI.errors.ErrorFactory;
import info.dylansouthard.StraysBookAPI.mapper.UserMapper;
import info.dylansouthard.StraysBookAPI.model.user.User;
import info.dylansouthard.StraysBookAPI.service.AuthTokenService;
import info.dylansouthard.StraysBookAPI.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
@RestController
public class AuthTokenController {
    @Autowired
    private AuthTokenService authTokenService;

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private UserService userService;

    @GetMapping(ApiRoutes.AUTH.STATUS)
    @Operation(summary = "Check user authentication status", description = "Returns authentication status and user info if authenticated.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User status retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    public ResponseEntity<AuthStatusDTO> checkUserStatus(@AuthenticationPrincipal User user) {
        if (user == null) return ResponseEntity.ok(new AuthStatusDTO(false, null));
        UserPrivateDTO userDTO = userService.fetchMyProfile(user);
        return ResponseEntity.ok(new AuthStatusDTO(true, userDTO));
    }

    @PostMapping(ApiRoutes.AUTH.REFRESH)
    @Operation(summary = "Refresh access token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters. Error Code: INVALID_PARAMS"),
            @ApiResponse(responseCode = "401", description = "Forbidden. Error Code: " + ErrorCodes.INVALID_TOKEN),
            @ApiResponse(responseCode = "500", description = "Server error. Error Code: INTERNAL_SERVER_ERROR")
    })
    public ResponseEntity<AuthTokenPairDTO> refreshAccessToken(
            @RequestParam("refresh_token") String refreshToken,
            @RequestParam("device_id") String deviceId
    ) {
        AuthTokenPairDTO newAccessToken = authTokenService.refreshAccessToken(refreshToken, deviceId);
        return ResponseEntity.ok(newAccessToken);
    }

    @DeleteMapping(ApiRoutes.AUTH.REVOKE)
    @Operation(summary = "Revoke token for a specific device")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Token revoked successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters. Error Code: INVALID_PARAMS"),
            @ApiResponse(responseCode = "401", description = "Unauthorized. Error Code: AUTH"),
            @ApiResponse(responseCode = "401", description = "Invalid token. Error Code: " + ErrorCodes.INVALID_TOKEN),
            @ApiResponse(responseCode = "500", description = "Server error. Error Code: INTERNAL_SERVER_ERROR")
    })
    public ResponseEntity<Void> revokeDeviceToken(
            @RequestParam("device_id") String deviceId,
            @AuthenticationPrincipal User user
            ) {
        if (user == null) throw ErrorFactory.auth();
        authTokenService.revokeTokensForDevice(user, deviceId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping(ApiRoutes.AUTH.REVOKE_ALL)
    @Operation(summary = "Revoke all tokens for the current user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "All tokens revoked successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters. Error Code: " + ErrorCodes.INVALID_PARAMS),
            @ApiResponse(responseCode = "401", description = "Unauthorized. Error Code: AUTH"),
            @ApiResponse(responseCode = "500", description = "Server error. Error Code: " + ErrorCodes.INTERNAL_SERVER_ERROR)
    })
    public ResponseEntity<Void> revokeAllTokens(
            @AuthenticationPrincipal User user
    ) {
        if (user == null) throw ErrorFactory.auth();
        authTokenService.revokeAllTokensForUser(user);
        return ResponseEntity.noContent().build();
    }
}
