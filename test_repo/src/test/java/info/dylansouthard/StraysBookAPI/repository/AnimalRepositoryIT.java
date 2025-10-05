package info.dylansouthard.StraysBookAPI.repository;

import info.dylansouthard.StraysBookAPI.model.SterilizationStatus;
import info.dylansouthard.StraysBookAPI.model.Vaccination;
import info.dylansouthard.StraysBookAPI.model.enums.AnimalType;
import info.dylansouthard.StraysBookAPI.model.enums.SexType;
import info.dylansouthard.StraysBookAPI.model.enums.VaccinationType;
import info.dylansouthard.StraysBookAPI.model.enums.VerificationStatusType;
import info.dylansouthard.StraysBookAPI.model.friendo.Animal;
import info.dylansouthard.StraysBookAPI.model.shared.GeoSchema;
import info.dylansouthard.StraysBookAPI.model.user.User;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class AnimalRepositoryIT extends RepositoryIT {


    @Test
    @Transactional
    void whenSavingAnimal_Expect_AnimalIsRetrievable() {
        // Save it
        Animal savedAnimal = animalRepository.save(validAnimal);

        // Fetch from DB
        Optional<Animal> retrievedAnimal = animalRepository.findById(savedAnimal.getId());

        // Assertions
        assertTrue(retrievedAnimal.isPresent(), "Animal should be found in DB");
        assertEquals("Amelie", retrievedAnimal.get().getName(), "Animal name should match");
    }

    @Test
    void whenSavingInvalidAnimal_Expect_ThrowException() {
        Animal invalidAnimal = new Animal();

        assertThrows(DataIntegrityViolationException.class, () -> animalRepository.save(invalidAnimal));
    }

    @Test
    public void When_Finding_InvalidAnimal_Expect_AnimalNotFoundSuccessfully() {
        Optional<Animal> animal = animalRepository.findById(999L);
        assertTrue(animal.isEmpty());
    }

    @Test
    public void When_Finding_InactiveAnimal_Expect_AnimalNotFoundSuccessfully() {

        validAnimal.setShouldAppear(false);
        Animal savedAnimal = animalRepository.save(validAnimal);
        Optional<Animal> animal = animalRepository.findByActiveId(savedAnimal.getId());
        assertTrue(animal.isEmpty());
    }

    @Test
    public void When_FindingAnimalsInArea_Expect_AnimalFoundSuccessfully() {
        Animal _ = animalRepository.save(new Animal(AnimalType.CAT, SexType.FEMALE, "アメリ", new GeoSchema(34.7376, 135.3415)));
        List<Animal> foundAnimals = animalRepository.findByLocation(34.7385, 135.3415, 1000);
        assertEquals(1, foundAnimals.size(), "Animal in area should be found");
    }

    @Test
    @Transactional
    public void When_FindingAnimalsOutOfArea_Expect_ZeroResults() {
        // Given: Save an animal far away from the search area
        Animal farAnimal = new Animal(AnimalType.DOG, SexType.MALE, "FarBoi", new GeoSchema(36.0000, 137.0000));
        animalRepository.save(farAnimal);

        // When: Search in a different area (1000m radius around Kyoto-ish)
        List<Animal> foundAnimals = animalRepository.findByLocation(34.7385, 135.3415, 1000);

        // Then: Should return empty
        assertEquals(0, foundAnimals.size(), "No animals should be found outside the search radius");
    }
    //UPDATE
    @Test
    public void When_UpdatingAnimal_Expect_AnimalUpdatedSuccessfully() {
        Animal animal = animalRepository.save(validAnimal);
        LocalDate birthday = LocalDate.now();
        animal.setBorn(birthday);
        animal.setDangerous(true);
        Animal updatedAnimal = animalRepository.save(animal);
        assertAll("Animal properties",
                ()->assertTrue(updatedAnimal.getDangerous()),
                ()->assertEquals(animal.getBorn(), birthday)
                );
    }

    //DELETE
    @Test
    public void When_DeletingAnimal_Expect_AnimalDeletedSuccessfully() {
        Animal animal = animalRepository.save(validAnimal);
        Long id = animal.getId();
        animalRepository.delete(animal);
        animalRepository.flush();
        assertTrue(animalRepository.findById(id).isEmpty(), "Deleted animal should not be found in DB");
    }



    //VALIDATION
    @Test
    public void When_UpdatingEmptyName_Expect_ThrowException() {
        Animal animal = animalRepository.save(validAnimal);
        animal.setName(null);
        assertThrows(DataIntegrityViolationException.class, () -> animalRepository.save(animal));
    }

    //RELATIONS
    @Test
    public void When_UpdatingSterilizationStatus_Expect_SterilizationStatusUpdatedSuccessfully() {
       validAnimal.setSterilizationStatus(new SterilizationStatus(true));
       Animal animal = animalRepository.save(validAnimal);

       Animal retrievedAnimal = animalRepository.findById(animal.getId()).get();

       SterilizationStatus sterilizationStatus = retrievedAnimal.getSterilizationStatus();

       long sterilizationStatusId = sterilizationStatus.getId();
       assertAll(
               "Sterilization status properties",
               () -> assertTrue(sterilizationStatus.getSterilized()),
               ()->assertEquals(VerificationStatusType.UNVERIFIED, sterilizationStatus.getVerificationStatus()
               ));

       User user = userRepository.save(validUser);
       sterilizationStatus.verify(user);
       sterilizationStatusRepository.saveAndFlush(sterilizationStatus);
       assertEquals(VerificationStatusType.VERIFIED,retrievedAnimal.getSterilizationStatus().getVerificationStatus());

       animalRepository.delete(animal);
       animalRepository.flush();
       Optional<SterilizationStatus> deletedSS = sterilizationStatusRepository.findById(sterilizationStatusId);
       assertTrue(deletedSS.isEmpty(), "Sterilization status should not be found in DB");
    }

    @Test
    @Transactional
    public void When_UpdatingVaccinationStatus_Expect_VaccinationStatusUpdatedSuccessfully() {
        User savedUser = userRepository.save(validUser);
        Vaccination vaccination = new Vaccination(VaccinationType.CCV, savedUser);
        validAnimal.getVaccinations().add(vaccination);
        Animal animal = animalRepository.save(validAnimal);

        Animal retrievedAnimal = animalRepository.findById(animal.getId()).get();

        assertEquals(1, retrievedAnimal.getVaccinations().size(), "Vaccination count should be 1");
        Vaccination retrievedVaccination = retrievedAnimal.getVaccinations().stream().findFirst().get();

        assertAll(
                "Vaccination assertions",
                ()->assertEquals(VaccinationType.CCV, retrievedVaccination.getType(), "Vaccination type should be CCV"),
                ()-> assertEquals(savedUser.getId(), retrievedVaccination.getRegisteredBy().getId(), "Vaccine should be registered by user ID")
                );

        animalRepository.delete(animal);
        animalRepository.flush();
        Optional<Vaccination> deletedVS = vaccinationRepository.findById(retrievedVaccination.getId());
        assertTrue(deletedVS.isEmpty(), "Vaccination status of deleted animal should not be found in DB");
    }
}


