package info.dylansouthard.StraysBookAPI.BasicTests;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import info.dylansouthard.StraysBookAPI.dto.friendo.UpdateAnimalDTO;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class DeserializationTest {

    @Test
    public void UpdateAnimalIsDeserialized() throws JsonProcessingException {
        String json = "{ \"name\": \"Rebecca\" }";

        ObjectMapper mapper = new ObjectMapper();
        UpdateAnimalDTO dto = mapper.readValue(json, UpdateAnimalDTO.class);

        System.out.println("Keys: " + dto.getUpdates().keySet());
    }
}
