package info.dylansouthard.StraysBookAPI.dto.user;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateWatchlistDTO {
    @NotNull(message = "Animal ID cannot be null")
    private Long animalId;
}
