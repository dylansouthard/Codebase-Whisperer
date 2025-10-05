package info.dylansouthard.StraysBookAPI.util.helpers;

import java.time.LocalDateTime;
import java.time.ZoneId;

public class DateUtils {
    public static Long localDTToLong(LocalDateTime localDateTime) {
       return localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
