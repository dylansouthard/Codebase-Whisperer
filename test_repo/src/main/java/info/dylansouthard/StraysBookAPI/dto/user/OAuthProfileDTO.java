package info.dylansouthard.StraysBookAPI.dto.user;

import info.dylansouthard.StraysBookAPI.model.enums.OAuthProviderType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OAuthProfileDTO {
    private OAuthProviderType providerType;
    private String id;
    private String name;
    private String email;
    private String profileImageUrl;
}
