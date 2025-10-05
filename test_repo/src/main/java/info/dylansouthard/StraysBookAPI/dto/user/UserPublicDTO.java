package info.dylansouthard.StraysBookAPI.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserPublicDTO extends UserSummaryMinDTO {

    @Schema(
            description = "Brief personal introduction or bio of the user",
            example = "Animal lover and community volunteer, passionate about helping stray animals.",
            nullable = true,
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private String intro;

    @Schema(
            description = "URL of the user's profile image",
            example = "https://example.com/images/profile/jimbo.jpg",
            nullable = true,
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private String profileImgUrl;

}
