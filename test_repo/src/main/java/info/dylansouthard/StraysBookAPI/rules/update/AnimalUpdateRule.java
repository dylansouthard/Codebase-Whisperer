package info.dylansouthard.StraysBookAPI.rules.update;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import info.dylansouthard.StraysBookAPI.rules.enums.AccessLevel;
import lombok.Getter;

import java.util.Objects;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AnimalUpdateRule extends UpdateRule{
    private AccessLevel access;
    private Object condition;

    public boolean hasUpdatePermission(boolean canUpdateAsPrimaryCaretaker, Object currentValue) {
       return access == AccessLevel.PUBLIC
                || canUpdateAsPrimaryCaretaker
                || (access == AccessLevel.CONDITIONAL
                && Objects.equals(String.valueOf(currentValue), String.valueOf(condition)));
    }
}
