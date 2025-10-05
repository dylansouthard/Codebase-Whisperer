package info.dylansouthard.StraysBookAPI.service.ImageProcessing;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface ImageProcessingService {

    public byte[] processImage(MultipartFile file) throws IOException;

}
