package info.dylansouthard.StraysBookAPI.service;

import info.dylansouthard.StraysBookAPI.common.BaseDBTest;
import info.dylansouthard.StraysBookAPI.config.DummyTestData;
import info.dylansouthard.StraysBookAPI.dto.user.WatchlistUpdateResultDTO;
import info.dylansouthard.StraysBookAPI.errors.ErrorFactory;
import info.dylansouthard.StraysBookAPI.model.friendo.Animal;
import info.dylansouthard.StraysBookAPI.model.user.User;
import info.dylansouthard.StraysBookAPI.testutils.ExceptionAssertionRunner;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class WatchlistServiceIT extends BaseDBTest {


    @Autowired
    private WatchlistService watchlistService;


    // ============================================================
    // Watchlist Tests
    // ============================================================

    /**
     * Tests adding and removing an animal to/from a user's watchlist and verifies the animal is correctly added/removed.
     */
    @Test
    public void When_AddingToggledInWatchlist_Expect_AnimalToggled() {
        User user = userRepository.save(DummyTestData.createUser());
        Animal animal = animalRepository.save(DummyTestData.createAnimal());
        WatchlistUpdateResultDTO addDTO = watchlistService.updateWatchlist(user.getId(), animal.getId());

        assertAll("add animal to watchlist assertions",
                () -> assertTrue(addDTO.isWatched()),
                () -> assertEquals(animal.getId(), addDTO.getAnimalId())
        );

        WatchlistUpdateResultDTO removeDTO = watchlistService.updateWatchlist(user.getId(), animal.getId());

        assertAll("add animal to watchlist assertions",
                () -> assertFalse(removeDTO.isWatched()),
                () -> assertEquals(animal.getId(), removeDTO.getAnimalId())
        );
    }

    /**
     * Tests that adding an invalid (nonexistent) animal to a user's watchlist throws the appropriate error.
     */
    @Test
    public void When_AddingInvalidAnimalToWatchlist_Expect_ThrowsError() {
        User user = userRepository.save(DummyTestData.createUser());

        Animal animal = DummyTestData.createAnimal();
        animal.setId(234L);
        ExceptionAssertionRunner.assertThrowsExceptionOfType(
                () -> watchlistService.updateWatchlist(user.getId(), animal.getId()),
                ErrorFactory.animalNotFound(),
                "Add invalid animal to watchlist"
        );
    }

    /**
     * Tests that adding an animal to the watchlist of an invalid (nonexistent) user throws the appropriate error.
     */
    @Test
    public void When_AddingAnimalToWatchlistOfInvalidUser_Expect_ThrowsError() {
        User user = DummyTestData.createUser();
        user.setId(234L);
        Animal animal = animalRepository.save(DummyTestData.createAnimal());
        ExceptionAssertionRunner.assertThrowsExceptionOfType(
                () -> watchlistService.updateWatchlist(user.getId(), animal.getId()),
                ErrorFactory.userNotFound(),
                "Add invalid animal to watchlist"
        );
    }
}
