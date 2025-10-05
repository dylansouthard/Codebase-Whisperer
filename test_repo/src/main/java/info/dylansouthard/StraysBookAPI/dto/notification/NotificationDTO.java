package info.dylansouthard.StraysBookAPI.dto.notification;

import info.dylansouthard.StraysBookAPI.dto.careEvent.CareEventSummaryDTO;
import info.dylansouthard.StraysBookAPI.dto.friendo.AnimalSummaryMinDTO;
import info.dylansouthard.StraysBookAPI.dto.user.UserSummaryMinDTO;
import info.dylansouthard.StraysBookAPI.model.enums.NotificationContentType;
import info.dylansouthard.StraysBookAPI.model.enums.NotificationType;
import info.dylansouthard.StraysBookAPI.model.enums.PriorityType;
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
@Getter
@Setter
public class NotificationDTO {

    @Schema(
            description = "Unique ID of the notification",
            example = "123",
            nullable = false,
            requiredMode = Schema.RequiredMode.REQUIRED,
            type = "integer"
    )
    private Long id;

    @Schema(
            description = "Type of notification content (e.g., CARE_EVENT, STATUS_UPDATE, NOTE)",
            example = "CARE_EVENT",
            nullable = false,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private NotificationContentType contentType;

    @Schema(
            description = "Type of notification",
            example = "ALERT",
            nullable = false,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private NotificationType type;

    @Schema(
            description = "Priority level of the notification (e.g., LOW, MEDIUM, HIGH)",
            example = "MEDIUM",
            nullable = true,
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private PriorityType priority;

    @Schema(
            description = "List of animals associated with this notification",
            nullable = false,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private List<AnimalSummaryMinDTO> animals = new ArrayList<>();

    @Schema(
            description = "Summary of the care event linked to this notification, if applicable",
            nullable = true,
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private CareEventSummaryDTO careEvent;

    @Schema(
            description = "New value associated with this notification, such as a status change or update note",
            example = "Vaccinated",
            nullable = true,
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private Object newValue;

    @Schema(
            description = "User who registered the notification",
            nullable = false,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private UserSummaryMinDTO registeredBy;

    @Schema(
            description = "Timestamp when the notification was created, in ISO 8601 format",
            example = "2025-03-16T14:30:00",
            nullable = false,
            requiredMode = Schema.RequiredMode.REQUIRED,
            type = "string",
            format = "date-time"
    )
    private LocalDateTime createdAt;

    @Schema(
            description = "Timestamp when the check was returned, in ISO 8601 format",
            example = "2025-03-16T14:30:00",
            nullable = true,
            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            type = "string",
            format = "date-time"
    )
    private LocalDateTime returnedAt;

    @Schema(
            description = "Optional notes or comments about the notification",
            example = "Animal showed improvement after treatment.",
            nullable = true,
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private String notes;

    @Schema(
            description = "Date and time the event occurred or is considered to have occurred (may differ from createdAt if backdated or scheduled)",
            example = "2025-06-24T09:00:00",
            nullable = false,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private LocalDateTime notificationDate;

}
