package info.dylansouthard.StraysBookAPI.service.FileStorage;

import java.io.IOException;


public interface FileStorageService {

    /**
     * Saves a file to the storage system.
     *
     * @param data the binary content of the file
     * @param filename the name to store the file as
     * @return the public or accessible URL/path to the stored file
     * @throws IOException if saving fails
     */
    String save(byte[] data, String filename) throws IOException;

    /**
     * Deletes a stored file, if supported.
     *
     * @param path the path or identifier of the file
     * @return true if deleted, false otherwise
     */
    boolean delete(String path);

    /**
     * Checks if a file exists at the given path.
     *
     * @param path the path or identifier of the file
     * @return true if the file exists, false otherwise
     */
    boolean fileExists(String path);


}
