package info.dylansouthard.StraysBookAPI.mapper;

import info.dylansouthard.StraysBookAPI.dto.StatusDTO;
import info.dylansouthard.StraysBookAPI.model.enums.AnimalType;
import info.dylansouthard.StraysBookAPI.model.enums.SexType;
import info.dylansouthard.StraysBookAPI.model.enums.StatusType;
import info.dylansouthard.StraysBookAPI.model.friendo.Animal;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.*;

public class StatusMapperTest {
    private final StatusMapper statusMapper = Mappers.getMapper(StatusMapper.class);
    @Test
    void When_MappingToStatusDTO_Then_ReturnStatusDTO() {
        Animal animal = new Animal(AnimalType.CAT, SexType.FEMALE, "Fluffy wuffy");
        StatusDTO emptyDTO = statusMapper.mapStatus(animal);
        assertNotNull(emptyDTO, "Empty StatusDTO should not be null");

        StatusDTO dto = statusMapper.mapStatus(animal);

        assertAll( "Status DTO Assertions",
                ()->assertNotNull(dto, "StatusDTO should not be null"),
                ()->assertEquals(StatusType.STRAY, dto.getType(), "StatusDTO type should be STRAY")
                );
    }
}
