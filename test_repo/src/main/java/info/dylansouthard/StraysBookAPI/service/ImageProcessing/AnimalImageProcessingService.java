package info.dylansouthard.StraysBookAPI.service.ImageProcessing;

import info.dylansouthard.StraysBookAPI.errors.ErrorFactory;
import info.dylansouthard.StraysBookAPI.util.helpers.ImageProcessingUtils;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

@Service
public class AnimalImageProcessingService implements ImageProcessingService {

    private static final int MAX_WIDTH = 500;
    private static final int MAX_HEIGHT = 500;
    private static final float JPEG_QUALITY = 0.7f;


    @Override
    public byte[] processImage(MultipartFile file) {
        ImageProcessingUtils.validateImageFile(file);
        try {
            BufferedImage bufferedImage = ImageIO.read(file.getInputStream());

            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            Thumbnails.of(bufferedImage)
                    .size(MAX_WIDTH, MAX_HEIGHT)
                    .outputFormat("jpg")
                    .outputQuality(JPEG_QUALITY)
                    .toOutputStream(baos);

            return baos.toByteArray();
        } catch (Exception e) {
            throw ErrorFactory.imageProcessingError();
        }

    };
}
