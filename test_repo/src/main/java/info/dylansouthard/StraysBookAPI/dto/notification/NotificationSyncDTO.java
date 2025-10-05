package info.dylansouthard.StraysBookAPI.dto.notification;

import info.dylansouthard.StraysBookAPI.dto.common.PaginatedResponseDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter
public class NotificationSyncDTO {
    @Schema(
        description = "Paginated list of update notifications for non-critical changes (e.g., name change, photo update)",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private PaginatedResponseDTO<NotificationDTO> updateNotifications;

    @Schema(
        description = "List of critical alert notifications (e.g., illness, injury) which should be reviewed immediately",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private List<NotificationDTO> alertNotifications = new ArrayList<>();

    @Schema(
        description = "Timestamp indicating when the notification sync response was generated",
        example = "2025-05-21T23:42:15",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private LocalDateTime returnedAt;
}
