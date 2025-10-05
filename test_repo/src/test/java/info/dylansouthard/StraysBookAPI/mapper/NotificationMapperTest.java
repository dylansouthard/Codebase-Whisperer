package info.dylansouthard.StraysBookAPI.mapper;

import info.dylansouthard.StraysBookAPI.common.BaseDBTest;
import info.dylansouthard.StraysBookAPI.config.DummyTestData;
import info.dylansouthard.StraysBookAPI.dto.notification.NotificationDTO;
import info.dylansouthard.StraysBookAPI.dto.common.PaginatedResponseDTO;
import info.dylansouthard.StraysBookAPI.model.enums.NotificationContentType;
import info.dylansouthard.StraysBookAPI.model.friendo.Animal;
import info.dylansouthard.StraysBookAPI.model.notification.AlertNotification;
import info.dylansouthard.StraysBookAPI.model.notification.Notification;
import info.dylansouthard.StraysBookAPI.model.notification.UpdateNotification;
import info.dylansouthard.StraysBookAPI.model.user.User;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
public class NotificationMapperTest extends BaseDBTest {
    private final NotificationMapper notificationMapper = Mappers.getMapper(NotificationMapper.class);

    private Notification createNotificationWithUserAndAnimal(Notification notification){
        notification.setId(1L);
        Animal animal = DummyTestData.createAnimal();
        notification.getAnimals().add(animal);
        User user = DummyTestData.createUser();
        notification.setRegisteredBy(user);
        return notification;
    }

    private UpdateNotification createUpdateNotificationWithUserAndAnimal(){
        UpdateNotification updateNotification = new UpdateNotification();
        return (UpdateNotification) createNotificationWithUserAndAnimal(updateNotification);
    }

    private AlertNotification createAlertNotificationWithUserAndAnimal(){
        AlertNotification alertNotification = new AlertNotification();
        return (AlertNotification) createNotificationWithUserAndAnimal(alertNotification);
    }
//
    @Test
    void When_MappingToNotificationDTO_Then_ReturnNotificationDTO() {

        AlertNotification notification = createAlertNotificationWithUserAndAnimal();
        notification.setContentType(NotificationContentType.PROFILE_UPDATE);
        NotificationDTO dto = notificationMapper.toNotificationDTO(notification);


        assertAll("Notification DTO Assertions",
                ()->assertNotNull(dto, "NotificationDTO should not be null"),
                ()->assertEquals(1, dto.getAnimals().size(), "Notification animals count should be 1"),
                ()->assertNotNull(dto.getRegisteredBy(), "Notification RegisteredBy should not be null")
                );
    }

    @Test
    void When_MappingToCareEventNotificationDTO_Then_ReturnCareEventNotificationDTO() {
        Notification notification = createUpdateNotificationWithUserAndAnimal();
        notification.setContentType(NotificationContentType.CARE_EVENT);
        notification.setCareEvent(DummyTestData.createCareEvent());
        NotificationDTO dto = notificationMapper.toNotificationDTO(notification);
        assertNotNull(dto.getCareEvent(), "Notification CareEvent should not be null");
    }

    @Test
    void shouldMapPageToPaginatedResponseDTO() {
        // Create concrete notifications
        UpdateNotification notification1 = new UpdateNotification();
        notification1.setId(1L);
        UpdateNotification notification2 = new UpdateNotification();
        notification2.setId(2L);

        // Create a list of notifications
        List<UpdateNotification> notifications = List.of(notification1, notification2);

        // Use PageRequest to correctly specify the page size
        PageRequest pageRequest = PageRequest.of(0, 10); // First page, size 10

        // Use the correct total number of elements and simulate a single page
        Page<UpdateNotification> notificationPage = new PageImpl<>(notifications, pageRequest, notifications.size());

        // Map to PaginatedResponseDTO using the mapper
        PaginatedResponseDTO<NotificationDTO> response = notificationMapper.toPaginatedResponseDTO(notificationPage);

        // Perform assertions
        assertAll(
                "Notification Mapper units",
                () -> assertEquals(2, response.getContent().size(),
                        "Expected number of notifications in the content does not match the actual size."),
                () -> assertEquals(0, response.getPageNumber(),
                        "Expected page number does not match the actual page number."),
                () -> assertEquals(10, response.getPageSize(),
                        "Expected page size does not match the actual page size."),
                () -> assertEquals(2, response.getTotalElements(),
                        "Expected total elements do not match the actual number of elements."),
                () -> assertEquals(1, response.getTotalPages(),
                        "Expected total pages do not match the actual total pages."),
                () -> assertFalse(response.isHasNext(),
                        "Expected no next page, but response indicates there is one."),
                () -> assertFalse(response.isHasPrevious(),
                        "Expected no previous page, but response indicates there is one.")
        );
    }


}
