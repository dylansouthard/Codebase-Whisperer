package info.dylansouthard.StraysBookAPI.testutils;

import com.fasterxml.jackson.databind.ObjectMapper;
import info.dylansouthard.StraysBookAPI.model.notification.AlertNotification;
import info.dylansouthard.StraysBookAPI.model.notification.Notification;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class NotificationLoader {

    public static <T extends Notification> List<T> loadDummyNotifications(Class<T> clazz) throws IOException {
        String typeName = clazz.equals(AlertNotification.class) ? "alert" : "update";
       return loadNotifications("src/test/resources/test-data/dummy_notifications_" + typeName + ".json", clazz);
    }


    public static <T extends Notification> List<T> loadNotifications(String filePath, Class<T> clazz) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(
                new File(filePath),
                mapper.getTypeFactory().constructCollectionType(List.class, clazz)
        );
    }
}
