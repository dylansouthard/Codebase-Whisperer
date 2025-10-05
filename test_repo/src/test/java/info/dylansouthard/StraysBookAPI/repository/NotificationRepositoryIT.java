package info.dylansouthard.StraysBookAPI.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import info.dylansouthard.StraysBookAPI.common.BaseNotificationTest;
import info.dylansouthard.StraysBookAPI.constants.PaginationConsts;
import info.dylansouthard.StraysBookAPI.model.enums.ConditionType;
import info.dylansouthard.StraysBookAPI.model.friendo.Animal;
import info.dylansouthard.StraysBookAPI.model.notification.AlertNotification;
import info.dylansouthard.StraysBookAPI.model.notification.Notification;
import info.dylansouthard.StraysBookAPI.model.notification.UpdateNotification;
import info.dylansouthard.StraysBookAPI.testutils.TestConveniences;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
public class NotificationRepositoryIT extends BaseNotificationTest {

    private static final Logger logger = LoggerFactory.getLogger(NotificationRepositoryIT.class);

    //region CREATE =======================================
    @Test
    public void When_SavingNotification_Expect_NotificationIsRetrievable() {
        UpdateNotification notification = constructValidConditionUpdateNotification();
        notification.setNewValue(ConditionType.UNDERFED.name());
        UpdateNotification savedNotification = notificationRepository.save(notification);
        ObjectMapper mapper = new ObjectMapper();

        Optional<Notification> fetchedNotification = notificationRepository.findById(savedNotification.getId());
        assertAll("Notification Save Assertions",
                ()->assertTrue(fetchedNotification.isPresent(), "Saved Notification should be retrievable."),
                ()->assertEquals(mapper.valueToTree(ConditionType.UNDERFED.name()), fetchedNotification.get().getNewValue())

        );
    }

    @Test
    @Transactional
    public void When_SavingInvalidNotification_Expect_ThrowException() {
        UpdateNotification notification = new UpdateNotification();
        assertThrows(DataIntegrityViolationException.class, () -> notificationRepository.save(notification), "Saving invalid Notification should throw an error");
    }
//endregion

    //region READ ========================================

    @Test
    public void When_FetchingUpdateNotifications_Expect_NotificationsAreRetrievable() throws IOException {
        List<UpdateNotification> allNotifications = allUpdateNotifications;
        List<UpdateNotification> notificationsForAnimalOne = getNotificationsForAnimalOne(UpdateNotification.class);
        LocalDateTime lastChecked = LocalDateTime.now().minusDays(50);

        Page<UpdateNotification> rNotifications = notificationRepository.findUpdateNotifications(
                allAnimalIds,
                queryUser.getId(),
                lastChecked,
                PaginationConsts.notificationPageable()
        );

        Page<UpdateNotification> sameUserNotifications = notificationRepository.findUpdateNotifications(
                allAnimalIds,
                registerUser.getId(),
                lastChecked,
                PaginationConsts.notificationPageable()
        );

        Page<UpdateNotification> unknownAnimalNotifications = notificationRepository.findUpdateNotifications(
                List.of(678L),
                registerUser.getId(),
                lastChecked,
                PaginationConsts.notificationPageable()
        );

        int expectedPageNum = TestConveniences.getTotalPages(allNotifications.size(), PaginationConsts.DEFAULT_NOTIFICATION_PAGE_SIZE);

        Page<Notification> notificationsForAnimal = notificationRepository.findNotificationsForAnimal(allAnimalIds.getFirst(), PaginationConsts.notificationForAnimalProfile());

        assertAll(
                "find notification assertions",
                () -> assertEquals(
                        Math.min(allNotifications.size(), PaginationConsts.DEFAULT_NOTIFICATION_PAGE_SIZE),
                        rNotifications.getContent().size(),
                        "Rendered notifications should match expected page size or total notifications in DB, whichever is smaller"
                ),
                () -> assertEquals(
                        0,
                        sameUserNotifications.getContent().size(),
                        "Notifications created by the requesting user should be excluded"
                ),
                () -> assertEquals(
                        0,
                        unknownAnimalNotifications.getContent().size(),
                        "Notifications without valid animal IDs should not be returned"
                ),
                () -> assertFalse(
                        notificationsForAnimal.isEmpty(),
                        "Notifications for the selected animal should be present"
                ),
                () -> assertEquals(
                        Math.min(notificationsForAnimalOne.size(), PaginationConsts.DEFAULT_NOTIFICATION_PAGE_SIZE),
                        notificationsForAnimal.getContent().size(),
                        "Paginated animal notifications should match expected page size"
                ),
                () -> assertEquals(
                        expectedPageNum,
                        rNotifications.getTotalPages(),
                        "Total pages should match the expected page count"
                ),
                () -> assertEquals(
                        expectedPageNum > 1,
                        rNotifications.hasNext(),
                        "hasNext should be true if there is more than one page"
                ),
                () -> assertFalse(
                        rNotifications.hasPrevious(),
                        "hasPrevious should be false on the first page"
                )
        );
    }

    @Test
    public void When_FetchingAlertNotifications_Expect_NotificationsAreRetrievable() throws IOException {
        LocalDateTime lastChecked = LocalDateTime.now().minusDays(10);
        List<AlertNotification> allNotifications = allAlertNotifications;
        List<AlertNotification> recentNotifications = allNotifications.stream()
                .filter(n -> n.getCreatedAt().isAfter(lastChecked))
                .toList();
        List<AlertNotification> notificationsForAnimalOne = recentNotifications.stream().filter(n -> n.getAnimals().contains(animals.get(0))).toList();


        List<AlertNotification> rNotifications = notificationRepository.findAlertNotifications(
                allAnimalIds,
                queryUser.getId(),
                lastChecked
        );

        List<AlertNotification> sameUserNotifications = notificationRepository.findAlertNotifications(
                allAnimalIds,
                registerUser.getId(),
                lastChecked
        );

        List<AlertNotification> unknownAnimalNotifications = notificationRepository.findAlertNotifications(
                List.of(678L),
                registerUser.getId(),
                lastChecked
        );

        int expectedPageNum = TestConveniences.getTotalPages(allNotifications.size(), PaginationConsts.HP_NOTIFICATION_PAGE_SIZE);

        Page<Notification> notificationsForAnimal = notificationRepository.findNotificationsForAnimal(allAnimalIds.getFirst(), PaginationConsts.notificationForAnimalProfile());

        assertAll(
                "find notification assertions",
                () -> assertEquals(recentNotifications.size(), rNotifications.size(), "LP notification should equal those in db"),
                () -> assertEquals(0, sameUserNotifications.size(), "Notifications created by user should not return"),
                () -> assertEquals(0, unknownAnimalNotifications.size(), "Notifications without animal ID should not return"),
                () -> assertFalse(notificationsForAnimal.isEmpty(), "animal notifications should not be empty"),
                () -> assertEquals(notificationsForAnimalOne.size(), notificationsForAnimal.getContent().size())
        );
    }

    //endregion
    //region UPDATE =======================================
    @Test
    @Transactional
    public void When_UpdatingNotification_Expect_NotificationIsUpdated() {
        UpdateNotification notification = notificationRepository.save(constructValidConditionUpdateNotification());

        notification.setNotes("test");

        notificationRepository.save(notification);

        Notification fetchedNotification = notificationRepository.findById(notification.getId()).get();

        assertEquals("test", fetchedNotification.getNotes(), "Notification should be updated");
    }
//
    @Test
    @Transactional
    public void When_AddingAnimalsToNotification_Expect_NotificationIsAdded() {
        Notification notification = notificationRepository.save(constructValidConditionUpdateNotification());
        Notification fetchedNotification = notificationRepository.findById(notification.getId()).get();
        Animal fetchedAnimal = animalRepository.findById(notification.getAnimals().stream().findFirst().get().getId()).orElseThrow();

        assertAll("Notification animal relationship assertions",
                ()->assertEquals(2, fetchedNotification.getAnimals().size(), "Animals should be saved to notification"),
                ()-> assertEquals(1, fetchedAnimal.getAssociatedNotifications().size(), "Notification should be saved to animal")
                );
    }

    @Test
    @Transactional
    public void When_RemovingAnimalsFromNotification_Expect_NotificationIsRemoved() {
        Notification notification = notificationRepository.save(constructValidConditionUpdateNotification());
        Animal animal = notification.getAnimals().stream().findFirst().get();
        notification.removeAnimal(animal);
        assertEquals(1, notification.getAnimals().size(), "Animals should be removed from notification before saving");
        notificationRepository.saveAndFlush(notification);

        Notification fetchedNotification = notificationRepository.findById(notification.getId()).get();
        Animal fetchedAnimal = animalRepository.findById(animal.getId()).orElseThrow();

        assertAll("Notification animal relationship assertions",
                ()->assertEquals(1, fetchedNotification.getAnimals().size(), "Animals should be removed from notification"),
                ()-> assertEquals(0, fetchedAnimal.getAssociatedNotifications().size(), "Notification should be removed from animal")
        );
    }

    //endregion
}
