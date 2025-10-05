package info.dylansouthard.StraysBookAPI.cases;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class AnimalUpdateTestCase {
    private String desc;
    private boolean userIsPrimaryCaretaker;
    private Map<String, Object> originalValues;
    private Map<String, Object> updates;
    private boolean shouldSucceed;
    private String throwsError;

    @Override
    public String toString() {
        return desc;
    }
}
