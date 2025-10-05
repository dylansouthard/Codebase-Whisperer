package info.dylansouthard.StraysBookAPI.serializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import info.dylansouthard.StraysBookAPI.dto.friendo.AnimalDTO;
import info.dylansouthard.StraysBookAPI.model.shared.GeoSchema;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GeoSchemaSerializationTest {


@Test
    public void testGeoSchemaSerialization() throws Exception {
        // Create a GeoSchema with known values.
        GeoSchema geo = new GeoSchema(34.7391, 135.3421);

        // Configure an ObjectMapper.
        ObjectMapper mapper = new ObjectMapper();
        // Register the module if needed; in this case, our serializer should be picked up via annotation.
        // Disable timestamps for date types if needed (not needed for GeoSchema).
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Serialize the GeoSchema to JSON.
        String json = mapper.writeValueAsString(geo);
        System.out.println("Serialized GeoSchema: " + json);

        // Define the expected JSON output.
        String expected = "{\"latitude\":34.7391,\"longitude\":135.3421}";

        // Assert equality.
        assertEquals(expected, json, "GeoSchema did not serialize as expected.");
    }
    @Test
    public void testAnimalDTOSerialization() throws Exception {
        // Create an AnimalDTO and set location
        AnimalDTO animal = new AnimalDTO();
        animal.setId(42L);
        animal.setName("Testy");
        animal.setLocation(new GeoSchema(34.7391, 135.3421));

        // Use Jackson to serialize
        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        String json = mapper.writeValueAsString(animal);
        System.out.println("Serialized AnimalDTO: " + json);

        // Basic check for embedded location structure
        assert json.contains("\"location\"");
        assert json.contains("\"latitude\":34.7391");
        assert json.contains("\"longitude\":135.3421");
    }
}
