package info.dylansouthard.StraysBookAPI.rules.update;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserUpdateRule extends UpdateRule{
}
