package info.dylansouthard.StraysBookAPI.util.helpers;

import info.dylansouthard.StraysBookAPI.errors.ErrorFactory;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public class ImageProcessingUtils {

    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "image/jpeg", "image/png", "image/webp"
    );

    public static void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ErrorFactory.missingImage();
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw ErrorFactory.invalidImageFormat();
        }
    }

}