package info.dylansouthard.StraysBookAPI.controller;

import info.dylansouthard.StraysBookAPI.constants.ApiRoutes;
import info.dylansouthard.StraysBookAPI.dto.notification.NotificationSyncDTO;
import info.dylansouthard.StraysBookAPI.errors.ErrorFactory;
import info.dylansouthard.StraysBookAPI.model.user.User;
import info.dylansouthard.StraysBookAPI.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
public class NotificationController {
    @Autowired
    private NotificationService notificationService;

    @GetMapping(ApiRoutes.NOTIFICATIONS.SYNC)
    @Operation(
            summary = "Sync recent notifications for given animals",
            description = "Returns all recent alert notifications and a paginated list of update notifications for the specified animal IDs and user."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved notifications"),
            @ApiResponse(responseCode = "400", description = "Missing or invalid parameters. Error Code: INVALID_PARAMS"),
            @ApiResponse(responseCode = "401", description = "Unauthorized. Error Code: AUTH"),
            @ApiResponse(responseCode = "500", description = "Server error. Error Code: INTERNAL_SERVER_ERROR")
    })
    public ResponseEntity<NotificationSyncDTO> syncNotifications(
            @RequestParam(required = false) List<Long> animalIds,

            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            @RequestParam(name="last_checked", required = false) LocalDateTime lastChecked,

            @AuthenticationPrincipal User user
    ) {
        if (user == null) throw ErrorFactory.auth();
        NotificationSyncDTO res = notificationService.syncNotifications(user, animalIds, lastChecked);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }
}
