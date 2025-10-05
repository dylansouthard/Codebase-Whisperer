package info.dylansouthard.StraysBookAPI.util.helpers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;

public class JsonHelper {
    public static JsonNode convertToJsonNode(Object rawValue) {
        ObjectMapper mapper = new ObjectMapper();

        return switch (rawValue) {
            case null -> NullNode.getInstance();
            case JsonNode node -> node;
            case String str -> {
                try {
                    yield mapper.readTree(str.trim());
                } catch (Exception e) {
                    yield new TextNode(str);
                }
            }
            default -> mapper.valueToTree(rawValue);
        };
    }
}
