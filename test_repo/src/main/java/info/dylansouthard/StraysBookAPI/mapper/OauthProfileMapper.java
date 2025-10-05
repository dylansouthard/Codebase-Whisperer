package info.dylansouthard.StraysBookAPI.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import info.dylansouthard.StraysBookAPI.dto.user.OAuthProfileDTO;
import info.dylansouthard.StraysBookAPI.errors.ErrorFactory;
import info.dylansouthard.StraysBookAPI.model.enums.OAuthProviderType;
import org.springframework.stereotype.Component;

@Component
public class OauthProfileMapper {
    public OAuthProfileDTO map(OAuthProviderType provider, JsonNode profileJson) {
        return switch (provider) {
            case GOOGLE -> mapGoogle(profileJson);
            case FACEBOOK -> mapFacebook(profileJson);
            case LINE -> mapLine(profileJson);
            default -> throw ErrorFactory.invalidParams();
        };
    }


    private OAuthProfileDTO mapGoogle(JsonNode json) {
        return new OAuthProfileDTO(
                OAuthProviderType.GOOGLE,
                json.get("sub").asText(),
                json.get("name").asText(),
                json.has("email") ? json.get("email").asText() : null,
                json.has("picture") ? json.get("picture").asText() : null
        );
    }

    private OAuthProfileDTO mapFacebook(JsonNode json) {
        return new OAuthProfileDTO(
                OAuthProviderType.FACEBOOK,
                json.get("id").asText(),
                json.get("name").asText(),
                json.has("email") ? json.get("email").asText() : null,
                json.path("picture").path("data").path("url").asText()
        );
    }

    private OAuthProfileDTO mapLine(JsonNode json) {
        return new OAuthProfileDTO(
                OAuthProviderType.LINE,
                json.get("userId").asText(),
                json.get("displayName").asText(),
                null,
                json.has("pictureUrl") ? json.get("pictureUrl").asText() : null
        );
    }
}
