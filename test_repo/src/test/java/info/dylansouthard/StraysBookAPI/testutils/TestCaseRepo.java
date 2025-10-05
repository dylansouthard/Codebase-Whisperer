package info.dylansouthard.StraysBookAPI.testutils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import info.dylansouthard.StraysBookAPI.cases.AnimalUpdateTestCase;
import info.dylansouthard.StraysBookAPI.cases.UserUpdateTestCase;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

public class TestCaseRepo {


    public static Stream<AnimalUpdateTestCase> getAnimalUpdateTestCases() throws IOException {
            ClassPathResource resource = new ClassPathResource("test-data/animal_update_cases.json");
            ObjectMapper mapper = new ObjectMapper();
            List<AnimalUpdateTestCase> testCases = mapper.readValue(resource.getInputStream(), new TypeReference<>() {});
            return testCases.stream();
    }

    public static Stream<UserUpdateTestCase> getUserUpdateTestCases() throws IOException {
        ClassPathResource resource = new ClassPathResource("test-data/user_update_cases.json");
        ObjectMapper mapper = new ObjectMapper();
        List<UserUpdateTestCase> testCases = mapper.readValue(resource.getInputStream(), new TypeReference<>() {});
        return testCases.stream();
    }

}
