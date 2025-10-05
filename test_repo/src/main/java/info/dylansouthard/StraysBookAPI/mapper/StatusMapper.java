package info.dylansouthard.StraysBookAPI.mapper;

import info.dylansouthard.StraysBookAPI.dto.StatusDTO;
import info.dylansouthard.StraysBookAPI.model.friendo.Animal;
import org.mapstruct.Mapper;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface StatusMapper {

    @Named("mapStatus")
    default StatusDTO mapStatus(Animal animal){
       if (animal == null) {return null;}
       return new StatusDTO(animal.getStatusType(), animal.getStatusNotes());
    }
}
