package com.dmitryerikin.android.blacklodge.Exceptions;

public class AudioRecordException extends Exception {
    public AudioRecordException() {
        super();
    }

    public AudioRecordException(String message) {
        super(message);
    }

    public AudioRecordException(String message, Throwable cause) {
        super(message, cause);
    }

    public AudioRecordException(Throwable cause) {
        super(cause);
    }
}
