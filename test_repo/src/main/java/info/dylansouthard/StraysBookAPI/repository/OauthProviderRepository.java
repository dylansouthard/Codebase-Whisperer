package info.dylansouthard.StraysBookAPI.repository;

import info.dylansouthard.StraysBookAPI.model.enums.OAuthProviderType;
import info.dylansouthard.StraysBookAPI.model.user.OAuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OauthProviderRepository extends JpaRepository<OAuthProvider, Long> {
    Optional<OAuthProvider> findByProviderAndProviderUserId(OAuthProviderType provider, String providerUserId);
}
