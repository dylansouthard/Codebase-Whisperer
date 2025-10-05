package info.dylansouthard.StraysBookAPI.config;

import info.dylansouthard.StraysBookAPI.model.enums.OAuthProviderType;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class OAuthProperties {
    private final Environment env;
    private final String OAUTH_STUB = "spring.security.oauth2.client.";
    private final String REG_STUB = OAUTH_STUB + "registration.";
    private final String PROVIDER_STUB = OAUTH_STUB + "provider.";

    public OAuthProperties(Environment env) {
        this.env = env;
    }

    private String getProperty(String stub, OAuthProviderType provider, String key) {
        return env.getProperty(stub + provider.toString().toLowerCase() + "." + key);
    }

    private String getProviderProperty(OAuthProviderType provider, String key) {
        return getProperty(PROVIDER_STUB, provider, key);
    }

    private String getRegistrationProperty(OAuthProviderType provider, String key) {
        return getProperty(REG_STUB, provider, key);
    }

    public String getRedirectUri(OAuthProviderType provider) {
        return getRegistrationProperty(provider, "redirect-uri");
    }

    public String getAuthorizationUri(OAuthProviderType provider) {
        return getProviderProperty(provider, "authorization-uri");
    }

    public String getClientId(OAuthProviderType provider) {
        return getRegistrationProperty(provider, "client-id");
    }

    public String getScope(OAuthProviderType provider) {
        return getRegistrationProperty(provider, "scope");
    }

    public String getTokenUri(OAuthProviderType provider) {
        return getProviderProperty(provider, "token-uri");
    }

    public String getSecret(OAuthProviderType provider) {
        return getRegistrationProperty(provider, "client-secret");
    }

    public String getUserInfoUri (OAuthProviderType provider) {
        return getProviderProperty(provider, "user-info-uri");
    }

    public String getUserNameAttribute (OAuthProviderType provider) {
        return getProviderProperty(provider, "user-name-attribute");
    }
}