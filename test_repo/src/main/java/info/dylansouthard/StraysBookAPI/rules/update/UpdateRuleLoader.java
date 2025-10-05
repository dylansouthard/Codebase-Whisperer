package info.dylansouthard.StraysBookAPI.rules.update;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.Map;

/**
 * Loads update rules for animals from a JSON file located in the classpath.
 *
 * This loader provides a lazy-loaded, cached map of update rules, which define
 * access restrictions and update conditions for various animal fields.
 *
 * The rules are expected to be stored at:
 * resources/rules/update-rules/animal_update_rules.json
 */
public class UpdateRuleLoader {
    private static Map<String, AnimalUpdateRule> animalRules;
    private static Map<String, UserUpdateRule> userRules;

    /**
     * Returns the update rules for animals. Loads from JSON only once.
     *
     * @return Map of field name → AnimalUpdateRule
     * @throws RuntimeException if the rules file cannot be read
     */
    public static Map<String, AnimalUpdateRule> getAnimalRules() {
        if (animalRules == null) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                ClassPathResource resource = new ClassPathResource("rules/update-rules/animal_update_rules.json");
                animalRules = mapper.readValue(resource.getInputStream(), new TypeReference<>() {});
            } catch (IOException e) {
                throw new RuntimeException("Failed to load animal update rules");
            }
        }
        return animalRules;
    }

    /**
     * Returns the update rules for users. Loads from JSON only once.
     *
     * @return Map of field name → UserUpdateRule
     * @throws RuntimeException if the rules file cannot be read
     */
    public static Map<String, UserUpdateRule> getUserRules() {
        if (userRules == null) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                ClassPathResource resource = new ClassPathResource("rules/update-rules/user_update_rules.json");
                userRules = mapper.readValue(resource.getInputStream(), new TypeReference<>() {});
            } catch (IOException e) {
                throw new RuntimeException("Failed to load user update rules");
            }

        }
        return userRules;
    }

}
