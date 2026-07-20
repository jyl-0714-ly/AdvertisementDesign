package com.advertisementdesign.back.common.exception;

public class ApiException extends RuntimeException {
    private final int code;

    public ApiException(ApiErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public ApiException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
