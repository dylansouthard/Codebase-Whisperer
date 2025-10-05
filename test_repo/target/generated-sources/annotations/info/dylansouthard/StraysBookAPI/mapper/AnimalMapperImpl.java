package info.dylansouthard.StraysBookAPI.mapper;

import info.dylansouthard.StraysBookAPI.dto.friendo.AnimalDTO;
import info.dylansouthard.StraysBookAPI.dto.friendo.AnimalSummaryDTO;
import info.dylansouthard.StraysBookAPI.dto.friendo.AnimalSummaryMinDTO;
import info.dylansouthard.StraysBookAPI.dto.friendo.CreateAnimalDTO;
import info.dylansouthard.StraysBookAPI.dto.friendo.UpdateAnimalDTO;
import info.dylansouthard.StraysBookAPI.dto.vaccination.VaccinationSummaryDTO;
import info.dylansouthard.StraysBookAPI.model.Vaccination;
import info.dylansouthard.StraysBookAPI.model.friendo.Animal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.Generated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-08-11T16:29:08+0900",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 23.0.2 (Homebrew)"
)
@Component
public class AnimalMapperImpl implements AnimalMapper {

    @Autowired
    private ConditionMapper conditionMapper;
    @Autowired
    private StatusMapper statusMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private VaccinationMapper vaccinationMapper;

    @Override
    public AnimalSummaryDTO toAnimalSummaryDTO(Animal animal) {
        if ( animal == null ) {
            return null;
        }

        AnimalSummaryDTO animalSummaryDTO = new AnimalSummaryDTO();

        animalSummaryDTO.setStatus( statusMapper.mapStatus( animal ) );
        animalSummaryDTO.setCondition( conditionMapper.mapCondition( animal ) );
        animalSummaryDTO.setId( animal.getId() );
        animalSummaryDTO.setName( animal.getName() );
        animalSummaryDTO.setImgUrl( animal.getImgUrl() );
        animalSummaryDTO.setType( animal.getType() );
        animalSummaryDTO.setDescription( animal.getDescription() );
        animalSummaryDTO.setSex( animal.getSex() );
        animalSummaryDTO.setLastFed( animal.getLastFed() );

        animalSummaryDTO.setFriendoType( info.dylansouthard.StraysBookAPI.model.enums.FriendoType.ANIMAL );

        return animalSummaryDTO;
    }

    @Override
    public AnimalDTO toAnimalDTO(Animal animal) {
        if ( animal == null ) {
            return null;
        }

        AnimalDTO animalDTO = new AnimalDTO();

        animalDTO.setCondition( conditionMapper.mapCondition( animal ) );
        animalDTO.setStatus( statusMapper.mapStatus( animal ) );
        animalDTO.setRegisteredBy( userMapper.toUserSummaryMinDTO( animal.getRegisteredBy() ) );
        animalDTO.setId( animal.getId() );
        animalDTO.setName( animal.getName() );
        animalDTO.setImgUrl( animal.getImgUrl() );
        animalDTO.setType( animal.getType() );
        animalDTO.setDescription( animal.getDescription() );
        if ( animal.getDangerous() != null ) {
            animalDTO.setDangerous( animal.getDangerous() );
        }
        animalDTO.setLocation( animal.getLocation() );
        animalDTO.setLastUpdated( animal.getLastUpdated() );
        animalDTO.setNotes( animal.getNotes() );
        animalDTO.setSex( animal.getSex() );
        if ( animal.getHasCollar() != null ) {
            animalDTO.setHasCollar( animal.getHasCollar() );
        }
        animalDTO.setBorn( animal.getBorn() );
        animalDTO.setLastFed( animal.getLastFed() );
        animalDTO.setVaccinations( vaccinationSetToVaccinationSummaryDTOList( animal.getVaccinations() ) );

        animalDTO.setFriendoType( info.dylansouthard.StraysBookAPI.model.enums.FriendoType.ANIMAL );

        return animalDTO;
    }

    @Override
    public AnimalSummaryMinDTO toAnimalSummaryMinDTO(Animal animal) {
        if ( animal == null ) {
            return null;
        }

        AnimalSummaryMinDTO animalSummaryMinDTO = new AnimalSummaryMinDTO();

        animalSummaryMinDTO.setId( animal.getId() );
        animalSummaryMinDTO.setName( animal.getName() );
        animalSummaryMinDTO.setImgUrl( animal.getImgUrl() );

        return animalSummaryMinDTO;
    }

    @Override
    public Animal fromCreateAnimalDTO(CreateAnimalDTO dto) {
        if ( dto == null ) {
            return null;
        }

        Animal animal = new Animal();

        animal.setId( dto.getId() );
        animal.setNotes( dto.getNotes() );
        animal.setType( dto.getType() );
        animal.setName( dto.getName() );
        animal.setDescription( dto.getDescription() );
        animal.setDangerous( dto.isDangerous() );
        animal.setImgUrl( dto.getImgUrl() );
        animal.setLocation( dto.getLocation() );
        animal.setSex( dto.getSex() );
        animal.setHasCollar( dto.isHasCollar() );

        mapConditionDetails( dto, animal );

        return animal;
    }

    @Override
    public void updateAnimalFromDTO(UpdateAnimalDTO dto, Animal animal) {
        if ( dto == null ) {
            return;
        }
    }

    protected List<VaccinationSummaryDTO> vaccinationSetToVaccinationSummaryDTOList(Set<Vaccination> set) {
        if ( set == null ) {
            return null;
        }

        List<VaccinationSummaryDTO> list = new ArrayList<VaccinationSummaryDTO>( set.size() );
        for ( Vaccination vaccination : set ) {
            list.add( vaccinationMapper.toVaccinationSummaryDTO( vaccination ) );
        }

        return list;
    }
}
