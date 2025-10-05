package info.dylansouthard.StraysBookAPI.repository;

import info.dylansouthard.StraysBookAPI.model.CareEvent;
import info.dylansouthard.StraysBookAPI.model.notification.Notification;
import info.dylansouthard.StraysBookAPI.model.enums.CareEventType;
import info.dylansouthard.StraysBookAPI.model.friendo.Animal;
import info.dylansouthard.StraysBookAPI.model.friendo.Litter;
import info.dylansouthard.StraysBookAPI.model.user.User;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class CareEventRepositoryIT extends RepositoryIT {

    //CREATE
    @Test
    @Transactional
    public void When_DeletingCareEvent_Expect_CareEventDeletedFromUserAndAnimals() {
        // ✅ Save user & animal first
        Animal animal = animalRepository.saveAndFlush(validAnimal);
        User user = userRepository.saveAndFlush(validUser);

        // ✅ Create and associate the care event
        CareEvent careEvent = new CareEvent(CareEventType.FED, LocalDateTime.now(), user);
        careEvent.addAnimal(animal);

        // ✅ save care event (after relationship is established)
        CareEvent savedCareEvent = careEventRepository.saveAndFlush(careEvent);
        animalRepository.saveAndFlush(animal);

        // ✅ Retrieve data to verify
        Animal savedAnimal = animalRepository.findById(animal.getId()).orElseThrow();
        User savedUser = userRepository.findById(user.getId()).orElseThrow();

        assertAll(
                "Care event save assertions",
                () -> assertNotNull(savedCareEvent, "Care Event should exist"),
                () -> assertEquals(1, savedCareEvent.getAnimals().size(), "Care event should have animal"),
                () -> assertNotNull(savedCareEvent.getRegisteredBy(), "Care event should have a user"),
                () -> assertEquals(1, savedAnimal.getCareEvents().size(), "Animal should have care event"),
                () -> assertEquals(1, savedUser.getCareEvents().size(), "User should have care event")
        );

        careEventRepository.delete(savedCareEvent);
        Animal foundAnimal = animalRepository.findById(animal.getId()).orElseThrow();
        Optional<CareEvent> deletedCareEvent = careEventRepository.findById(savedCareEvent.getId());
        assertAll(
                "Care event delete assertions",
                ()->assertTrue(deletedCareEvent.isEmpty(), "Care event should be empty"),
                ()->assertEquals(0, foundAnimal.getCareEvents().size(), "Animal Care Events should be empty")
                );
    }

    @Test
    @Transactional
    public void When_AddLitterToCareEvent_Expect_AnimalsAddedToCareEvent() {
        User user = userRepository.saveAndFlush(validUser);
        Litter litter = litterRepository.saveAndFlush(constructValidLitter());
        CareEvent careEvent = new CareEvent(CareEventType.FED, LocalDateTime.now(), user);
        careEvent.addLitter(litter);

        assertAll("Litter CE Assertions",
                ()->assertNotNull(careEvent.getLitter(),"Litter should be added to CE"),
                ()->assertEquals(litter.getAnimals().size(), careEvent.getAnimals().size(), "Litter should have the same number of animals")
                );
    }

    @Test
    @Transactional
    public void When_CareEventRelationsDeleted_Expect_RelationsRemovedFromCareEvent() {
        Animal animal = animalRepository.saveAndFlush(validAnimal);
        User user = userRepository.saveAndFlush(validUser);
        Litter litter = litterRepository.saveAndFlush(constructValidLitter());

        CareEvent careEvent = new CareEvent(CareEventType.FED, LocalDateTime.now(), user);
        careEvent.addLitter(litter);
        careEvent.addAnimal(animal);

        CareEvent savedCareEvent = careEventRepository.saveAndFlush(careEvent);
        animalRepository.saveAndFlush(animal);

        userRepository.deleteById(user.getId());
        animalRepository.deleteById(animal.getId());

        CareEvent retrievedCareEvent = careEventRepository.findById(savedCareEvent.getId()).orElseThrow();

        assertAll("CE rel del assertions",
                ()->assertEquals(litter.getAnimals().size(), retrievedCareEvent.getAnimals().size(), "Animal should be removed from care event"),
                ()->assertNull(careEvent.getRegisteredBy(), "Deleted user should be null")
                );
    }

    @Test
    @Transactional
    public void When_CareEventCreated_Expect_NotificationSaved() {
        User user = userRepository.saveAndFlush(validUser);
        Animal animal = animalRepository.saveAndFlush(validAnimal);
        CareEvent careEvent = new CareEvent(CareEventType.FED, LocalDateTime.now(), user);
        careEventRepository.saveAndFlush(careEvent);
        Optional<Notification> foundNotification = notificationRepository.findById(careEvent.getNotification().getId());

        assertTrue(foundNotification.isPresent(), "Notification should exist");
    }


    @Test
    @Transactional
    public void When_FetchingCareEventsForAnimalByDate_Expect_CareEventsFetched() {
        User user = userRepository.saveAndFlush(validUser);
        Animal animal = animalRepository.saveAndFlush(validAnimal);
        CareEvent careEventOne = addAnimalAndSave(new CareEvent(CareEventType.FED, LocalDateTime.now(), user), animal);
        CareEvent careEventTwo = addAnimalAndSave(new CareEvent(CareEventType.VET, LocalDateTime.now().minusDays(29), user), animal);
        CareEvent careEventThree = addAnimalAndSave(new CareEvent(CareEventType.VACCINATED, LocalDateTime.now().minusDays(31), user), animal);
        CareEvent careEventFour = careEventRepository.saveAndFlush(new CareEvent(CareEventType.PLAYED, LocalDateTime.now(), user));

        List<CareEvent> fetchedCareEvents = careEventRepository.findRecentCareEventsByAnimalId(animal.getId(), LocalDateTime.now().minusDays(30));

        assertAll("Fetched Care Events",
                () -> assertTrue(fetchedCareEvents.contains(careEventOne), "Should include careEventOne (now)"),
                () -> assertTrue(fetchedCareEvents.contains(careEventTwo), "Should include careEventTwo (29 days ago)"),
                () -> assertFalse(fetchedCareEvents.contains(careEventThree), "Should NOT include careEventThree (31 days ago)"),
                () -> assertFalse(fetchedCareEvents.contains(careEventFour), "Should NOT include careEventFour (not linked to animal)"),
                () -> assertEquals(2, fetchedCareEvents.size(), "Should fetch exactly 2 care events")
        );
    }
}
