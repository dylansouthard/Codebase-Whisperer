package info.dylansouthard.StraysBookAPI.common;

import info.dylansouthard.StraysBookAPI.BaseTestContainer;
import info.dylansouthard.StraysBookAPI.config.DummyTestData;
import info.dylansouthard.StraysBookAPI.dto.careEvent.CreateSimpleCareEventDTO;
import info.dylansouthard.StraysBookAPI.model.CareEvent;
import info.dylansouthard.StraysBookAPI.model.enums.AnimalType;
import info.dylansouthard.StraysBookAPI.model.enums.CareEventType;
import info.dylansouthard.StraysBookAPI.model.enums.NotificationContentType;
import info.dylansouthard.StraysBookAPI.model.enums.SexType;
import info.dylansouthard.StraysBookAPI.model.friendo.Animal;
import info.dylansouthard.StraysBookAPI.model.notification.Notification;
import info.dylansouthard.StraysBookAPI.model.notification.UpdateNotification;
import info.dylansouthard.StraysBookAPI.model.user.User;
import info.dylansouthard.StraysBookAPI.repository.*;
import info.dylansouthard.StraysBookAPI.testutils.NotificationLoader;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

public class BaseDBTest extends BaseTestContainer {
    @PersistenceContext
    protected EntityManager entityManager;

    @Autowired
    protected AnimalRepository animalRepository;

    @Autowired
    protected AuthTokenRepository authTokenRepository;

    @Autowired
    protected CareEventRepository careEventRepository;

    @Autowired
    protected NotificationRepository notificationRepository;

    @Autowired
    protected LitterRepository litterRepository;

    @Autowired
    protected SterilizationStatusRepository sterilizationStatusRepository;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected VaccinationRepository vaccinationRepository;

    protected List<Animal> constructValidAnimals() {
        Animal animal1 = new Animal(AnimalType.CAT, SexType.FEMALE, "SabiSabi");
        Animal animal2 = new Animal(AnimalType.CAT, SexType.FEMALE, "Ame-chan");
        return List.of(animal1, animal2);
    }

    protected List<Animal> constructAndSaveValidAnimals() {
        List<Animal> animals = constructValidAnimals();
        for (Animal animal : animals) {animalRepository.saveAndFlush(animal);}
        return animals;
    }

    protected UpdateNotification constructValidConditionUpdateNotification() {
        List<Animal> animals = constructAndSaveValidAnimals();
        return new UpdateNotification(NotificationContentType.CONDITION_UPDATE, animals);
    }

    protected CreateSimpleCareEventDTO constructSimpleCEDTO(LocalDateTime specifiedDate) {
        CreateSimpleCareEventDTO createDTO = new CreateSimpleCareEventDTO();
        createDTO.setType(CareEventType.FED);
        if (specifiedDate != null) createDTO.setDate(specifiedDate);
        createDTO.setNotes("some notes");
        return createDTO;
    }

    protected CareEvent addAnimalAndSave(CareEvent event, Animal animal) {
        event.getAnimals().add(animal);
        return careEventRepository.saveAndFlush(event);
    }

    protected MockMultipartFile getCatImage() {
        return loadTestImage("cat.jpeg");
    }

    protected MockMultipartFile getNonImageFile() {
        byte[] invalidImageBytes = "not-an-image".getBytes(StandardCharsets.UTF_8);
        return new MockMultipartFile(
                "image",
                "notimage.txt",
                "text/plain", // clearly not image/jpeg or image/png
                invalidImageBytes
        );
    }

    protected MockMultipartFile getInvalidImage() {
        byte[] garbageData = "this-is-not-really-an-image".getBytes(StandardCharsets.UTF_8);
        return new MockMultipartFile(
                "image",
                "fakeimage.jpg",
                "image/jpeg", // Lies!
                garbageData
        );
    }

    private MockMultipartFile loadTestImage(String filename) {
        try (InputStream is = getClass().getResourceAsStream("/images/" + filename)) {
            if (is == null) {
                throw new IllegalArgumentException("Test image not found: " + filename);
            }
            return new MockMultipartFile("image", filename, "image/jpeg", is);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load test image: " + filename, e);
        }
    }

    protected <T extends Notification> List<T> constructAndSaveDummyNotifications(List<Animal> animals, User registeredBy, Class<T> clazz) throws IOException {
        if (animals.isEmpty()) return Collections.emptyList();

        List<T> notifications = NotificationLoader.loadDummyNotifications(clazz);

        for (int i = 0; i < notifications.size(); i++) {
            T notification = notifications.get(i);
            notification.setRegisteredBy(registeredBy);
            notification.setCreatedAt(LocalDateTime.now().minusDays(i));

            // Animal assignment logic
            if (animals.size() == 1) {
                notification.addAnimal(animals.get(0));
            } else {
                if (i % 2 == 0 || i % 3 == 0) {

                    notification.addAnimal(animals.get(0));
                }
                if (i % 2 != 0 || i % 3 == 0) {
                    notification.addAnimal(animals.get(1));
                }
            }
        }

        return notificationRepository.saveAll(notifications);
    }

    protected Animal createAnimalWithPrimaryCaretaker(User primaryCaretaker, User nonPrimaryCaretaker) {
        // Create Animal
        Animal animal = DummyTestData.createAnimal();
        animal.setRegisteredBy(primaryCaretaker);
        Animal savedAnimal = animalRepository.saveAndFlush(animal);

        // Add Care Events
        CareEvent event1 = addAnimalAndSave(new CareEvent(CareEventType.FED, LocalDateTime.now().minusDays(5), primaryCaretaker), savedAnimal);
        CareEvent event2 = addAnimalAndSave(new CareEvent(CareEventType.VACCINATED, LocalDateTime.now().minusDays(3), primaryCaretaker), savedAnimal);
        CareEvent event3 = addAnimalAndSave(new CareEvent(CareEventType.STERILIZED, LocalDateTime.now().minusDays(1), nonPrimaryCaretaker), savedAnimal);

        return animal;
    }

    protected Animal createAnimalWithCareEvents(User user) {
        // Create Animal
        Animal animal = DummyTestData.createAnimal();
        animal.setRegisteredBy(user);
        Animal savedAnimal = animalRepository.saveAndFlush(animal);

        // Add Care Events
        CareEvent event1 = addAnimalAndSave(new CareEvent(CareEventType.FED, LocalDateTime.now().minusDays(5), user), savedAnimal);
        CareEvent event2 = addAnimalAndSave(new CareEvent(CareEventType.VACCINATED, LocalDateTime.now().minusDays(3), user), savedAnimal);
        CareEvent event3 = addAnimalAndSave(new CareEvent(CareEventType.STERILIZED, LocalDateTime.now().minusDays(1), user), savedAnimal);

        return animal;
    }
}
