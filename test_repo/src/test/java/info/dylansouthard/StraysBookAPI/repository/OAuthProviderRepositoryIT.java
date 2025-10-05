package info.dylansouthard.StraysBookAPI.repository;

import info.dylansouthard.StraysBookAPI.common.BaseDBTest;
import info.dylansouthard.StraysBookAPI.config.DummyTestData;
import info.dylansouthard.StraysBookAPI.model.enums.OAuthProviderType;
import info.dylansouthard.StraysBookAPI.model.user.OAuthProvider;
import info.dylansouthard.StraysBookAPI.model.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class OAuthProviderRepositoryIT extends BaseDBTest {
    @Autowired
    OauthProviderRepository oauthProviderRepository;

    @Test
    public void When_SearchForOAuthProviderByUser_OAuthProviderFound () {
        User user = userRepository.save(DummyTestData.createUser());
        user.addOAuthProvider(new OAuthProvider(OAuthProviderType.GOOGLE, "google123"));
        userRepository.save(user);

        Optional<OAuthProvider> fetchedProvider = oauthProviderRepository.findByProviderAndProviderUserId(OAuthProviderType.GOOGLE, "google123");

        assertAll("Provider assertions",
                ()->assertFalse(fetchedProvider.isEmpty(), "Fetched provider should not be empty"),
                ()->assertEquals(fetchedProvider.get().getUser().getId(), user.getId())
                );
    }

}
