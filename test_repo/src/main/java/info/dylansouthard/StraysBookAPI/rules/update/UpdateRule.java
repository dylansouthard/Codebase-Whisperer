package info.dylansouthard.StraysBookAPI.rules.update;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
abstract class UpdateRule {
    protected boolean nullable;
    protected boolean allowEmpty;

    public boolean isValid(Object value) {
        return (value != null || nullable) && (value != "" || allowEmpty);
    }
}
