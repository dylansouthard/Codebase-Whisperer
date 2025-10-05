package info.dylansouthard.StraysBookAPI.mapper;

import info.dylansouthard.StraysBookAPI.dto.notification.NotificationDTO;
import info.dylansouthard.StraysBookAPI.dto.common.PaginatedResponseDTO;
import info.dylansouthard.StraysBookAPI.model.notification.Notification;
import info.dylansouthard.StraysBookAPI.model.notification.UpdateNotification;
import org.mapstruct.Mapper;
import org.springframework.data.domain.Page;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    NotificationDTO toNotificationDTO(Notification notification);

    default PaginatedResponseDTO<NotificationDTO> toPaginatedResponseDTO(Page<UpdateNotification> page) {
        return new PaginatedResponseDTO<>(
                page.getContent().stream().map(this::toNotificationDTO).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalPages(),
                page.getTotalElements(),
                page.hasNext(),
                page.hasPrevious()
        );
    }
}
