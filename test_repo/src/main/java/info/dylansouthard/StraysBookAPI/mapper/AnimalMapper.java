package info.dylansouthard.StraysBookAPI.mapper;

import info.dylansouthard.StraysBookAPI.dto.ConditionDTO;
import info.dylansouthard.StraysBookAPI.dto.common.PaginatedResponseDTO;
import info.dylansouthard.StraysBookAPI.dto.friendo.*;
import info.dylansouthard.StraysBookAPI.model.friendo.Animal;
import org.mapstruct.*;
import org.springframework.data.domain.Page;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        uses = {ConditionMapper.class, StatusMapper.class, UserMapper.class, VaccinationMapper.class})
public interface AnimalMapper {

    @Mapping(source = ".", target = "status", qualifiedByName = "mapStatus")
    @Mapping(source = ".", target = "condition", qualifiedByName = "mapCondition")
    @Mapping(target="friendoType", expression ="java(info.dylansouthard.StraysBookAPI.model.enums.FriendoType.ANIMAL)")
    AnimalSummaryDTO toAnimalSummaryDTO(Animal animal);

    @Mapping(target = "friendoType", expression = "java(info.dylansouthard.StraysBookAPI.model.enums.FriendoType.ANIMAL)")
    @Mapping(source = ".", target = "condition", qualifiedByName = "mapCondition")
    @Mapping(source = ".", target = "status", qualifiedByName = "mapStatus")
    @Mapping(source = "registeredBy", target = "registeredBy", qualifiedByName = "toUserSummaryMinDTO")
    AnimalDTO toAnimalDTO(Animal animal);


    AnimalSummaryMinDTO toAnimalSummaryMinDTO(Animal animal);

    Animal fromCreateAnimalDTO(CreateAnimalDTO dto);

    void updateAnimalFromDTO(UpdateAnimalDTO dto, @MappingTarget Animal animal);

    @AfterMapping
    default void mapConditionDetails(CreateAnimalDTO dto, @MappingTarget Animal animal) {
        ConditionDTO condition = dto.getCondition();
        if (condition != null) {
            setConditionDetails(condition, animal);
        }
    }

    static void setConditionDetails(ConditionDTO condition, @MappingTarget Animal animal) {
        animal.setConditions(condition.getTypes());
        animal.setCriticalCondition(condition.getCritical());
        animal.setConditionRating(condition.getRating());
        animal.setConditionNotes(condition.getNotes());
    }

    default PaginatedResponseDTO<AnimalSummaryDTO> toPaginatedResponseDTO(Page<Animal> page) {
        return new PaginatedResponseDTO<>(
                page.getContent().stream().map(this::toAnimalSummaryDTO).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalPages(),
                page.getTotalElements(),
                page.hasNext(),
                page.hasPrevious()
        );
    }
}
