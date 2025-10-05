package info.dylansouthard.StraysBookAPI.mapper;

import info.dylansouthard.StraysBookAPI.dto.careEvent.CareEventSummaryDTO;
import info.dylansouthard.StraysBookAPI.dto.friendo.AnimalSummaryMinDTO;
import info.dylansouthard.StraysBookAPI.dto.notification.NotificationDTO;
import info.dylansouthard.StraysBookAPI.dto.user.UserSummaryMinDTO;
import info.dylansouthard.StraysBookAPI.model.CareEvent;
import info.dylansouthard.StraysBookAPI.model.friendo.Animal;
import info.dylansouthard.StraysBookAPI.model.notification.Notification;
import info.dylansouthard.StraysBookAPI.model.user.User;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-08-11T16:29:08+0900",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 23.0.2 (Homebrew)"
)
@Component
public class NotificationMapperImpl implements NotificationMapper {

    @Override
    public NotificationDTO toNotificationDTO(Notification notification) {
        if ( notification == null ) {
            return null;
        }

        NotificationDTO notificationDTO = new NotificationDTO();

        notificationDTO.setId( notification.getId() );
        notificationDTO.setContentType( notification.getContentType() );
        notificationDTO.setType( notification.getType() );
        notificationDTO.setAnimals( animalSetToAnimalSummaryMinDTOList( notification.getAnimals() ) );
        notificationDTO.setCareEvent( careEventToCareEventSummaryDTO( notification.getCareEvent() ) );
        notificationDTO.setNewValue( notification.getNewValue() );
        notificationDTO.setRegisteredBy( userToUserSummaryMinDTO( notification.getRegisteredBy() ) );
        notificationDTO.setCreatedAt( notification.getCreatedAt() );
        notificationDTO.setNotes( notification.getNotes() );
        notificationDTO.setNotificationDate( notification.getNotificationDate() );

        return notificationDTO;
    }

    protected AnimalSummaryMinDTO animalToAnimalSummaryMinDTO(Animal animal) {
        if ( animal == null ) {
            return null;
        }

        AnimalSummaryMinDTO animalSummaryMinDTO = new AnimalSummaryMinDTO();

        animalSummaryMinDTO.setId( animal.getId() );
        animalSummaryMinDTO.setName( animal.getName() );
        animalSummaryMinDTO.setImgUrl( animal.getImgUrl() );

        return animalSummaryMinDTO;
    }

    protected List<AnimalSummaryMinDTO> animalSetToAnimalSummaryMinDTOList(Set<Animal> set) {
        if ( set == null ) {
            return null;
        }

        List<AnimalSummaryMinDTO> list = new ArrayList<AnimalSummaryMinDTO>( set.size() );
        for ( Animal animal : set ) {
            list.add( animalToAnimalSummaryMinDTO( animal ) );
        }

        return list;
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

    protected CareEventSummaryDTO careEventToCareEventSummaryDTO(CareEvent careEvent) {
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
}
