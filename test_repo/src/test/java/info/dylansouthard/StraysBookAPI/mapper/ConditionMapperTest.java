package info.dylansouthard.StraysBookAPI.mapper;

import info.dylansouthard.StraysBookAPI.dto.ConditionDTO;
import info.dylansouthard.StraysBookAPI.model.enums.AnimalType;
import info.dylansouthard.StraysBookAPI.model.enums.ConditionType;
import info.dylansouthard.StraysBookAPI.model.enums.SexType;
import info.dylansouthard.StraysBookAPI.model.friendo.Animal;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.*;

public class ConditionMapperTest {
    private final ConditionMapper conditionMapper = Mappers.getMapper(ConditionMapper.class);
    @Test
    void When_MappingToConditionDTO_Then_ReturnConditionDTO() {
        Animal animal = new Animal(AnimalType.CAT, SexType.FEMALE, "Fluffy wuffy");
        ConditionDTO emptyDTO = conditionMapper.mapCondition(animal);
        assertNotNull(emptyDTO, "Empty ConditionDTO should not be null");
        animal.getConditions().add(ConditionType.HEALTHY);
        animal.setConditionRating(5);
        ConditionDTO dto = conditionMapper.mapCondition(animal);

        assertAll( "Condition DTO Assertions",
                ()->assertNotNull(dto, "ConditionDTO should not be null"),
                ()->assertEquals(1, dto.getTypes().size(), "ConditionDTO types length should be 1"),
                ()->assertEquals(5, dto.getRating(), "ConditionDTO rating should be 5")
                );
    }
}
