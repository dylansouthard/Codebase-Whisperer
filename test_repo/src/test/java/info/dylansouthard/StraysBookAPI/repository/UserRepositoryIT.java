package info.dylansouthard.StraysBookAPI.repository;

import info.dylansouthard.StraysBookAPI.errors.exceptions.DuplicateOAuthProviderException;
import info.dylansouthard.StraysBookAPI.model.enums.AnimalType;
import info.dylansouthard.StraysBookAPI.model.enums.AuthTokenType;
import info.dylansouthard.StraysBookAPI.model.enums.OAuthProviderType;
import info.dylansouthard.StraysBookAPI.model.enums.SexType;
import info.dylansouthard.StraysBookAPI.model.friendo.Animal;
import info.dylansouthard.StraysBookAPI.model.friendo.Litter;
import info.dylansouthard.StraysBookAPI.model.user.AuthToken;
import info.dylansouthard.StraysBookAPI.model.user.OAuthProvider;
import info.dylansouthard.StraysBookAPI.model.user.User;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;


public class UserRepositoryIT extends RepositoryIT {



    public AuthToken createAuthToken(String token) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime later = now.plusHours(1);

        // Add the first token
       return new AuthToken(AuthTokenType.ACCESS, token, now, later, "device123");
    }


    // --- TEST METHODS ---

    // 1. Basic CRUD Operations

    //Create
    @Test
    public void When_SavingValidUser_Expect_UserPersistedSuccessfully() {

        User savedUser = userRepository.save(validUser);
        assertNotNull(savedUser.getId(), "ID should not be null after saving");
        assertEquals("Bing Bong", savedUser.getDisplayName(), "Display name should match");
    }


    //Retrieve
    @Test
    public void When_Finding_ValidUser_Expect_UserFoundSuccessfully() {
        User savedUser = userRepository.save(validUser);
        assertTrue(userRepository.findActiveById(savedUser.getId()).isPresent(), "User should be found");
    }

    @Test
    public void When_Finding_InvalidUser_Expect_UserNotFoundSuccessfully() {
        Optional<User> user = userRepository.findActiveById(999L);
        assertTrue(user.isEmpty());
    }



    //Update
    @Test
    public void When_UpdatingValidUser_Expect_UserUpdatedSuccessfully() {
        User user = userRepository.save(validUser);
        user.setDisplayName("New");
        user.setEmail("new@new.com");
        User savedUser = userRepository.save(user);
        assertAll("User properties",
                () -> assertEquals("New", savedUser.getDisplayName(), "Display name should match"),
                () -> assertEquals("new@new.com", savedUser.getEmail(), "Email should match")
        );
    }

    //Delete
    @Test
    public void When_Deleting_ValidUser_Expect_UserDeletedSuccessfully() {

        User user = userRepository.save(validUser);
        Long id = user.getId();
        userRepository.delete(user);
        assertTrue(userRepository.findById(id).isEmpty(), "User should not exist after deletion");

        User secondUser = userRepository.save(new User("name", "email@email.com"));
        secondUser.setIsDeleted(true);
        userRepository.save(secondUser);
        userRepository.flush();

        assertTrue(userRepository.findActiveById(secondUser.getId()).isEmpty(), "User should not exist after deletion");
    }


    //2. Validation
    @Test
    public void When_SavingInvalidUser_Expect_UserNotPersistedSuccessfully() {
        User user = new User();
        assertThrows(Exception.class, () -> userRepository.save(user));
    }

    @Test
    public void When_Given_Duplicate_Email_Expect_ThrowException() {
        User user = userRepository.save(validUser);
        User newUser = new User("BobDole", user.getEmail());
        assertThrows(Exception.class, () -> userRepository.save(newUser));
    }

    @Test
    public void When_UpdatingDuplicateEmail_Expect_ThrowException() {
        User user = userRepository.save(validUser);
        User newUser = userRepository.save(new User("New", "new@new.com"));
        newUser.setEmail(user.getEmail());

        assertThrows(Exception.class, () -> {
            userRepository.save(newUser);
            userRepository.flush();
        });
    }

    @Test
    public void When_UpdatingNullDisplayName_Expect_ThrowException() {
        User user = userRepository.save(validUser);
        user.setDisplayName(null);
        assertThrows(Exception.class, () -> {
            userRepository.save(user);
            userRepository.flush();
        });
    }

    // 2. Relationships
    //User authentication

    @Test
    public void When_UpdatingOAuthProviders_Expect_OAthProviderCreatedSuccessfully() {
        User user = userRepository.save(validUser);
        user.addOAuthProvider(new OAuthProvider(OAuthProviderType.GOOGLE, "123google"));
        User savedUser = userRepository.save(user);
        assertEquals("123google", savedUser.getOAuthProviders().stream().findFirst().get().getProviderUserId(), "OAuthProviderID should match saved id");
    }

    @Test
    @Transactional
    public void When_RemovingOAuthProvider_Expect_ProviderRemovedFromDB() {
        //Save the user and add an OAuthProvider
        User user = userRepository.save(validUser);
        OAuthProvider provider = new OAuthProvider(OAuthProviderType.GOOGLE, "123google");
        user.addOAuthProvider(provider);
        userRepository.saveAndFlush(user);

        //Verify the provider exists in the database
        User savedUser = userRepository.findById(user.getId()).orElseThrow();
        assertEquals(1, savedUser.getOAuthProviders().size(), "User should have 1 OAuth provider");
        assertEquals("123google", savedUser.getOAuthProviders().stream().findFirst().get().getProviderUserId(), "OAuthProviderID should match");

        // Remove the provider and save the user
        savedUser.getOAuthProviders().clear();
        userRepository.saveAndFlush(savedUser);

        //Verify the provider is removed from the database using EntityManager
        String jpql = "SELECT COUNT(p) FROM OAuthProvider p WHERE p.providerUserId = :providerUserId";
        Long count = entityManager.createQuery(jpql, Long.class)
                .setParameter("providerUserId", "123google")
                .getSingleResult();

        assertEquals(0, count, "OAuthProvider should be removed from the database");
    }


    @Test
    public void When_AddingDuplicateOAuthProvider_Expect_ThrowException() {
        User user = userRepository.save(validUser);
        user.addOAuthProvider(new OAuthProvider(OAuthProviderType.GOOGLE, "123google"));
        User savedUser = userRepository.save(user);
        assertThrows(DuplicateOAuthProviderException.class, ()->savedUser.addOAuthProvider(new OAuthProvider(OAuthProviderType.GOOGLE, "456google")), "Should throw exception on duplicate OAuthProvider" );
    }



    @Test
    @Transactional
    public void When_AddingAuthToken_Expect_TokenAddedSuccessfully() {
        User user = userRepository.save(validUser);


        // Create and add the token
        AuthToken authToken = createAuthToken("token123");
        user.addAuthToken(authToken);
        userRepository.save(user);

        // Reload the user to verify persistence
        User savedUser = userRepository.findById(user.getId()).orElseThrow();

        // Assertions
        assertEquals(1, savedUser.getAuthTokens().size(), "User should have 1 token after addition");
        assertEquals(authToken.getToken(), savedUser.getAuthTokens().stream().findFirst().get().getToken(), "Token should match saved token");
        assertEquals("device123", savedUser.getAuthTokens().stream().findFirst().get().getDeviceId(), "Device ID should match");
    }

    @Test
    @Transactional
    public void When_AddingAuthTokenForSameDevice_Expect_TokenReplacedSuccessfully() {
        User user = userRepository.save(validUser);


        // Add the first token
        AuthToken authTokenOne = createAuthToken("token123");
        user.addAuthToken(authTokenOne);
        userRepository.save(user);

        // Add a second token for the same device
        AuthToken authTokenTwo = createAuthToken("token456");
        user.addAuthToken(authTokenTwo);
        userRepository.save(user);

        // Reload the user to verify persistence
        User savedUser = userRepository.findById(user.getId()).orElseThrow();

        // Assertions
        assertEquals(1, savedUser.getAuthTokens().size(), "User should only have 1 token after replacement");
        assertTrue(savedUser.getAuthTokens().stream()
                .filter(token -> token.getToken().equals(authTokenOne.getToken())).findFirst().isEmpty(), "User should not contain the first token");

        assertEquals(authTokenTwo.getToken(), savedUser.getAuthTokens().stream().findFirst().get().getToken(), "Token should match the new token ID");
    }

    @Test
    @Transactional
    public void When_RemovingAuthTokenFromUser_Expect_TokenRemovedFromDBSuccessfully() {
        User user = userRepository.save(validUser);
        AuthToken authToken = createAuthToken("token123");
        user.addAuthToken(authToken);
        userRepository.saveAndFlush(user);
        AuthToken savedAuthToken = user.getAuthTokens().stream().findFirst().get();

        assertTrue(authTokenRepository.findById(savedAuthToken.getId()).isPresent(), "Added token should be found");

        user.getAuthTokens().clear();
        userRepository.saveAndFlush(user);

        assertFalse(authTokenRepository.findById(savedAuthToken.getId()).isPresent(), "Removed token should not be found");
    }

    private void addAnimalToUser(Animal animal, User user) {
        user.addWatchedAnimal(animal);
        userRepository.saveAndFlush(user);
        animalRepository.saveAndFlush(animal);
    }

    private void addLitterToUser(Litter litter, User user) {
        user.addWatchedLitter(litter);
        userRepository.saveAndFlush(user);
        litterRepository.saveAndFlush(litter);
    }

    @Test
    @Transactional
    public void When_UpdatingAnimalWatchlist_Expect_WatchlistUpdatedSuccessfully() {


        // Arrange - Save user and animal
        User user = userRepository.save(validUser);
        Animal animal = animalRepository.save(validAnimal);

        // Act - Add animal to user's watchlist
        addAnimalToUser(animal, user);

        // Assert - Animal should be in watchlist
        assertEquals(1, user.getWatchedAnimals().size(), "Animal should be added to user list");
        Animal fetchedAnimal = animalRepository.findById(animal.getId()).orElseThrow();
        assertEquals(1, fetchedAnimal.getUsersWatching().size(), "Animal should be watched by user");

        // Act - Remove animal from watchlist
        user.removeWatchedAnimal(animal);
        userRepository.saveAndFlush(user);
        animalRepository.saveAndFlush(animal);

        // Assert - Animal should no longer be in watchlist
        assertEquals(0, user.getWatchedAnimals().size(), "Animal should be removed from user list");
        fetchedAnimal = animalRepository.findById(animal.getId()).orElseThrow();
        assertEquals(0, fetchedAnimal.getUsersWatching().size(), "Animal should no longer be watched by user");

        // Act - Add animal to user's watchlist
        user.addWatchedAnimal(animal);
        userRepository.saveAndFlush(user);
        animalRepository.saveAndFlush(animal);
        animalRepository.deleteById(animal.getId());

        assertEquals(0, user.getWatchedAnimals().size(), "Animal should be removed from user list");


        Animal replacementAnimal = animalRepository.save(new Animal(AnimalType.CAT, SexType.FEMALE, "Sabi" ));
        addAnimalToUser(replacementAnimal, user);
        userRepository.deleteById(user.getId());

        assertEquals(0, replacementAnimal.getUsersWatching().size(), "User should be removed from animal list");
    }

    @Test
    @Transactional
    public void When_UpdatingLitterWatchlist_Expect_WatchlistUpdatedSuccessfully() {


        // Arrange - Save user and litter
        User user = userRepository.save(validUser);
        Litter litter = litterRepository.saveAndFlush(constructValidLitter());

        Animal animal = litter.getAnimals().stream().findFirst().get();
        addAnimalToUser(animal, user);
        // Act - Add litter to user's watchlist
        addLitterToUser(litter, user);

        // Assert - Litter and animals should be in watchlist
        assertAll("Litter watchlist adds",
                ()-> assertEquals(1, user.getWatchedLitters().size(), "Litter should be added to user list"),
                ()->assertEquals(2, user.getWatchedAnimals().size(), "All unique animals in litter should be added to user list")
        );
        assertEquals(1, user.getWatchedLitters().size(), "Litter should be added to user list");
        Litter fetchedLitter = litterRepository.findById(litter.getId()).orElseThrow();
        assertEquals(1, fetchedLitter.getUsersWatching().size(), "Litter should be watched by user");

        // Act - Remove litter from watchlist
        user.removeWatchedLitter(litter);
        userRepository.saveAndFlush(user);
        litterRepository.saveAndFlush(litter);

        // Assert - Litter should no longer be in watchlist
        assertEquals(0, user.getWatchedLitters().size(), "Litter should be removed from user list");
        fetchedLitter = litterRepository.findById(litter.getId()).orElseThrow();
        assertEquals(0, fetchedLitter.getUsersWatching().size(), "Litter should no longer be watched by user");

        // Act - Add litter to user's watchlist
        user.addWatchedLitter(litter);
        userRepository.saveAndFlush(user);
        litterRepository.saveAndFlush(litter);
        litterRepository.deleteById(litter.getId());

        assertAll(
                "Litter watchlist deletions",
                ()-> assertEquals(0, user.getWatchedLitters().size(), "Litter should be removed from user list"),
                ()-> assertEquals(2, user.getWatchedAnimals().size(), "Animals should not be removed from user list")
                );

    }

    @Test
    @Transactional
    public void When_UserDeleted_Expect_UserRemovedFromLitterUsersWatching() {
        User user = userRepository.save(validUser);
        Litter litter = litterRepository.save(constructValidLitter());
        addLitterToUser(litter, user);

        userRepository.deleteById(user.getId());

        Litter retreivedLitter = litterRepository.findById(litter.getId()).orElseThrow();

        assertEquals(0, retreivedLitter.getUsersWatching().size(), "User should be removed from user list");
    }




    //TODO: TEST OTHER RELATIONSHIPS
}
