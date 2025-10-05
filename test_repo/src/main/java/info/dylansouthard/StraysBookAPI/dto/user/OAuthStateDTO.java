package info.dylansouthard.StraysBookAPI.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OAuthStateDTO {

    @JsonProperty("device_id")
    private String deviceId;

    private String platform;
}
