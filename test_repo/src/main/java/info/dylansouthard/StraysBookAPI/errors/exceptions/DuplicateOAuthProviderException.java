package info.dylansouthard.StraysBookAPI.errors.exceptions;

public class DuplicateOAuthProviderException extends RuntimeException {
    public DuplicateOAuthProviderException(String message) {
        super(message);
    }

    public DuplicateOAuthProviderException() {
        super("OAuth provider already exists");
    }
}
