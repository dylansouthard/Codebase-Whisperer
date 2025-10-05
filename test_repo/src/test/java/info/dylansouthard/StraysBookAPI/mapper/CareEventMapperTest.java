package info.dylansouthard.StraysBookAPI.mapper;

import info.dylansouthard.StraysBookAPI.dto.careEvent.CareEventDTO;
import info.dylansouthard.StraysBookAPI.dto.careEvent.CareEventSummaryDTO;
import info.dylansouthard.StraysBookAPI.model.CareEvent;
import info.dylansouthard.StraysBookAPI.model.enums.AnimalType;
import info.dylansouthard.StraysBookAPI.model.enums.CareEventType;
import info.dylansouthard.StraysBookAPI.model.enums.SexType;
import info.dylansouthard.StraysBookAPI.model.friendo.Animal;
import info.dylansouthard.StraysBookAPI.model.user.User;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static info.dylansouthard.StraysBookAPI.config.DummyTestData.createCareEvent;
import static org.junit.jupiter.api.Assertions.*;

public class CareEventMapperTest {
    private final CareEventMapper careEventMapper = Mappers.getMapper(CareEventMapper.class);


    @Test
    public void When_MappingToCareEventSummaryDTO_Then_ReturnCareEventSummaryDTO() {
        CareEvent careEvent = createCareEvent();

        CareEventSummaryDTO dto = careEventMapper.toCareEventSummaryDTO(careEvent);
        assertAll("Care Event DTO assertions",
                ()->assertNotNull(dto, "Care Event Summary DTO should not be null"),
                ()->assertEquals(CareEventType.FED, dto.getType())
                );
    }

    @Test
    public void When_MappingToCareEventDTO_Then_ReturnCareEventDTO() {
        CareEvent careEvent = createCareEvent();

        String animalName = "kitty";
        String userName = "Joe Schmo";

        User user = new User();
        user.setDisplayName(userName);
        Animal animal = new Animal(AnimalType.CAT, SexType.FEMALE, animalName);
        careEvent.setRegisteredBy(user);
        careEvent.getAnimals().add(animal);

        CareEventDTO dto = careEventMapper.toCareEventDTO(careEvent);

        assertAll("Care Event DTO assertions",
                ()->assertNotNull(dto, "Care Event  DTO should not be null"),
                ()->assertEquals(CareEventType.FED, dto.getType()),
                ()->assertEquals(1, dto.getAnimals().size()),
                ()->assertEquals(animalName, dto.getAnimals().stream().findFirst().get().getName()),
                ()->assertNotNull(dto.getRegisteredBy()),
                ()->assertEquals(userName, dto.getRegisteredBy().getDisplayName())
        );
    }
}
