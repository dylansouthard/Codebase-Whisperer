package info.dylansouthard.StraysBookAPI.service;

import info.dylansouthard.StraysBookAPI.constants.PaginationConsts;
import info.dylansouthard.StraysBookAPI.dto.careEvent.CareEventDTO;
import info.dylansouthard.StraysBookAPI.dto.careEvent.CreateSimpleCareEventDTO;
import info.dylansouthard.StraysBookAPI.dto.common.PaginatedResponseDTO;
import info.dylansouthard.StraysBookAPI.errors.ErrorFactory;
import info.dylansouthard.StraysBookAPI.mapper.CareEventMapper;
import info.dylansouthard.StraysBookAPI.model.CareEvent;
import info.dylansouthard.StraysBookAPI.model.friendo.Animal;
import info.dylansouthard.StraysBookAPI.model.user.User;
import info.dylansouthard.StraysBookAPI.repository.AnimalRepository;
import info.dylansouthard.StraysBookAPI.repository.CareEventRepository;
import info.dylansouthard.StraysBookAPI.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class CareEventService {
  @Autowired
    private CareEventRepository careEventRepository;

  @Autowired
  private AnimalRepository animalRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CareEventMapper careEventMapper;

    public List<CareEvent> getAllRecentCareEventsByAnimalId(Long animalId) {
      LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
      return careEventRepository.findRecentCareEventsByAnimalId(animalId, thirtyDaysAgo);
  }

  @Transactional
  public CareEventDTO createSimpleCareEvent(CreateSimpleCareEventDTO dto, Long animalId, Long userId) {
      Animal animal = animalRepository.findById(animalId).orElseThrow(ErrorFactory::animalNotFound);
      User user = userRepository.findActiveById(userId).orElseThrow(ErrorFactory::userNotFound);

      LocalDateTime regDate = dto.getDate() != null ? dto.getDate() : LocalDateTime.now();

      CareEvent careEvent = new CareEvent(dto.getType(), regDate, user, List.of(animal));
      if (dto.getNotes() != null) careEvent.setNotes(dto.getNotes());

      try {
          CareEvent savedCareEvent = careEventRepository.save(careEvent);
          animalRepository.save(animal);
          return careEventMapper.toCareEventDTO(savedCareEvent);
      } catch (Exception e) {
          log.error(e.getMessage());
          throw ErrorFactory.internalServerError();
      }
  }

  @Transactional
  public PaginatedResponseDTO<CareEventDTO> getCareEventsForAnimal(Long animalId, Integer page, Integer pageSize) {
      if (animalId == null) throw ErrorFactory.invalidParams();
      System.out.println("--getting care event");
      Animal _ = animalRepository.findById(animalId).orElseThrow(ErrorFactory::animalNotFound);
      System.out.println("--got animal");
      Page<CareEvent> careEventDTOPage = careEventRepository.findPaginatedCareEventsForAnimal(
              animalId,
              PaginationConsts.careEventPageable(page, pageSize)
      );
      System.out.println("--returning dto");
      return careEventMapper.toPaginatedResponseDTO(careEventDTOPage);
  }
}
