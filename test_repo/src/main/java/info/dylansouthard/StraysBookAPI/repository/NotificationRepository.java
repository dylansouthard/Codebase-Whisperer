package info.dylansouthard.StraysBookAPI.repository;

import info.dylansouthard.StraysBookAPI.model.notification.AlertNotification;
import info.dylansouthard.StraysBookAPI.model.notification.Notification;
import info.dylansouthard.StraysBookAPI.model.notification.UpdateNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    // Get high-priority notifications (priority 1 and 2) with a limit
    @Query("""
    SELECT DISTINCT n FROM UpdateNotification n
    JOIN n.animals a
    WHERE a.id IN :animalIds
    AND (n.registeredBy IS NULL OR n.registeredBy.id != :queryUserId)
    AND n.notificationDate > :lastChecked
    ORDER BY n.notificationDate DESC
    """)
    Page<UpdateNotification> findUpdateNotifications(
            @Param("animalIds") List<Long> animalIds,
            @Param("queryUserId") Long queryUserId,
            @Param("lastChecked") LocalDateTime lastChecked,
            Pageable pageable);

    @Query("""
        SELECT n FROM AlertNotification n
        JOIN n.animals a
        WHERE a.id IN :animalIds
        AND (n.registeredBy IS NULL OR n.registeredBy.id != :queryUserId)
        AND n.notificationDate > :lastChecked
        ORDER BY n.priority DESC
    """)
    List<AlertNotification> findAlertNotifications(
            @Param("animalIds") List<Long> animalIds,
            @Param("queryUserId") Long queryUserId,
            @Param("lastChecked") LocalDateTime lastChecked
    );

    @Query("""
        SELECT n FROM Notification n
        JOIN n.animals a
        WHERE a.id = :animalId
        ORDER BY n.notificationDate DESC
        """)
    Page<Notification> findNotificationsForAnimal(
            @Param("animalId") Long animalId,
            Pageable pageable);
}
