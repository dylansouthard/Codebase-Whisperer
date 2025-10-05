package info.dylansouthard.StraysBookAPI.util.helpers;

public class FileUtils {

    public static String getFileExtension(String filename){
        return filename.contains(".") ? filename.substring(filename.lastIndexOf(".") + 1) : filename;
    }
}
