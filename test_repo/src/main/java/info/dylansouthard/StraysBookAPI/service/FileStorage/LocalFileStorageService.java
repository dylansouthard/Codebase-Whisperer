package info.dylansouthard.StraysBookAPI.service.FileStorage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

@Getter
@Setter
@Service
@ConfigurationProperties(prefix = "storage")
public class LocalFileStorageService implements FileStorageService {
    private String baseDir;
    private String baseUrl;

    private File getFileAtLocalPath (String path) {
        return new File(baseDir, path.replace(baseUrl + "/", ""));
    }

    @Override
    public String save(byte[] data, String filename) throws IOException {
        File file = new File(baseDir, filename);
        file.getParentFile().mkdirs();

        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data);
        }

        return baseUrl + "/" + filename;
    }

    @Override
    public boolean delete(String imgUrl) {
        File file = getFileAtLocalPath(imgUrl);
        return file.exists() && file.delete();
    }

    @Override
    public boolean fileExists(String imgUrl) {
        File file = getFileAtLocalPath(imgUrl);
        return file.exists();
    }
}
