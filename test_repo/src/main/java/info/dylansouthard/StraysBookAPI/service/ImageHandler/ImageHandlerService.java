package info.dylansouthard.StraysBookAPI.service.ImageHandler;

import org.springframework.web.multipart.MultipartFile;

public interface ImageHandlerService {
    String handleImageUpload(MultipartFile file);

    boolean deleteImage(String imgUrl);

    boolean imageExists(String imgUrl);
}
