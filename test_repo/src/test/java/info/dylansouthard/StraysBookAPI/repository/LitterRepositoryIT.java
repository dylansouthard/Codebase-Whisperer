package info.dylansouthard.StraysBookAPI.repository;

import info.dylansouthard.StraysBookAPI.model.enums.AnimalType;
import info.dylansouthard.StraysBookAPI.model.enums.SexType;
import info.dylansouthard.StraysBookAPI.model.friendo.Animal;
import info.dylansouthard.StraysBookAPI.model.friendo.Litter;
import info.dylansouthard.StraysBookAPI.model.shared.GeoSchema;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class LitterRepositoryIT extends RepositoryIT {

    @Test
    @Transactional
    void When_SavingLitter_Expect_LitterIsRetrievable() {
        // Save it
        Litter savedLitter = litterRepository.saveAndFlush(constructValidLitter());

        // Fetch from DB
        Optional<Litter> retrievedLitter = litterRepository.findById(savedLitter.getId());

        // Assertions

        assertAll("Litter and Animal Assertion",
                ()->assertTrue(retrievedLitter.isPresent(), "Litter should be found in DB"),
                ()->assertEquals("New Litter", retrievedLitter.get().getName(), "Litter name should match"),
                ()->assertEquals(2, retrievedLitter.get().getAnimals().size(), "Litter should have 2 animals")
                );

        Optional<Animal> retrievedAnimal = animalRepository.findById(retrievedLitter.get().getAnimals().stream().findFirst().get().getId());

        assertAll("Retrieved litter animal assertions",
                ()->assertTrue(retrievedAnimal.isPresent(), "Litter Animal should be found in DB"),
                ()->assertEquals(savedLitter.getId(), retrievedAnimal.get().getLitter().getId(), "Litter should be saved in animal")
                );
    }

    @Test
    public void When_FindingLittersInArea_Expect_AnimalFoundSuccessfully() {
        Animal newAnimal = new Animal(AnimalType.CAT, SexType.FEMALE, "アメリ");
        Litter _ = litterRepository.saveAndFlush(new Litter(AnimalType.CAT, "NewLitter", List.of(newAnimal), new GeoSchema(34.7376, 135.3415)));
        List<Litter> foundLitters = litterRepository.findByLocation(34.7385, 135.3415, 1000);
        List<Animal> foundAnimals = animalRepository.findByLocation(34.7385, 135.3415, 1000);
        assertAll("Litter location assertions",
                ()->assertEquals(1, foundLitters.size(), "Litter in area should be found"),
                ()->assertEquals(1, foundAnimals.size(), "Litter Animals in area should be found")
        );
    }
    @Test
    @Transactional
    void whenUpdatingLitter_Expect_LitterIsUpdated() {
        Litter savedLitter = litterRepository.save(constructValidLitter());
        savedLitter.setName("UpdatedName");
        Animal newAnimal = new Animal(AnimalType.CAT, SexType.FEMALE, "Rookie");
        Animal savedAnimal = animalRepository.saveAndFlush(newAnimal);

        savedLitter.addAnimal(savedAnimal);

        Litter updatedLitter = litterRepository.saveAndFlush(savedLitter);


        assertAll("Litter update assertions",
                ()->assertEquals("UpdatedName", updatedLitter.getName(), "Litter name should match"),
                ()->assertEquals(3, updatedLitter.getAnimals().size(), "Litter should have 3 animals")
                );


     Animal retrievedAnimal = animalRepository.findById(savedAnimal.getId()).get();

        assertAll("Updated Litter Animal Assertions",
               ()->assertEquals(updatedLitter.getId(), retrievedAnimal.getLitter().getId(), "Animal should be last added")
                );
    }

    @Test
    @Transactional
    void whenDeletingLitter_Expect_LitterDeletedAndAnimalsPersisted() {
        Litter savedLitter = litterRepository.saveAndFlush(constructValidLitter());
        Long firstAnimalId = savedLitter.getAnimals().stream().findFirst().get().getId();
        litterRepository.delete(savedLitter);
        Optional<Litter> retrievedLitter = litterRepository.findById(savedLitter.getId());
        Optional<Animal> retrievedAnimal = animalRepository.findById(firstAnimalId);

        assertAll("Litter deletions",
                ()->assertTrue(retrievedLitter.isEmpty(), "Litter should be deleted"),
                ()->assertTrue(retrievedAnimal.isPresent(), "Animal should be deleted"),
                ()->assertNull(retrievedAnimal.get().getLitter(), "Litter should be removed from animal")
                );
    }

    @Test
    @Transactional
    void whenLitterAnimalDeleted_Expect_AnimalRemovedFromLitter() {
        Litter savedLitter = litterRepository.saveAndFlush(constructValidLitter());
        Animal newAnimal = animalRepository.save(new Animal(AnimalType.CAT, SexType.MALE, "Romeow"));
       savedLitter.addAnimal(newAnimal);
       Litter updatedLitter = litterRepository.saveAndFlush(savedLitter);
       animalRepository.delete(newAnimal);

       Litter retrievedLitter = litterRepository.findById(updatedLitter.getId()).get();

        assertEquals(2, retrievedLitter.getAnimals().size(), "Litter should have only 2 animals");
    }
}
