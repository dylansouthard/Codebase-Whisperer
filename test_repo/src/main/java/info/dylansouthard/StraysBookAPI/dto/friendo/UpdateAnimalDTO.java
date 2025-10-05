package info.dylansouthard.StraysBookAPI.dto.friendo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import info.dylansouthard.StraysBookAPI.config.CustomUpdateAnimalDTODeserializer;
import info.dylansouthard.StraysBookAPI.dto.common.UpdateDTO;
import lombok.Getter;


@Getter
@JsonIgnoreProperties(ignoreUnknown = false)
@JsonDeserialize(using = CustomUpdateAnimalDTODeserializer.class)
public class UpdateAnimalDTO extends UpdateDTO {}
