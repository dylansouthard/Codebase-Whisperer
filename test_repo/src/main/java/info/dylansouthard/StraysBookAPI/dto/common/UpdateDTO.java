package info.dylansouthard.StraysBookAPI.dto.common;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
@JsonIgnoreProperties(ignoreUnknown = false)
public class UpdateDTO {
    @JsonIgnore
    protected Map<String, Object> updates = new HashMap<>();

    @JsonAnySetter
    public void addUpdate(String key, Object value) {
        System.out.println("captured update: " + key + " = " + value);
        updates.put(key, value);
    }

    @Schema(hidden = true)
    public boolean hasUpdate(String fieldName) {
        return updates.containsKey(fieldName);
    }

    public Object getUpdateValue(String fieldName) {
        return updates.get(fieldName);
    }

    @JsonCreator
    public UpdateDTO() {
        this.updates = new HashMap<>();
    }

}
