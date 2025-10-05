package info.dylansouthard.StraysBookAPI.mapper;

import info.dylansouthard.StraysBookAPI.dto.careEvent.CareEventDTO;
import info.dylansouthard.StraysBookAPI.dto.careEvent.CareEventSummaryDTO;
import info.dylansouthard.StraysBookAPI.dto.friendo.AnimalSummaryDTO;
import info.dylansouthard.StraysBookAPI.dto.user.UserSummaryMinDTO;
import info.dylansouthard.StraysBookAPI.model.CareEvent;
import info.dylansouthard.StraysBookAPI.model.friendo.Animal;
import info.dylansouthard.StraysBookAPI.model.user.User;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-08-11T16:29:08+0900",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 23.0.2 (Homebrew)"
)
@Component
public class CareEventMapperImpl implements CareEventMapper {

    @Override
    public CareEventDTO toCareEventDTO(CareEvent careEvent) {
        if ( careEvent == null ) {
            return null;
        }

        CareEventDTO careEventDTO = new CareEventDTO();

        careEventDTO.setId( careEvent.getId() );
        careEventDTO.setType( careEvent.getType() );
        careEventDTO.setDate( careEvent.getDate() );
        careEventDTO.setNewValue( careEvent.getNewValue() );
        careEventDTO.setNotes( careEvent.getNotes() );
        careEventDTO.setRegisteredBy( userToUserSummaryMinDTO( careEvent.getRegisteredBy() ) );
        if ( careEvent.getAssociatedId() != null ) {
            careEventDTO.setAssociatedId( careEvent.getAssociatedId() );
        }
        careEventDTO.setAnimals( animalSetToAnimalSummaryDTOSet( careEvent.getAnimals() ) );

        return careEventDTO;
    }

    @Override
    public CareEventSummaryDTO toCareEventSummaryDTO(CareEvent careEvent) {
        if ( careEvent == null ) {
            return null;
        }

        CareEventSummaryDTO careEventSummaryDTO = new CareEventSummaryDTO();

        careEventSummaryDTO.setId( careEvent.getId() );
        careEventSummaryDTO.setType( careEvent.getType() );
        careEventSummaryDTO.setDate( careEvent.getDate() );
        careEventSummaryDTO.setNewValue( careEvent.getNewValue() );
        careEventSummaryDTO.setNotes( careEvent.getNotes() );
        careEventSummaryDTO.setRegisteredBy( userToUserSummaryMinDTO( careEvent.getRegisteredBy() ) );

        return careEventSummaryDTO;
    }

    protected UserSummaryMinDTO userToUserSummaryMinDTO(User user) {
        if ( user == null ) {
            return null;
        }

        UserSummaryMinDTO userSummaryMinDTO = new UserSummaryMinDTO();

        userSummaryMinDTO.setId( user.getId() );
        userSummaryMinDTO.setDisplayName( user.getDisplayName() );

        return userSummaryMinDTO;
    }

    protected AnimalSummaryDTO animalToAnimalSummaryDTO(Animal animal) {
        if ( animal == null ) {
            return null;
        }

        AnimalSummaryDTO animalSummaryDTO = new AnimalSummaryDTO();

        animalSummaryDTO.setId( animal.getId() );
        animalSummaryDTO.setName( animal.getName() );
        animalSummaryDTO.setImgUrl( animal.getImgUrl() );
        animalSummaryDTO.setType( animal.getType() );
        animalSummaryDTO.setDescription( animal.getDescription() );
        animalSummaryDTO.setSex( animal.getSex() );
        animalSummaryDTO.setLastFed( animal.getLastFed() );

        return animalSummaryDTO;
    }

    protected Set<AnimalSummaryDTO> animalSetToAnimalSummaryDTOSet(Set<Animal> set) {
        if ( set == null ) {
            return null;
        }

        Set<AnimalSummaryDTO> set1 = new LinkedHashSet<AnimalSummaryDTO>( Math.max( (int) ( set.size() / .75f ) + 1, 16 ) );
        for ( Animal animal : set ) {
            set1.add( animalToAnimalSummaryDTO( animal ) );
        }

        return set1;
    }
}
