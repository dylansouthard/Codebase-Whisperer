package info.dylansouthard.StraysBookAPI.dto.friendo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public abstract class FriendoSummaryMinDTO {
    @Schema(
            description = "Unique ID of the animal/litter",
            example = "1",
            nullable = false,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long id;

    @Schema(
            description = "Name of the animal/litter",
            example = "Rebecca Anderson Clive",
            nullable = false,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String name;

    @Schema(
            description = "Image URL of the animal/litter",
            example = "https://www.google.com/img.jpg",
            nullable = true,
            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            format = "uri"
    )
    private String imgUrl;

    @Schema(
            description = "Indicates whether the current user is watching this animal/litter",
            example = "true",
            nullable = false,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Boolean isWatched = false;

    public FriendoSummaryMinDTO(Long id, String name, String imgUrl) {
        this.id = id;
        this.name = name;
        this.imgUrl = imgUrl;
    }
}
