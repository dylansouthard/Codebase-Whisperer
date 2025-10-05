package info.dylansouthard.StraysBookAPI.config;

import info.dylansouthard.StraysBookAPI.dto.friendo.CreateAnimalDTO;
import info.dylansouthard.StraysBookAPI.model.CareEvent;
import info.dylansouthard.StraysBookAPI.model.enums.AnimalType;
import info.dylansouthard.StraysBookAPI.model.enums.AuthTokenType;
import info.dylansouthard.StraysBookAPI.model.enums.CareEventType;
import info.dylansouthard.StraysBookAPI.model.enums.SexType;
import info.dylansouthard.StraysBookAPI.model.friendo.Animal;
import info.dylansouthard.StraysBookAPI.model.user.AuthToken;
import info.dylansouthard.StraysBookAPI.model.user.User;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

public class DummyTestData {
    @Getter
    public static final String defaultAnimalName = "Amelie";

   public static User createUser() {
        User user = new User();
        user.setDisplayName("Joe Schmo");
        return user;
    }

    public static Animal createAnimal() {
        return new Animal(AnimalType.CAT, SexType.FEMALE, defaultAnimalName);
    }

    public static CareEvent createCareEvent() {
        CareEvent careEvent = new CareEvent();
        careEvent.setId(1L);
        careEvent.setType(CareEventType.FED);
        careEvent.setDate(LocalDateTime.now());
        return careEvent;
    }

    public static CreateAnimalDTO generateCreateAnimalDTO() {
        CreateAnimalDTO dto = new CreateAnimalDTO();
        dto.setName(defaultAnimalName);
        dto.setType(AnimalType.CAT);
        dto.setSex(SexType.FEMALE);
        return dto;
    }

    public static AuthToken createExpiredRefreshToken(User user, String deviceId) {
        AuthToken token = new AuthToken();
        token.setUser(user);
        token.setDeviceId(deviceId);
        token.setType(AuthTokenType.REFRESH);
        token.setToken("expired-" + UUID.randomUUID());
        token.setIssuedAt(LocalDateTime.now().minusDays(10));
        token.setExpiresAt(LocalDateTime.now().minusDays(5)); // Already expired

        return token;
    }

}
