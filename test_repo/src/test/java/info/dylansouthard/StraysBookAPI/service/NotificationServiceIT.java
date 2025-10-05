package info.dylansouthard.StraysBookAPI.service;

import info.dylansouthard.StraysBookAPI.common.BaseNotificationTest;
import info.dylansouthard.StraysBookAPI.constants.PaginationConsts;
import info.dylansouthard.StraysBookAPI.dto.common.PaginatedResponseDTO;
import info.dylansouthard.StraysBookAPI.dto.notification.NotificationDTO;
import info.dylansouthard.StraysBookAPI.dto.notification.NotificationSyncDTO;
import info.dylansouthard.StraysBookAPI.model.notification.AlertNotification;
import info.dylansouthard.StraysBookAPI.model.notification.UpdateNotification;
import info.dylansouthard.StraysBookAPI.testutils.NotificationLoader;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class NotificationServiceIT extends BaseNotificationTest {

    @Autowired
    private NotificationService notificationService;

    @Test
    public void When_FetchingUpdateNotifications_ShouldReturnNotifications() {
        LocalDateTime lastChecked = LocalDateTime.now().minusDays(50);
        PaginatedResponseDTO<NotificationDTO> firstPageResults = notificationService.getUpdateNotifications(queryUser.getId(), allAnimalIds, lastChecked, null, null);
        PaginatedResponseDTO<NotificationDTO> secondPageResults = notificationService.getUpdateNotifications(queryUser.getId(), allAnimalIds, lastChecked, 1, null);
        PaginatedResponseDTO<NotificationDTO> bigPageResults = notificationService.getUpdateNotifications(queryUser.getId(), allAnimalIds, lastChecked, 0, 300);
        int dps = PaginationConsts.DEFAULT_NOTIFICATION_PAGE_SIZE;

        assertAll(
                "Update Notification Service Assertions",

                // First page assertions
                () -> assertEquals(dps, firstPageResults.getContent().size(), "First page should contain the default number of update notifications"),
                () -> assertEquals(0, firstPageResults.getPageNumber(), "First page number should be 0"),
                () -> assertEquals(dps, firstPageResults.getPageSize(), "Page size should match default"),
                () -> assertEquals(allUpdateNotifications.size(), firstPageResults.getTotalElements(), "Total elements should match all update notifications"),
                () -> assertTrue(firstPageResults.getTotalPages() >= 1, "Total pages should be at least 1"),
                () -> assertTrue(firstPageResults.isHasNext() || firstPageResults.getTotalPages() == 1, "Should have next page unless only one exists"),
                () -> assertFalse(firstPageResults.isHasPrevious(), "First page should not have a previous page"),

                // Second page assertions
                () -> assertEquals(1, secondPageResults.getPageNumber(), "Second page number should be 1"),
                () -> assertEquals(dps, secondPageResults.getPageSize(), "Second page size should match default"),
                () -> assertTrue(secondPageResults.getContent().size() <= dps, "Second page should not exceed default page size"),
                () -> assertTrue(secondPageResults.isHasPrevious(), "Second page should have a previous page"),
                () -> assertTrue(secondPageResults.isHasNext() || secondPageResults.getPageNumber() == secondPageResults.getTotalPages() - 1, "HasNext depends on remaining pages"),

                // Big page assertions
                () -> assertEquals(allUpdateNotifications.size(), bigPageResults.getContent().size(), "Big page should return all notifications if under limit"),
                () -> assertEquals(0, bigPageResults.getPageNumber(), "Big page should be page 0"),
                () -> assertEquals(300, bigPageResults.getPageSize(), "Big page size should reflect requested size"),
                () -> assertEquals(1, bigPageResults.getTotalPages(), "If all fit in one big page, total pages should be 1"),
                () -> assertFalse(bigPageResults.isHasNext(), "Big page should not have a next page"),
                () -> assertFalse(bigPageResults.isHasPrevious(), "Big page should not have a previous page")
        );
    }

    @Test
    public void When_FetchingAlertNotifications_ShouldReturnAlertNotifications() {
        LocalDateTime lastChecked = LocalDateTime.now().minusDays(50);
        List<NotificationDTO> results = notificationService.getAlertNotifications(queryUser.getId(), allAnimalIds, lastChecked);
        assertEquals(allAlertNotifications.size(), results.size(), "the results should return all alert notifications ");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void When_FetchingNotificationSync_ShouldReturnNotificationSync(boolean loadFromWatched) throws IOException {
        int numDaysAgo = 21;
        LocalDateTime lastChecked = LocalDateTime.now().minusDays(numDaysAgo);

        List<Long> animalIds = allAnimalIds;

        if (loadFromWatched) {
            animalIds = null;
            animals.forEach(animal -> {
                queryUser.addWatchedAnimal(animal);
                userRepository.save(queryUser);
            });
        }

        NotificationSyncDTO results = notificationService.syncNotifications(queryUser, animalIds, lastChecked);
        int fullUpdateNotificationSize = NotificationLoader.loadDummyNotifications(UpdateNotification.class).size();
        int fullAlertNotificationSize = NotificationLoader.loadDummyNotifications(AlertNotification.class).size();
        int pageSize = PaginationConsts.DEFAULT_NOTIFICATION_PAGE_SIZE;

        assertAll("sync notification assertions",
                () -> assertEquals(
                        Math.min(pageSize, Math.min(numDaysAgo, fullUpdateNotificationSize)),
                        results.getUpdateNotifications().getContent().size(),
                        "should return update notifications filtered by lastChecked and capped by page size"
                ),
                () -> assertEquals(
                        (Math.min(numDaysAgo, fullUpdateNotificationSize) + pageSize - 1) / pageSize,
                        results.getUpdateNotifications().getTotalPages(),
                        "should correctly calculate total pages for update notifications based on filtered count"
                ),
                () -> assertEquals(
                        Math.min(numDaysAgo, fullAlertNotificationSize),
                        results.getAlertNotifications().size(),
                        "should return all alert notifications newer than lastChecked without pagination"
                ),
                () -> assertNotNull(
                        results.getReturnedAt(),
                        "should include returnedAt timestamp in response"
                ),
                () -> assertTrue(
                        results.getReturnedAt().isAfter(lastChecked),
                        "returnedAt should be after the lastChecked timestamp"
                ),
                () -> assertTrue(
                        results.getUpdateNotifications().getContent().stream()
                                .allMatch(n -> n.getNotificationDate().isAfter(lastChecked)),
                        "all returned update notifications should be newer than lastChecked"
                )
        );
    }

    @Test
    public void When_SyncingNotificationsWithNoAnimals_Then_Return_Empty_List () {
        int numDaysAgo = 21;
        LocalDateTime lastChecked = LocalDateTime.now().minusDays(numDaysAgo);
        NotificationSyncDTO results = notificationService.syncNotifications(queryUser, null, lastChecked);
        assertAll("empty notification sync",
                ()->assertTrue(results.getUpdateNotifications().getContent().isEmpty(), "Update notifications should be empty"),
                ()->assertTrue(results.getAlertNotifications().isEmpty(), "Alert Notifications Should be empty")
                );
    }
}
