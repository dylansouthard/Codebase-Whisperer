package info.dylansouthard.StraysBookAPI.service;

import info.dylansouthard.StraysBookAPI.dto.ConditionDTO;
import info.dylansouthard.StraysBookAPI.dto.friendo.*;
import info.dylansouthard.StraysBookAPI.dto.user.UserSummaryMinDTO;
import info.dylansouthard.StraysBookAPI.errors.AppException;
import info.dylansouthard.StraysBookAPI.errors.ErrorFactory;
import info.dylansouthard.StraysBookAPI.mapper.AnimalMapper;
import info.dylansouthard.StraysBookAPI.mapper.CareEventMapper;
import info.dylansouthard.StraysBookAPI.mapper.UserMapper;
import info.dylansouthard.StraysBookAPI.model.CareEvent;
import info.dylansouthard.StraysBookAPI.model.enums.NotificationContentType;
import info.dylansouthard.StraysBookAPI.model.friendo.Animal;
import info.dylansouthard.StraysBookAPI.model.user.User;
import info.dylansouthard.StraysBookAPI.repository.AnimalRepository;
import info.dylansouthard.StraysBookAPI.repository.UserRepository;
import info.dylansouthard.StraysBookAPI.rules.update.AnimalUpdateRule;
import info.dylansouthard.StraysBookAPI.rules.update.UpdateRuleLoader;
import info.dylansouthard.StraysBookAPI.service.ImageHandler.AnimalImageHandlerService;
import info.dylansouthard.StraysBookAPI.util.updaters.AnimalUpdater;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static info.dylansouthard.StraysBookAPI.util.CareEventWeightUtil.weightCareEvent;

@Slf4j
@Service
public class AnimalService {
    @Autowired
    private AnimalRepository animalRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AnimalImageHandlerService animalImageHandlerService;
    @Autowired
    private AnimalMapper animalMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private CareEventService careEventService;
    @Autowired
    private CareEventMapper careEventMapper;
    @Autowired
    private NotificationService notificationService;


    @Transactional
    public AnimalDTO createAnimal(CreateAnimalDTO createDTO, MultipartFile image, User user) {
        if (createDTO == null) {
            throw ErrorFactory.invalidCreate();
        }
        User managedUser = userRepository.findById(user.getId())
                .orElseThrow(ErrorFactory::auth);

        String imgURL = animalImageHandlerService.handleImageUpload(image);

        try {
            Animal animal = animalMapper.fromCreateAnimalDTO(createDTO);
            animal.setImgUrl(imgURL);
            animal.setRegisteredBy(managedUser);
            Animal savedAnimal = animalRepository.save(animal);
            managedUser.addWatchedAnimal(animal);
            userRepository.save(managedUser);
            AnimalDTO dto = animalMapper.toAnimalDTO(savedAnimal);
            dto.setIsWatched(true);
            return dto;
        } catch (DataIntegrityViolationException e) {
            log.error(e.getMessage(), e);
            animalImageHandlerService.deleteImage(imgURL);
            throw ErrorFactory.invalidCreate();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            animalImageHandlerService.deleteImage(imgURL);
            throw ErrorFactory.internalServerError();
        }
    }

    public List<AnimalSummaryDTO> getAnimalsInArea(Double latitude, Double longitude, Double radius, Long userId) {
        if (latitude == null || longitude == null) throw ErrorFactory.invalidCoordinates();

        List<Animal> animals = animalRepository.findByLocation(latitude, longitude, radius);

        List<AnimalSummaryDTO> animalSummaries = animals.stream().map(animalMapper::toAnimalSummaryDTO).toList();

        if (userId != null) {
           List<Long> watchedIds = animalRepository.findWatchedIdsIn(userId, animals.stream().map(Animal::getId).toList());
           for (AnimalSummaryDTO animalSummaryDTO : animalSummaries) {
               if (watchedIds.contains(animalSummaryDTO.getId())) {
                   animalSummaryDTO.setIsWatched(true);
               }
           }
        }

        return animalSummaries;
    }

    @Transactional
    public AnimalDTO fetchAnimalDetails(Long id, Long userId) {
       Animal animal = fetchAnimalById(id);

        try {
            AnimalDTO animalDTO = animalMapper.toAnimalDTO(animal);

            List<CareEvent> recentCareEvents = careEventService.getAllRecentCareEventsByAnimalId(id);

            UserSummaryMinDTO primaryCareTaker = getPrimaryCaretaker(recentCareEvents, animal);

//            animalDTO.setRecentCareEvents(recentCareEvents.stream().map(careEventMapper::toCareEventSummaryDTO).toList());
            animalDTO.setPrimaryCaretaker(primaryCareTaker);

            if (userId != null) {
                animalDTO.setIsWatched(animalRepository.isWatchedByUser(userId, id));
            }

            return animalDTO;

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw ErrorFactory.internalServerError();
        }
    }

    @Transactional
    public AnimalDTO updateAnimal(Long animalId, UpdateAnimalDTO updateDTO, User loggedUser) {

        User user = userRepository.findActiveById(loggedUser.getId()).orElseThrow(ErrorFactory::auth);
        if (updateDTO == null) throw ErrorFactory.invalidParams();

        Animal animal = animalRepository.findById(animalId)
                .orElseThrow(ErrorFactory::animalNotFound);

        Map<String, Object> updates = updateDTO.getUpdates();

        UserSummaryMinDTO primaryCaretaker = getPrimaryCaretaker(animal);

        boolean canUpdateAsPrimaryCaretaker = canUpdateAsPrimaryCaretaker(user, primaryCaretaker);

        int numAuthorizedUpdates = 0;
        int numValidUpdates = 0;

        try {
            Map<String, AnimalUpdateRule> rules = UpdateRuleLoader.getAnimalRules();
            Map<String, Object> completedUpdates = new HashMap<>();
            for (Map.Entry<String, Object> entry : updates.entrySet()) {
                String key = entry.getKey();
                Object newValue = entry.getValue();

                AnimalUpdateRule rule = rules.get(key);
                if (rule == null || !rule.isValid(newValue)) continue;

                numValidUpdates++;

                Object currentValue = AnimalUpdater.getCurrentValue(animal, key);

                if (rule.hasUpdatePermission(canUpdateAsPrimaryCaretaker, currentValue)) {
                    numAuthorizedUpdates++;
                    AnimalUpdater.applyUpdate(animal, key, newValue);
                   completedUpdates.put(key, newValue);
                }
            }

            if (numValidUpdates == 0) throw ErrorFactory.invalidParams();
            if (numAuthorizedUpdates == 0) throw ErrorFactory.authForbidden();
            notificationService.createUpdateNotification(animal, completedUpdates, user, NotificationContentType.PROFILE_UPDATE);
            Animal updatedAnimal = animalRepository.save(animal);
            user.addWatchedAnimal(updatedAnimal);
            userRepository.save(user);
            AnimalDTO animalDTO = animalMapper.toAnimalDTO(updatedAnimal);
            animalDTO.setPrimaryCaretaker(primaryCaretaker);
            animalDTO.setIsWatched(true);
            return animalDTO;
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw ErrorFactory.internalServerError();
        }
    }

    public ImageUpdateResponseDTO updateAnimalImage(Long animalId, MultipartFile image, User user) {

        Animal animal = animalRepository.findByActiveId(animalId).orElseThrow(ErrorFactory::animalNotFound);

        UserSummaryMinDTO primaryCaretaker = getPrimaryCaretaker(animal);

        if (!canUpdateAsPrimaryCaretaker(user, primaryCaretaker)) throw ErrorFactory.authForbidden();

        String imgURL = animalImageHandlerService.handleImageUpload(image);

        String originalImage = animal.getImgUrl();
        try {
            animal.setImgUrl(imgURL);
            animalRepository.save(animal);
           if (originalImage != null && !originalImage.isEmpty()) animalImageHandlerService.deleteImage(originalImage);
           return new ImageUpdateResponseDTO(animal.getId(), animal.getName(), animal.getImgUrl());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            animalImageHandlerService.deleteImage(imgURL);
            throw ErrorFactory.internalServerError();
        }
    }

    public ConditionDTO updateCondition(Long animalId, ConditionDTO condition, Long userId) {
        User user = userRepository.findActiveById(userId).orElseThrow(ErrorFactory::auth);
        Animal animal = animalRepository.findByActiveId(animalId).orElseThrow(ErrorFactory::animalNotFound);

        AnimalMapper.setConditionDetails(condition, animal);


        notificationService.createUpdateNotification(animal, Map.of("condition", condition), user, NotificationContentType.CONDITION_UPDATE);
    }

    public void deleteAnimal(Long animalId, User user) {
        Animal animal = fetchAnimalById(animalId);
        UserSummaryMinDTO primaryCaretaker = getPrimaryCaretaker(animal);
        if (!canUpdateAsPrimaryCaretaker(user, primaryCaretaker)) throw ErrorFactory.authForbidden();
        try {
            animal.setShouldAppear(false);
            animal.setRemoveAt(LocalDateTime.now().plusDays(30));
            animalRepository.save(animal);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw ErrorFactory.internalServerError();
        }
    }

    @Transactional
    public UserSummaryMinDTO getPrimaryCaretaker(List<CareEvent> events, Animal animal) {
        if (events.isEmpty()) return null;

        Map<User, Integer> caretakerScores = new HashMap<>();

        User createdUser = animal.getRegisteredBy();

        LocalDateTime createdDate = animal.getCreatedAt();

        //  New animal bonus
        if (createdDate.isAfter(LocalDateTime.now().minusDays(30)) && createdUser != null) {
            caretakerScores.merge(createdUser, 5, Integer::sum);
        }

        // Score all care events
        for (CareEvent event : events) {
            User user = event.getRegisteredBy();
            if (user == null) continue;
            int score = weightCareEvent(event.getType());
            caretakerScores.merge(user, score, Integer::sum);
        }

        //  Find highest score
        int maxScore = caretakerScores.values().stream()
                .max(Integer::compareTo)
                .orElse(0);

        //  Find users with max score
        List<User> topUsers = caretakerScores.entrySet().stream()
                .filter(entry -> entry.getValue() == maxScore)
                .map(Map.Entry::getKey)
                .toList();

        if (topUsers.size() == 1) {
            return userMapper.toUserSummaryMinDTO(topUsers.get(0));
        }

        // Tie-breaker: latest care event
        User latestUser = events.stream()
                .filter(e -> e.getRegisteredBy() != null && topUsers.contains(e.getRegisteredBy()))
                .sorted(Comparator.comparing(CareEvent::getDate).reversed())  // Most recent first
                .map(CareEvent::getRegisteredBy)
                .findFirst()
                .orElse(null);

        if (latestUser != null) {
            return userMapper.toUserSummaryMinDTO(latestUser);
        }

        return null;
    }

    private UserSummaryMinDTO getPrimaryCaretaker(Animal animal) {
        List<CareEvent> recentCareEvents = careEventService.getAllRecentCareEventsByAnimalId(animal.getId());
        return getPrimaryCaretaker(recentCareEvents, animal);
    }

    private boolean canUpdateAsPrimaryCaretaker(User user, UserSummaryMinDTO primaryCaretaker) {
        return primaryCaretaker == null || primaryCaretaker.getId().equals(user.getId());
    }

    private Animal fetchAnimalById(Long id) {
        if (id == null) {
            throw ErrorFactory.invalidParams();
        }
        return animalRepository.findById(id)
                .orElseThrow(ErrorFactory::animalNotFound);
    }
}
