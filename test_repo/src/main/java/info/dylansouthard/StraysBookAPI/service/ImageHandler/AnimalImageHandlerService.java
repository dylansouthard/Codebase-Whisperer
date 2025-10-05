package info.dylansouthard.StraysBookAPI.service.ImageHandler;

import info.dylansouthard.StraysBookAPI.errors.ErrorFactory;
import info.dylansouthard.StraysBookAPI.service.FileStorage.LocalFileStorageService;
import info.dylansouthard.StraysBookAPI.service.ImageProcessing.AnimalImageProcessingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Service
public class AnimalImageHandlerService implements ImageHandlerService {

    private final AnimalImageProcessingService imageProcessingService;

    public final LocalFileStorageService fileStorageService;

    @Value("${storage.animal-profile-dir}")
    private String animalProfileDir;

    @Autowired
    public AnimalImageHandlerService(AnimalImageProcessingService imageProcessingService, LocalFileStorageService fileStorageService) {
        this.imageProcessingService = imageProcessingService;
        this.fileStorageService = fileStorageService;
    }


    @Override
    public String handleImageUpload(MultipartFile file) {
        byte[] processedImage = imageProcessingService.processImage(file);
        String filename = animalProfileDir + File.separator + UUID.randomUUID().toString() + ".jpg";
        try {
            return fileStorageService.save(processedImage, filename);
        } catch (IOException e) {
            throw ErrorFactory.cannotSaveImage();
        }
    }

    @Override
    public boolean deleteImage(String path) {
        return fileStorageService.delete(path);
    }

    @Override
    public boolean imageExists(String path) {
        return fileStorageService.fileExists(path);
    }


}
