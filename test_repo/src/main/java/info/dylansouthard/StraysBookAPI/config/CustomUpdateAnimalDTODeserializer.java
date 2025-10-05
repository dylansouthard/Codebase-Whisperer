package info.dylansouthard.StraysBookAPI.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import info.dylansouthard.StraysBookAPI.dto.friendo.UpdateAnimalDTO;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

public class CustomUpdateAnimalDTODeserializer extends JsonDeserializer<UpdateAnimalDTO> {
    @Override
    public UpdateAnimalDTO deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        ObjectMapper mapper = (ObjectMapper) p.getCodec();
        JsonNode node = mapper.readTree(p);

        UpdateAnimalDTO dto = new UpdateAnimalDTO();
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();

        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            dto.addUpdate(field.getKey(), mapper.treeToValue(field.getValue(), Object.class));
        }

        return dto;
    }
}
