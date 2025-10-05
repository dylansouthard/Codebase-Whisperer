package info.dylansouthard.StraysBookAPI.util.updaters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import info.dylansouthard.StraysBookAPI.model.friendo.Animal;
import info.dylansouthard.StraysBookAPI.model.shared.GeoSchema;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.time.LocalDate;
import java.util.Map;

public class AnimalUpdater {

    public static Object getCurrentValue(Animal animal, String field) {
        BeanWrapper wrapper = new BeanWrapperImpl(animal);
        return wrapper.getPropertyValue(field);
    }

    public static void applyUpdate(Animal animal, String field, Object value) {
        BeanWrapper wrapper = new BeanWrapperImpl(animal);

        if ("location".equals(field) && value instanceof Map<?,?> mapVal) {
            double lat = ((Number) mapVal.get("latitude")).doubleValue();
            double lon = ((Number) mapVal.get("longitude")).doubleValue();
            wrapper.setPropertyValue("location", new GeoSchema(lat, lon));
        } else if ("born".equals(field) && value instanceof String){
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            LocalDate date = mapper.convertValue(value, LocalDate.class);
            wrapper.setPropertyValue("born", date);
        } else {
            wrapper.setPropertyValue(field, value);
        }
    }
}
