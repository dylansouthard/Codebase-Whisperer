package info.dylansouthard.StraysBookAPI.service;

import info.dylansouthard.StraysBookAPI.common.BaseDBTest;
import info.dylansouthard.StraysBookAPI.config.DummyTestData;
import info.dylansouthard.StraysBookAPI.dto.careEvent.CareEventDTO;
import info.dylansouthard.StraysBookAPI.dto.careEvent.CreateSimpleCareEventDTO;
import info.dylansouthard.StraysBookAPI.dto.common.PaginatedResponseDTO;
import info.dylansouthard.StraysBookAPI.model.CareEvent;
import info.dylansouthard.StraysBookAPI.model.enums.CareEventType;
import info.dylansouthard.StraysBookAPI.model.friendo.Animal;
import info.dylansouthard.StraysBookAPI.model.notification.Notification;
import info.dylansouthard.StraysBookAPI.model.user.User;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static info.dylansouthard.StraysBookAPI.config.DummyTestData.createAnimal;
import static info.dylansouthard.StraysBookAPI.config.DummyTestData.createUser;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class CareEventServiceIT extends BaseDBTest {

    @Autowired
    private CareEventService careEventService;

    @Test
    @Transactional
    public void When_GetAllRecentCareEventsByAnimalId_Expect_ProperCareEventsReturned() {
        User user = userRepository.saveAndFlush(createUser());
        Animal animal = animalRepository.saveAndFlush(createAnimal());

        CareEvent careEventOne = addAnimalAndSave(new CareEvent(CareEventType.FED, LocalDateTime.now(), user), animal);
        CareEvent careEventTwo = addAnimalAndSave(new CareEvent(CareEventType.VET, LocalDateTime.now().minusDays(29), user), animal);
        CareEvent careEventThree = addAnimalAndSave(new CareEvent(CareEventType.VACCINATED, LocalDateTime.now().minusDays(31), user), animal);
        CareEvent careEventFour = careEventRepository.saveAndFlush(new CareEvent(CareEventType.PLAYED, LocalDateTime.now(), user)); // ❌ Not linked to animal

        List<CareEvent> fetchedCareEvents = careEventService.getAllRecentCareEventsByAnimalId(animal.getId());

        assertAll("Fetched Care Events (via Service)",
                () -> assertTrue(fetchedCareEvents.contains(careEventOne), "Should include careEventOne (now)"),
                () -> assertTrue(fetchedCareEvents.contains(careEventTwo), "Should include careEventTwo (29 days ago)"),
                () -> assertFalse(fetchedCareEvents.contains(careEventThree), "Should NOT include careEventThree (31 days ago)"),
                () -> assertFalse(fetchedCareEvents.contains(careEventFour), "Should NOT include careEventFour (not linked to animal)"),
                () -> assertEquals(2, fetchedCareEvents.size(), "Should fetch exactly 2 care events")
        );
    }

    @Transactional
    @Test
    public void When_CreateSimpleCareEvent_Expect_ProperCareEventsReturned() {
        User user = userRepository.saveAndFlush(createUser());
        Animal animal = animalRepository.saveAndFlush(createAnimal());
        LocalDateTime specifiedDate = LocalDateTime.now().minusDays(20);
        CreateSimpleCareEventDTO createDTO = constructSimpleCEDTO(specifiedDate);

        CareEventDTO savedEvent = careEventService.createSimpleCareEvent(createDTO, animal.getId(), user.getId());
        Animal fetchedAnimal = animalRepository.findById(animal.getId()).get();
        Set<Notification> notifications = fetchedAnimal.getAssociatedNotifications();

        assertAll("simple care event assertions",
                ()->assertEquals(1, savedEvent.getAnimals().size()),
                ()->assertEquals(specifiedDate, savedEvent.getDate()),
                ()->assertEquals("some notes", savedEvent.getNotes()),
                ()->assertEquals(CareEventType.FED, savedEvent.getType()),
                ()->assertEquals(1, fetchedAnimal.getCareEvents().size()),
                ()->assertEquals(1, notifications.size())
        );
    }

    @Transactional
    @Test
    public void When_FetchCareEventById_Expect_ProperCareEventsReturned() {
        User user = userRepository.saveAndFlush(DummyTestData.createUser());
        Animal animal = createAnimalWithCareEvents(user);

        PaginatedResponseDTO<CareEventDTO> careEvents = careEventService.getCareEventsForAnimal(animal.getId(), null, null);


        assertAll("Care Events for Animal Asseertion",
                ()->assertEquals(3, careEvents.getContent().size())
                );
    }

}