package info.dylansouthard.StraysBookAPI.service;

import info.dylansouthard.StraysBookAPI.constants.PaginationConsts;
import info.dylansouthard.StraysBookAPI.dto.common.PaginatedResponseDTO;
import info.dylansouthard.StraysBookAPI.dto.friendo.AnimalSummaryDTO;
import info.dylansouthard.StraysBookAPI.dto.user.WatchlistUpdateResultDTO;
import info.dylansouthard.StraysBookAPI.errors.AppException;
import info.dylansouthard.StraysBookAPI.errors.ErrorFactory;
import info.dylansouthard.StraysBookAPI.mapper.AnimalMapper;
import info.dylansouthard.StraysBookAPI.model.friendo.Animal;
import info.dylansouthard.StraysBookAPI.model.user.User;
import info.dylansouthard.StraysBookAPI.repository.AnimalRepository;
import info.dylansouthard.StraysBookAPI.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Service
public class WatchlistService {

    @Autowired
    private AnimalRepository animalRepository;

    @Autowired
    private AnimalMapper animalMapper;

    @Autowired
    private UserRepository userRepository;



    @Transactional
    public PaginatedResponseDTO<AnimalSummaryDTO> getWatchedAnimals(Long userId, Integer page, Integer pageSize) {
        Page<Animal> animalPage = animalRepository.findPaginatedWatchedAnimalsPaginated(
                userId,
                PaginationConsts.watchedAnimalPageable(page, pageSize)
        );

        return animalMapper.toPaginatedResponseDTO(animalPage);
    }

    @Transactional
    public WatchlistUpdateResultDTO updateWatchlist(Long userId, Long animalId) {
        User user = userRepository.findActiveById(userId).orElseThrow(ErrorFactory::userNotFound);
        Animal animal = animalRepository.findByActiveId(animalId).orElseThrow(ErrorFactory::animalNotFound);
        WatchlistUpdateResultDTO watchlistUpdateResultDTO = new WatchlistUpdateResultDTO();
        watchlistUpdateResultDTO.setAnimalId(animal.getId());
        try {
            if (user.getWatchedAnimals().contains(animal)) {
                user.removeWatchedAnimal(animal);
                watchlistUpdateResultDTO.setWatched(false);
            } else {
                user.addWatchedAnimal(animal);
                watchlistUpdateResultDTO.setWatched(true);
            }

            userRepository.save(user);

            return watchlistUpdateResultDTO;

        } catch (AppException e) {
            System.out.println(e.getMessage());
            throw ErrorFactory.internalServerError();
        }
    }

}
