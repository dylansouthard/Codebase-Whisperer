package info.dylansouthard.StraysBookAPI.common;

import info.dylansouthard.StraysBookAPI.config.DummyTestData;
import info.dylansouthard.StraysBookAPI.model.friendo.Animal;
import info.dylansouthard.StraysBookAPI.model.notification.AlertNotification;
import info.dylansouthard.StraysBookAPI.model.notification.Notification;
import info.dylansouthard.StraysBookAPI.model.notification.UpdateNotification;
import info.dylansouthard.StraysBookAPI.model.user.User;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;
import java.util.Map;


public class BaseNotificationTest extends BaseDBTest {

    protected User registerUser;
    protected User queryUser;
    protected List<Animal> animals;
    protected List<UpdateNotification> allUpdateNotifications;
    protected List<AlertNotification> allAlertNotifications;
    protected List<Long> allAnimalIds;

    private Map<Class<? extends Notification>, List<? extends Notification>> notificationMap;

    @SuppressWarnings("unchecked")
    protected <T extends Notification> List<T> getNotificationsForAnimalOne(Class<T> clazz) {
        List<T> allNotifications = (List<T>) notificationMap.getOrDefault(clazz, List.of());
        return allNotifications.stream().filter(n -> n.getAnimals().contains(animals.get(0))).toList();
    }

    @BeforeEach
    public void setUpNotificationTests() throws Exception {
        registerUser = userRepository.save(DummyTestData.createUser());
        queryUser = userRepository.save(new User("Happy Sappy", "happpysappy@email.com"));
        animals = constructAndSaveValidAnimals();
        allAnimalIds = animals.stream().map(Animal::getId).toList();
        allUpdateNotifications = constructAndSaveDummyNotifications(animals, registerUser, UpdateNotification.class);
        allAlertNotifications = constructAndSaveDummyNotifications(animals, registerUser, AlertNotification.class);

        notificationMap = Map.of(
                AlertNotification.class, allAlertNotifications,
                UpdateNotification.class, allUpdateNotifications
        );
    }
}
