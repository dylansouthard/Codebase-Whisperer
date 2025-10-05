package info.dylansouthard.StraysBookAPI.util.updaters;

import info.dylansouthard.StraysBookAPI.model.user.User;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

public class UserUpdater {

    public static Object getCurrentValue(User user, String field) {
        BeanWrapper wrapper = new BeanWrapperImpl(user);
        return wrapper.getPropertyValue(field);
    }

    public static void applyUpdate(User user, String field, Object value) {
        BeanWrapper wrapper = new BeanWrapperImpl(user);
        wrapper.setPropertyValue(field, value);
    }
}
