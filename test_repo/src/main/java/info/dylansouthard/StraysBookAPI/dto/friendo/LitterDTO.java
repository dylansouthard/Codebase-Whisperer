package info.dylansouthard.StraysBookAPI.dto.friendo;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
public class LitterDTO extends FriendoDTO {

    @ArraySchema(
            schema = @Schema(example = "{\"id\":1, \"name\":\"Puppy 1\", \"description\":\"A playful golden retriever puppy\", \"imgUrl\":\"https://www.example.com/puppy.jpg\"}"),
            arraySchema = @Schema(
                    description = "List of animals in the litter",
                    nullable = false,
                    requiredMode = Schema.RequiredMode.REQUIRED
            )
    )
    private List<FriendoSummaryDTO> animals = new ArrayList<>();
}

