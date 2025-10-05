package info.dylansouthard.StraysBookAPI.service;

import info.dylansouthard.StraysBookAPI.cases.AnimalUpdateTestCase;
import info.dylansouthard.StraysBookAPI.common.BaseDBTest;
import info.dylansouthard.StraysBookAPI.config.DummyTestData;
import info.dylansouthard.StraysBookAPI.dto.friendo.*;
import info.dylansouthard.StraysBookAPI.errors.ErrorFactory;
import info.dylansouthard.StraysBookAPI.mapper.AnimalMapper;
import info.dylansouthard.StraysBookAPI.model.CareEvent;
import info.dylansouthard.StraysBookAPI.model.enums.AnimalType;
import info.dylansouthard.StraysBookAPI.model.enums.CareEventType;
import info.dylansouthard.StraysBookAPI.model.enums.SexType;
import info.dylansouthard.StraysBookAPI.model.friendo.Animal;
import info.dylansouthard.StraysBookAPI.model.shared.GeoSchema;
import info.dylansouthard.StraysBookAPI.model.user.User;
import info.dylansouthard.StraysBookAPI.service.ImageHandler.AnimalImageHandlerService;
import info.dylansouthard.StraysBookAPI.testutils.ExceptionAssertionRunner;
import info.dylansouthard.StraysBookAPI.util.updaters.AnimalUpdater;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.LocalDateTime;
import java.util.List;

import static info.dylansouthard.StraysBookAPI.config.DummyTestData.defaultAnimalName;
import static info.dylansouthard.StraysBookAPI.config.DummyTestData.generateCreateAnimalDTO;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@Transactional
public class AnimalServiceIT extends BaseDBTest {
    @Autowired
    AnimalService animalService;

    @Autowired
    AnimalImageHandlerService animalImageHandlerService;

    @MockitoSpyBean
    private AnimalMapper spyMapper;

    // ==========================================
    // Animal Creation Tests
    // ==========================================

    /**
     * Tests successful animal creation with valid data.
     */
    @Test
    public void When_ValidCreateAnimalDTO_Expect_AnimalIsCreatedAndSaved() {
        User user = userRepository.save(DummyTestData.createUser());
        CreateAnimalDTO dto = generateCreateAnimalDTO();

        AnimalDTO result = animalService.createAnimal(dto, getCatImage(), user);

        assertAll("AnimalDTO validation",
                () -> assertNotNull(result, "Result should not be null"),
                () -> assertEquals(defaultAnimalName, result.getName(), "Animal name should be " + defaultAnimalName),
                () -> assertEquals(AnimalType.CAT, result.getType(), "Animal type should be CAT")
        );

        Animal fetchedAnimal = animalRepository.findById(result.getId()).orElse(null);
        assertAll("AnimalDTO saved validation",
                () -> assertNotNull(fetchedAnimal, "Fetched animal should not be null"),
                () -> assertEquals(defaultAnimalName, fetchedAnimal.getName(), "Animal name should match")
        );
    }

    @Test
    public void When_CreateAnimalWithNonImage_Expect_ThrowsException() {
        User user = userRepository.save(DummyTestData.createUser());
        CreateAnimalDTO dto = generateCreateAnimalDTO();

        ExceptionAssertionRunner.assertThrowsExceptionOfType(()-> animalService.createAnimal(dto, getNonImageFile(), user), ErrorFactory.invalidImageFormat(), "bum image format" );
    }

    @Test
    public void When_CreateAnimalWithInvalidImage_Expect_ThrowsException() {

        User user = userRepository.save(DummyTestData.createUser());

        CreateAnimalDTO dto = generateCreateAnimalDTO();

        ExceptionAssertionRunner.assertThrowsExceptionOfType(()-> animalService.createAnimal(dto, getInvalidImage(), user), ErrorFactory.imageProcessingError(), "bum image" );
    }

    /**
     * Tests animal creation with invalid data, expecting an error.
     */
    @Test
    public void When_InvalidCreateAnimalDTO_Expect_ThrowsError() {
        User user = userRepository.save(DummyTestData.createUser());

        ExceptionAssertionRunner.assertThrowsExceptionOfType(
                () -> animalService.createAnimal(null, getCatImage(), user),
                ErrorFactory.invalidCreate(),
                "Validate AppException details"
        );

        CreateAnimalDTO dto = new CreateAnimalDTO();
        dto.setName(null);
        ExceptionAssertionRunner.assertThrowsExceptionOfType(
                () -> animalService.createAnimal(dto, getCatImage(), user),
                ErrorFactory.invalidCreate(),
                "Validate Invalid AppException details"
        );
    }

    /**
     * Tests error handling when the mapper throws an exception.
     */
    @Test
    public void When_MapperThrowsException_Expect_InternalServerError() {
        User user = userRepository.save(DummyTestData.createUser());

        CreateAnimalDTO dto = generateCreateAnimalDTO();

        // Force mapper to throw an exception
        doThrow(new RuntimeException("Boom")).when(spyMapper).fromCreateAnimalDTO(any(CreateAnimalDTO.class));

        ExceptionAssertionRunner.assertThrowsExceptionOfType(() -> animalService.createAnimal(dto, getCatImage(), user), ErrorFactory.internalServerError(), "Internal server error check");
    }

    // ==========================================
    // Animal Detail Fetching Tests
    // ==========================================

    /**
     * Tests fetching animal details.
     */
    @Test
    public void When_AnimalDetailsFetched_Expect_AnimalIsRetrieved() {
        User user = userRepository.save(DummyTestData.createUser());

        Animal animal = DummyTestData.createAnimal();
        animal.setRegisteredBy(user);
        Animal savedAnimal = animalRepository.save(animal);

        AnimalDTO dto = animalService.fetchAnimalDetails(savedAnimal.getId(), null);

        assertAll("AnimalDTO Assertions",
                () -> assertNotNull(dto, "AnimalDTO should not be null"),
                () -> assertEquals(savedAnimal.getId(), dto.getId(), "Animal ID should match"),
                () -> assertEquals(savedAnimal.getName(), dto.getName(), "Animal name should match"),
                () -> assertNotNull(dto.getRegisteredBy(), "RegisteredBy should not be null"),
                () -> assertEquals(user.getDisplayName(), dto.getRegisteredBy().getDisplayName(), "Display name should match"),
                () -> assertNotNull(dto.getRecentCareEvents(), "Recent care events list should not be null"),
                () -> assertTrue(dto.getRecentCareEvents().isEmpty(), "Recent care events should be empty"),
                () -> assertNull(dto.getPrimaryCaretaker(), "Primary caretaker should be null with no care events")
        );
    }

    @Test
    @Transactional
    public void When_AnimalHasCareEvents_Expect_PrimaryCaretakerAndEventsSet() {
        // Create Users
        User userA = userRepository.save(DummyTestData.createUser());
        User userB = userRepository.save(new User("User B", "abc@abc.com"));

        Animal savedAnimal = createAnimalWithPrimaryCaretaker(userA, userB);
        // Fetch DTO
        AnimalDTO dto = animalService.fetchAnimalDetails(savedAnimal.getId(), null);

        assertAll("AnimalDTO with Care Events",
                () -> assertNotNull(dto, "AnimalDTO should not be null"),
                () -> assertEquals(savedAnimal.getId(), dto.getId(), "Animal ID should match"),
                () -> assertNotNull(dto.getRecentCareEvents(), "Recent care events should not be null"),
                () -> assertEquals(3, dto.getRecentCareEvents().size(), "Should have 3 care events"),
                () -> assertNotNull(dto.getPrimaryCaretaker(), "Primary caretaker should not be null"),
                () -> assertEquals(userA.getDisplayName(), dto.getPrimaryCaretaker().getDisplayName(), "UserB should be primary caretaker")
        );
    }

    @Test
    @Transactional
    public void When_FindingAnimalsInArea_Expect_CorrectAnimalFound() {
        // Given
        Animal inRangeAnimal = new Animal(AnimalType.CAT, SexType.FEMALE, "アメリ", new GeoSchema(34.7376, 135.3415)); // Within radius
        Animal outOfRangeAnimal = new Animal(AnimalType.DOG, SexType.MALE, "Borky", new GeoSchema(14.0000, 136.0000)); // Outside radius

        animalRepository.save(inRangeAnimal);
        animalRepository.save(outOfRangeAnimal);

        // When
        List<AnimalSummaryDTO> foundAnimals = animalService.getAnimalsInArea(34.7385, 135.3415, 1000.0, null);  // 1000m radius

        // Then
        assertAll("Validate only in-range animal is found",
                () -> assertEquals(1, foundAnimals.size(), "Only one animal should be found in the area"),
                () -> assertEquals("アメリ", foundAnimals.get(0).getName(), "Found animal should be アメリ")
        );
    }

    @Test
    public void When_FindingInvalidAnimal_Expect_NotFoundError() {
        long invalidId = 9999L;
        ExceptionAssertionRunner.assertThrowsExceptionOfType(()->animalService.fetchAnimalDetails(invalidId, null), ErrorFactory.animalNotFound(),"No animal found on invalid fetch");
    }

    @ParameterizedTest(name = "{index} => {0}")
    @MethodSource("info.dylansouthard.StraysBookAPI.testutils.TestCaseRepo#getAnimalUpdateTestCases")
    @Transactional
    public void When_UpdatingAnimal_AnimalShouldBeUpdatedInAccordanceWithAccessRules(AnimalUpdateTestCase testCase) {

        User user = userRepository.save(DummyTestData.createUser());
        User secondUser = userRepository.save(new User("User B", "abc@abc.com"));

        Animal animal = createAnimalWithPrimaryCaretaker(user, secondUser);

        UpdateAnimalDTO updateDTO = new UpdateAnimalDTO();
        testCase.getUpdates().forEach(updateDTO::addUpdate);

        testCase.getOriginalValues().forEach((key, value) -> {
            AnimalUpdater.applyUpdate(animal, key, value);
        });

        animalRepository.save(animal);

        if (!testCase.isShouldSucceed() && testCase.getUpdates().size() < 2) {
            ExceptionAssertionRunner.assertThrowsExceptionOfType(
                    ()-> animalService.updateAnimal(animal.getId(), updateDTO, testCase.isUserIsPrimaryCaretaker() ? user : secondUser),
                    ErrorFactory.authForbidden(),
                    "no ability to update animal"
            );
            return;
        }

        AnimalDTO updatedAnimal = animalService.updateAnimal(animal.getId(), updateDTO, testCase.isUserIsPrimaryCaretaker() ? user : secondUser);

        Object expectedValue = testCase.isShouldSucceed()
                ? testCase.getUpdates().values().iterator().next()
                :testCase.getOriginalValues().values().iterator().next();

        BeanWrapper udWrapper  = new BeanWrapperImpl(updatedAnimal);
        Object actualValue = udWrapper.getPropertyValue(testCase.getUpdates().keySet().iterator().next());

        assertEquals(
                String.valueOf(expectedValue),
                String.valueOf(actualValue),
                testCase.getDesc() + " Value should match expected update rule"
        );
    }

    // ==========================================
    // Animal Update Tests
    // ==========================================

    /**
     * Tests updating an animal based on update rules.
     */
    @Test
    void updateAnimal_shouldThrow_whenAnimalNotFound() {
        Long nonexistentId = -1L;
        UpdateAnimalDTO dto = new UpdateAnimalDTO();
        //todo

//        dto.setUpdates(Map.of("name", "Ghost"));

        User dummyUser = new User();
        dummyUser.setId(1L);

        ExceptionAssertionRunner.assertThrowsExceptionOfType(() -> {
            animalService.updateAnimal(nonexistentId, dto, dummyUser);
        }, ErrorFactory.animalNotFound(), "No animal found on update");
    }

    @Test
    void updateAnimal_shouldThrow_whenUpdateDTOIsNull() {
        User dummyUser = new User();
        dummyUser.setId(1L);

        ExceptionAssertionRunner.assertThrowsExceptionOfType(
                () -> animalService.updateAnimal(1L, null, dummyUser),
                ErrorFactory.invalidParams(),
                "Update animal null dto"
        );

    }

    @Test
    void When_UpdateAnimalImage_Then_ImageShouldUpdateAccordingToRules () {
        User user = userRepository.save(DummyTestData.createUser());
        CreateAnimalDTO dto = generateCreateAnimalDTO();
        User secondUser = userRepository.save(new User("User B", "abc@abc.com"));
        AnimalDTO animal = animalService.createAnimal(dto, getCatImage(), user);
        Animal savedAnimal = animalRepository.findById(animal.getId()).get();
        CareEvent _ = addAnimalAndSave(new CareEvent(CareEventType.FED, LocalDateTime.now().minusDays(5), user), savedAnimal);

        String originalImgUrl = savedAnimal.getImgUrl();
        assertTrue(animalImageHandlerService.imageExists(originalImgUrl));

        ImageUpdateResponseDTO res = animalService.updateAnimalImage(animal.getId(), getCatImage(), user);

        AnimalDTO resavedAnimal = animalService.fetchAnimalDetails(res.getId(), null);

        assertAll("Image update Assertions",
                ()-> assertFalse(animalImageHandlerService.imageExists(originalImgUrl)),
                ()-> assertTrue(animalImageHandlerService.imageExists(res.getImgUrl())),
                ()-> assertEquals(resavedAnimal.getImgUrl(), res.getImgUrl())
        );


        ExceptionAssertionRunner.assertThrowsExceptionOfType(
                ()->animalService.updateAnimalImage(resavedAnimal.getId(), getCatImage(), secondUser),
                ErrorFactory.authForbidden(),
                "Non primary caretaker should not be able to update image");
    }

    // ==========================================
    // Animal Deletion Tests
    // ==========================================

    /**
     * Tests deleting an animal by primary caretaker.
     */
    @Test
    void When_DeletingAnimal_AnimalShouldBeDeletedOnlyByPrimaryCaretaker() {
        User user = userRepository.save(DummyTestData.createUser());
        User secondUser = userRepository.save(new User("User B", "abc@abc.com"));
        Animal animal = createAnimalWithPrimaryCaretaker(user, secondUser);
        ExceptionAssertionRunner.assertThrowsExceptionOfType(()-> animalService.deleteAnimal(animal.getId(), secondUser),ErrorFactory.authForbidden(), "Non-primary caretaker not authorized to delete");

        assertNotNull(animalRepository.findByActiveId(animal.getId()).orElse(null), "Animal should still exist");

        animalService.deleteAnimal(animal.getId(), user);

        assertNull(animalRepository.findByActiveId(animal.getId()).orElse(null), "Deleted animal should not exist");
    }

    /**
     * Tests deleting a nonexistent animal, expecting an error.
     */
    @Test
    void When_DeletingNonexistentAnimal_ShouldThrowNotFoundError() {
        long invalidId = 9999L;
        User user = userRepository.save(DummyTestData.createUser());
        ExceptionAssertionRunner.assertThrowsExceptionOfType(()->animalService.deleteAnimal(invalidId, user), ErrorFactory.animalNotFound(),"No animal found on invalid delete");
    }
}
