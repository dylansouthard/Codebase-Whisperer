package info.dylansouthard.StraysBookAPI.dto.friendo;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ImageUpdateResponseDTO extends FriendoSummaryMinDTO {
    public ImageUpdateResponseDTO(Long id, String name, String imgUrl) {
        super(id, name, imgUrl);
    }
}
