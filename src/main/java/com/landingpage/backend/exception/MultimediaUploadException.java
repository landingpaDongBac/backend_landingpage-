package com.landingpage.backend.exception;

public class MultimediaUploadException extends RuntimeException {
    public MultimediaUploadException(String message) {
        super(message);
    }

    public MultimediaUploadException(String message, Throwable cause) {
        super(message, cause);
    }
}
