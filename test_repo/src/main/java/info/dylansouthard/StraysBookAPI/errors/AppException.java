package info.dylansouthard.StraysBookAPI.errors;

import lombok.Getter;

@Getter
public class AppException extends RuntimeException {
    private final String errorCode;
    private final String errorMessage;
    private final int status;

    public AppException(String errorCode, String errorMessage, int status) {
        super(errorMessage);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.status = status;
    }
}
