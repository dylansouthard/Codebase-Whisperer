package info.dylansouthard.StraysBookAPI.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import info.dylansouthard.StraysBookAPI.constants.PaginationConsts;
import info.dylansouthard.StraysBookAPI.dto.common.PaginatedResponseDTO;
import info.dylansouthard.StraysBookAPI.dto.notification.NotificationDTO;
import info.dylansouthard.StraysBookAPI.dto.notification.NotificationSyncDTO;
import info.dylansouthard.StraysBookAPI.errors.ErrorFactory;
import info.dylansouthard.StraysBookAPI.mapper.NotificationMapper;
import info.dylansouthard.StraysBookAPI.model.enums.NotificationContentType;
import info.dylansouthard.StraysBookAPI.model.enums.PriorityType;
import info.dylansouthard.StraysBookAPI.model.friendo.Animal;
import info.dylansouthard.StraysBookAPI.model.notification.AlertNotification;
import info.dylansouthard.StraysBookAPI.model.notification.UpdateNotification;
import info.dylansouthard.StraysBookAPI.model.user.User;
import info.dylansouthard.StraysBookAPI.repository.NotificationRepository;
import info.dylansouthard.StraysBookAPI.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class NotificationService {
    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationMapper notificationMapper;

    private final int NUM_DAYS_TO_CHECK = 30;
    @Autowired
    private UserRepository userRepository;

    @Transactional
    public UpdateNotification createUpdateNotification(Animal animal, Map<String, Object> payload, User registeredBy, NotificationContentType contentType) {
        UpdateNotification notification = new UpdateNotification(contentType, List.of(animal));
        ObjectMapper mapper = new ObjectMapper();
        notification.setNewValue(mapper.valueToTree(payload));
        notification.setRegisteredBy(registeredBy);
        return notificationRepository.save(notification);
    }

    @Transactional
    public AlertNotification createUpdateNotification(Animal animal, Map<String, Object> payload, User registeredBy, NotificationContentType contentType, PriorityType priority) {
        AlertNotification notification = new AlertNotification(contentType, List.of(animal), registeredBy, priority);
        ObjectMapper mapper = new ObjectMapper();
        notification.setNewValue(mapper.valueToTree(payload));
        notification.setRegisteredBy(registeredBy);
        return notificationRepository.save(notification);
    }

    @Transactional
    public PaginatedResponseDTO<NotificationDTO> getUpdateNotifications(Long userId, List<Long> animalIds, LocalDateTime lastChecked, Integer page, Integer pageSize) {
        if (userId == null || animalIds == null) throw ErrorFactory.invalidParams();
        LocalDateTime cutoffDate = lastChecked == null ? LocalDateTime.now().minusDays(NUM_DAYS_TO_CHECK) : lastChecked;
        Page<UpdateNotification> notificationDTOPage = notificationRepository.findUpdateNotifications(
                animalIds,
                userId,
                cutoffDate,
                PaginationConsts.notificationPageable(page, pageSize)
        );
        return notificationMapper.toPaginatedResponseDTO(notificationDTOPage);
    }

    @Transactional
    public List<NotificationDTO> getAlertNotifications(Long userId, List<Long> animalIds, LocalDateTime lastChecked) {
        if (userId == null || animalIds == null) throw ErrorFactory.invalidParams();
        LocalDateTime cutoffDate = lastChecked == null ? LocalDateTime.now().minusDays(NUM_DAYS_TO_CHECK) : lastChecked;
        List<AlertNotification> alertNotifications = notificationRepository.findAlertNotifications(animalIds, userId, cutoffDate);
        return alertNotifications.stream().map(notificationMapper::toNotificationDTO).toList();
    }

    @Transactional
    public NotificationSyncDTO syncNotifications(User passedUser, List<Long> animalIds, LocalDateTime lastChecked) {
        User user = userRepository.findActiveById(passedUser.getId()).orElseThrow(ErrorFactory::animalNotFound);
        List<Long> animalIdsToSync = animalIds;
        if (animalIdsToSync == null || animalIdsToSync.isEmpty()) {
            animalIdsToSync = user.getWatchedAnimals().stream().map(Animal::getId).toList();
        };
        Long userId = user.getId();
        PaginatedResponseDTO<NotificationDTO> updateNotifications = getUpdateNotifications(userId, animalIdsToSync, lastChecked, null, null);
        List<NotificationDTO> alertNotifications = getAlertNotifications(userId, animalIdsToSync, lastChecked);
        return new NotificationSyncDTO(updateNotifications, alertNotifications, LocalDateTime.now());
    }
}
