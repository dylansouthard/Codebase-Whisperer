package info.dylansouthard.StraysBookAPI.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import info.dylansouthard.StraysBookAPI.model.shared.GeoSchema;

import java.io.IOException;

public class GeoSchemaSerializer extends StdSerializer<GeoSchema> {

    public GeoSchemaSerializer() {
        super(GeoSchema.class);
    }

    @Override
    public void serialize(GeoSchema value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        gen.writeNumberField("latitude", value.getLatitude());
        gen.writeNumberField("longitude", value.getLongitude());
        gen.writeEndObject();
    }
}