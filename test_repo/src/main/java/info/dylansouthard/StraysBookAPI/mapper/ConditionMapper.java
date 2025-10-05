package info.dylansouthard.StraysBookAPI.mapper;

import info.dylansouthard.StraysBookAPI.dto.ConditionDTO;
import info.dylansouthard.StraysBookAPI.model.friendo.Animal;
import org.mapstruct.Mapper;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface ConditionMapper {

    @Named("mapCondition")
    default ConditionDTO mapCondition(Animal animal) {
        if (animal == null) {return null;}
        return new ConditionDTO(animal.getConditions(), animal.getCriticalCondition(), animal.getConditionRating(), animal.getConditionNotes());
    }
}
