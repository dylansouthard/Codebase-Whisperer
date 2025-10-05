package info.dylansouthard.StraysBookAPI.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import info.dylansouthard.StraysBookAPI.cases.AnimalUpdateTestCase;
import info.dylansouthard.StraysBookAPI.common.BaseDBTest;
import info.dylansouthard.StraysBookAPI.config.DummyTestData;
import info.dylansouthard.StraysBookAPI.constants.ApiRoutes;
import info.dylansouthard.StraysBookAPI.dto.careEvent.CreateSimpleCareEventDTO;
import info.dylansouthard.StraysBookAPI.dto.friendo.AnimalDTO;
import info.dylansouthard.StraysBookAPI.dto.friendo.CreateAnimalDTO;
import info.dylansouthard.StraysBookAPI.errors.AppException;
import info.dylansouthard.StraysBookAPI.errors.ErrorCodes;
import info.dylansouthard.StraysBookAPI.errors.ErrorFactory;
import info.dylansouthard.StraysBookAPI.errors.ErrorMessages;
import info.dylansouthard.StraysBookAPI.model.CareEvent;
import info.dylansouthard.StraysBookAPI.model.enums.AnimalType;
import info.dylansouthard.StraysBookAPI.model.enums.CareEventType;
import info.dylansouthard.StraysBookAPI.model.enums.SexType;
import info.dylansouthard.StraysBookAPI.model.friendo.Animal;
import info.dylansouthard.StraysBookAPI.model.shared.GeoSchema;
import info.dylansouthard.StraysBookAPI.model.user.User;
import info.dylansouthard.StraysBookAPI.repository.AnimalRepository;
import info.dylansouthard.StraysBookAPI.repository.UserRepository;
import info.dylansouthard.StraysBookAPI.service.AnimalService;
import info.dylansouthard.StraysBookAPI.testutils.ExceptionAssertionRunner;
import info.dylansouthard.StraysBookAPI.testutils.TestSecurityUtil;
import info.dylansouthard.StraysBookAPI.util.updaters.AnimalUpdater;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static info.dylansouthard.StraysBookAPI.config.DummyTestData.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
@AutoConfigureMockMvc
public class AnimalControllerIT extends BaseDBTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AnimalRepository animalRepository;

    @Autowired
    private AnimalService animalService;

    @Test
    public void test_CreateAnimal_WithInjectedUser() throws Exception {
        User user = userRepository.save(createUser());

        // ✅ Inject user directly, no token
        TestSecurityUtil.authenticateTestUser(user);

        CreateAnimalDTO dto = generateCreateAnimalDTO();
        MockMultipartFile jsonPart = new MockMultipartFile(
                "body",
                "",
                "application/json",
                objectMapper.writeValueAsBytes(dto)
        );


        String json = objectMapper.writeValueAsString(dto);
        mockMvc.perform(multipart(ApiRoutes.ANIMALS.CREATE)
                        .file(jsonPart)
                        .file(getCatImage())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated());
    }

    @Test
    public void When_FetchingValidAnimal_ReturnAnimalDTO() throws Exception {
        Animal savedAnimal = animalRepository.save(createAnimal());
        mockMvc.perform(get(ApiRoutes.ANIMALS.BASE + "/" + savedAnimal.getId()))
                .andExpect(status().isOk());
    }

    @Test
    public void When_FetchingInvalidAnimal_ReturnNotFoundError() throws Exception {
        long invalidId = 9999L;  // Assumed non-existent

        mockMvc.perform(get(ApiRoutes.ANIMALS.BASE + "/" + invalidId))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCodes.ANIMAL_NOT_FOUND))
                .andExpect(jsonPath("$.message").value(ErrorMessages.ANIMAL_NOT_FOUND));
    }

    @Test
    @Transactional
    public void When_GetNearbyAnimals_Expect_OnlyNearbyAnimalReturned() throws Exception {
        // Given
        Animal nearbyAnimal = new Animal(AnimalType.CAT, SexType.FEMALE, "アメリ", new GeoSchema(34.7376, 135.3415)); // ✅ Within radius
        Animal farAnimal = new Animal(AnimalType.DOG, SexType.MALE, "Borky", new GeoSchema(36.0000, 137.0000)); // ❌ Outside radius
        animalRepository.saveAll(List.of(nearbyAnimal, farAnimal));

        // When: Perform GET request to /nearby
        mockMvc.perform(get(ApiRoutes.ANIMALS.NEARBY)
                        .param("lat", "34.7385")
                        .param("lon", "135.3415")
                        .param("radius", "1000")  // meters
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("アメリ"));
    }

    @Test
    public void When_InvalidCoordinatesProvided_Expect400Error() throws Exception {
        // Test 1: Missing lat param
        mockMvc.perform(get(ApiRoutes.ANIMALS.NEARBY)
                        .param("lon", "135.3415"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCodes.INVALID_COORDINATES))
                .andExpect(jsonPath("$.message").value(ErrorMessages.INVALID_COORDINATES));

        // Test 2: Missing lon param
        mockMvc.perform(get(ApiRoutes.ANIMALS.NEARBY)
                        .param("lat", "34.7385"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCodes.INVALID_COORDINATES))
                .andExpect(jsonPath("$.message").value(ErrorMessages.INVALID_COORDINATES));

        // Test 3: Both missing
        mockMvc.perform(get(ApiRoutes.ANIMALS.NEARBY))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCodes.INVALID_COORDINATES))
                .andExpect(jsonPath("$.message").value(ErrorMessages.INVALID_COORDINATES));
    }

    @Test
    @Transactional
    public void When_FetchingCareEventsForAnimal_Expect_CareEventReturned() throws Exception {
        User user = userRepository.save(createUser());
        Animal animal = createAnimalWithCareEvents(user);

        mockMvc.perform(get(ApiRoutes.ANIMALS.BASE + "/" + animal.getId() + "/care-events")
                        .param("page", "0")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(3));
    }

    /**
     * Integration test for the animal update endpoint.
     * <p>
     * This test dynamically loads test cases from a JSON file to verify that the updateAnimal controller endpoint
     * allows or denies updates based on access rules defined per field (PUBLIC, RESTRICTED, CONDITIONAL).
     * <p>
     * Each test case includes a field to update, whether the user is the primary caretaker,
     * the original value, the attempted update value, and whether the update should succeed.
     *
     * @param testCase the dynamically loaded test case describing the update scenario
     */
    @ParameterizedTest(name = "{index} => {0}")
    @MethodSource("info.dylansouthard.StraysBookAPI.testutils.TestCaseRepo#getAnimalUpdateTestCases")
    @Transactional
    public void test_UpdateAnimal_WithInjectedUser(AnimalUpdateTestCase testCase) throws Exception {

        // Set up primary and secondary users and authenticate
        User user = userRepository.save(createUser());
        User secondUser = userRepository.save(new User("User B", "abc@abc.com"));
        Animal animal = createAnimalWithPrimaryCaretaker(user, secondUser);
        TestSecurityUtil.authenticateTestUser(testCase.isUserIsPrimaryCaretaker() ? user : secondUser);

        // Prepare the update payload from the test case
        Map<String, Object> updateJson = new HashMap<>(testCase.getUpdates());

        // Apply original values to the animal so that updates are measurable
        testCase.getOriginalValues().forEach((key, value) -> {
            AnimalUpdater.applyUpdate(animal, key, value);
        });

        // Persist the modified animal to the database
        animalRepository.save(animal);

        // Determine which field is being updated and what the expected result should be
        String field = testCase.getUpdates().keySet().iterator().next();


        // Perform the PATCH request and assert the correct HTTP status
        ResultActions result = mockMvc.perform(patch(ApiRoutes.ANIMALS.BASE + "/" + animal.getId())
                        .with(TestSecurityUtil.testUser(testCase.isUserIsPrimaryCaretaker() ? user : secondUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateJson)));


        if (testCase.getThrowsError() == null) {

            Object expectedValue = testCase.isShouldSucceed()
                    ? testCase.getUpdates().values().iterator().next()
                    : testCase.getOriginalValues().values().iterator().next();

            result.andExpect(status().isOk());
            // If the field is location, validate latitude and longitude separately
            if (field.equals("location")) {
                Map<String, Double> loc = (Map<String, Double>) expectedValue;
                result
                        .andExpect(jsonPath("$.location.latitude").value(loc.get("latitude")))
                        .andExpect(jsonPath("$.location.longitude").value(loc.get("longitude")));
            } else {
                result.andExpect(jsonPath("$." + field).value(String.valueOf(expectedValue)));
            }
        } else {
            AppException exception = ErrorFactory.getErrorOfType(testCase.getThrowsError());
            ExceptionAssertionRunner.assertThrowsExceptionOfType(result, exception);
        }
    }

    @Test
    @Transactional
    public void When_UpdateAnimalImage_AnimalImageUpdated() throws Exception {
        User user = userRepository.save(DummyTestData.createUser());
        CreateAnimalDTO dto = generateCreateAnimalDTO();
        AnimalDTO animal = animalService.createAnimal(dto, getCatImage(), user);
        Animal savedAnimal = animalRepository.findById(animal.getId()).get();
        CareEvent _ = addAnimalAndSave(new CareEvent(CareEventType.FED, LocalDateTime.now().minusDays(5), user), savedAnimal);
        TestSecurityUtil.authenticateTestUser(user);

        mockMvc.perform(multipart(org.springframework.http.HttpMethod.PATCH, ApiRoutes.ANIMALS.IMAGE, animal.getId())
                        .file(getCatImage())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .with(TestSecurityUtil.testUser(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(animal.getId()))
                .andExpect(jsonPath("$.name").value(animal.getName()))
                .andExpect(jsonPath("$.imgUrl").exists());
    }

    @Test
    @Transactional
    public void When_AddCareEvent_Expect_CareEventAdded() throws Exception {
        User user = userRepository.save(DummyTestData.createUser());
        TestSecurityUtil.authenticateTestUser(user);
        Animal animal = animalRepository.save(DummyTestData.createAnimal());
        CreateSimpleCareEventDTO dto = constructSimpleCEDTO(null);

        String json = objectMapper.writeValueAsString(dto);

        mockMvc.perform(post(ApiRoutes.ANIMALS.BASE + "/" + animal.getId() + "/care-events")
                        .with(TestSecurityUtil.testUser(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated()) // adjust this if you return 200 or something else
                .andExpect(jsonPath("$.type").value("FED"))
                .andExpect(jsonPath("$.animals[0].id").value(animal.getId()));
    }

    @Test
    @Transactional
    public void When_DeleteAnimalWithPrimaryCaretaker_Expect_AnimalDeleted() throws Exception {
        User user = userRepository.save(createUser());
        User secondUser = userRepository.save(new User("User B", "abc@abc.com"));
        Animal animal = createAnimalWithPrimaryCaretaker(user, secondUser);

        TestSecurityUtil.authenticateTestUser(user);
        mockMvc.perform(delete(ApiRoutes.ANIMALS.BASE + "/" + animal.getId())
                        .with(TestSecurityUtil.testUser(user)))
                .andDo(print())
                .andExpect(status().isNoContent());
    }

    @Test
    @Transactional
    public void When_DeleteAnimalWithNonPrimaryCaretaker_Expect_ThrowsError() throws Exception {
        User user = userRepository.save(createUser());
        User secondUser = userRepository.save(new User("User B", "abc@abc.com"));
        Animal animal = createAnimalWithPrimaryCaretaker(user, secondUser);
        TestSecurityUtil.authenticateTestUser(secondUser);

        ResultActions result = mockMvc.perform(delete(ApiRoutes.ANIMALS.BASE + "/" + animal.getId())
                        .with(TestSecurityUtil.testUser(secondUser)))
                .andDo(print());

        ExceptionAssertionRunner.assertThrowsExceptionOfType(result, ErrorFactory.authForbidden());
    }
}


