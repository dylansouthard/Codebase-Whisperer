package info.dylansouthard.StraysBookAPI.mapper;

import info.dylansouthard.StraysBookAPI.config.DummyTestData;
import info.dylansouthard.StraysBookAPI.dto.ConditionDTO;
import info.dylansouthard.StraysBookAPI.dto.friendo.*;
import info.dylansouthard.StraysBookAPI.dto.vaccination.VaccinationSummaryDTO;
import info.dylansouthard.StraysBookAPI.model.Vaccination;
import info.dylansouthard.StraysBookAPI.model.enums.*;
import info.dylansouthard.StraysBookAPI.model.friendo.Animal;
import info.dylansouthard.StraysBookAPI.model.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

//@ExtendWith(MockitoExtension.class)
@ExtendWith(SpringExtension.class)  // ✅ Use Spring's JUnit5 extension
@Import({AnimalMapperImpl.class, ConditionMapperImpl.class, StatusMapperImpl.class, UserMapperImpl.class, VaccinationMapperImpl.class })
public class AnimalMapperTest {

    @Autowired
    private AnimalMapper animalMapper;


    @Test
    public void When_MappingToAnimalSummaryMinDTO_Then_ReturnAnimalSummaryMinDTO() {
        Animal animal = DummyTestData.createAnimal();
        AnimalSummaryMinDTO dto = animalMapper.toAnimalSummaryMinDTO(animal);

        assertAll("Animal Summary DTO Assertions",
                ()->assertNotNull(dto, "DTO should not be null"),
                ()->assertEquals(dto.getName(), animal.getName(), "Animal DTO name should = animal name")
        );
    }


    @Test
    public void When_MappingToAnimalSummaryDTO_Then_ReturnAnimalSummaryDTO() {
        Animal animal = DummyTestData.createAnimal();
        AnimalSummaryDTO dto = animalMapper.toAnimalSummaryDTO(animal);

        assertAll("Animal Summary DTO Assertions",
                ()->assertNotNull(dto, "DTO should not be null"),
                ()->assertEquals(dto.getName(), animal.getName(), "Animal DTO name should = animal name"),
                ()->assertEquals(FriendoType.ANIMAL, dto.getFriendoType(), "DTO Friendo type should be animal")
        );
    }

    @Test
    public void When_MappingToAnimalDTO_Then_ReturnAnimalDTO() {
        Animal animal = DummyTestData.createAnimal();
        animal.getConditions().add(ConditionType.HEALTHY);

        AnimalDTO dto = animalMapper.toAnimalDTO(animal);

        assertAll("Animal DTO Assertions",
                ()->assertNotNull(dto, "DTO should not be null"),
                ()->assertEquals(dto.getName(), animal.getName(), "Animal DTO name should = animal name"),
                ()->assertEquals(FriendoType.ANIMAL, dto.getFriendoType(), "DTO Friendo type should be animal")
        );
    }

    @Test
    public void When_MappingAnimalToDTO_Then_ConditionAndStatusShouldNotBeNull() {
        // Given
        Animal animal = DummyTestData.createAnimal();
        animal.getConditions().add(ConditionType.HEALTHY);
        animal.setConditionRating(5);

        // When
        AnimalDTO dto = animalMapper.toAnimalDTO(animal);

        // Then
        assertNotNull(dto.getCondition(), "ConditionDTO should not be null");
        assertEquals(1, dto.getCondition().getTypes().size(), "ConditionDTO should have one type");
        assertEquals(5, dto.getCondition().getRating(), "ConditionDTO rating should be 5");
        assertNotNull(dto.getStatus(), "Status should not be null");
    }

    @Test
    public void When_MappingAnimalToDTO_Then_RegisteredByAndVaccinationsShouldNotBeNull() {
        // Given
        Animal animal = DummyTestData.createAnimal();

        // Simulate registeredBy (User)
        User user = new User();
        user.setId(2L);
        user.setDisplayName("UserMcUseFace");
        animal.setRegisteredBy(user);

        // Simulate vaccinations
        Vaccination vaccination = new Vaccination(VaccinationType.FELV, user);
        animal.getVaccinations().add(vaccination);

        // When
        AnimalDTO dto = animalMapper.toAnimalDTO(animal);

        // Then
        assertNotNull(dto.getRegisteredBy(), "RegisteredBy should not be null");
        assertEquals("UserMcUseFace", dto.getRegisteredBy().getDisplayName(), "User display name should match");

        assertNotNull(dto.getVaccinations(), "Vaccinations list should not be null");
        assertEquals(1, dto.getVaccinations().size(), "Vaccinations list should contain 1 entry");

        VaccinationSummaryDTO mappedVaccination = dto.getVaccinations().get(0);
        assertEquals(VaccinationType.FELV, mappedVaccination.getType(), "Vaccination type should match");
    }

    @Test
    public void When_MappingCreateAnimalDTOToAnimal_Then_ReturnAnimal() {
        // Arrange
        CreateAnimalDTO dto = new CreateAnimalDTO();

        String animalName = "Amelie";
        dto.setName(animalName);
        dto.setType(AnimalType.CAT);
        dto.setSex(SexType.FEMALE);


        ConditionDTO condition = new ConditionDTO();
        condition.setTypes(List.of(ConditionType.HEALTHY));
        condition.setCritical(false);
        condition.setRating(4);
        condition.setNotes("Healthy and happy cat.");
        dto.setCondition(condition);

        // Act
        Animal animal = animalMapper.fromCreateAnimalDTO(dto);

        // Assert
        assertNotNull(animal, "Mapped Animal should not be null");
        assertEquals(animalName, animal.getName(), "Animal name should match DTO name");
        assertEquals(AnimalType.CAT, animal.getType(), "Animal type should match DTO type");
        assertEquals(SexType.FEMALE, animal.getSex(), "Animal sex should match DTO sex");

        // Condition mapping assertions
        assertNotNull(animal.getConditions(), "Animal conditions list should not be null");
        assertEquals(1, animal.getConditions().size(), "Animal should have one condition type");
        assertTrue(animal.getConditions().contains(ConditionType.HEALTHY), "Animal conditions should include HEALTHY");

        assertEquals(false, animal.getCriticalCondition(), "Critical condition should be false");
        assertEquals(4, animal.getConditionRating(), "Condition rating should match DTO rating");
        assertEquals("Healthy and happy cat.", animal.getConditionNotes(), "Condition notes should match DTO notes");

    }


}
