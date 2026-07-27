package com.vyay.core.dto.wrapper;

/**
 * Generic API response wrapper for all endpoints.
 * code: 1 = success, 0 = failure
 * message: human-readable text
 * errorCode: machine-readable error code (null if success)
 * data: actual payload
 */
public class ApiResponse<T> {
    private int code;
    private String message;
    private String errorCode;
    private T data;

    public ApiResponse() {}

    public ApiResponse(int code, String message, String errorCode, T data) {
        this.code = code;
        this.message = message;
        this.errorCode = errorCode;
        this.data = data;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(1, "Success", null, data);
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(1, message, null, data);
    }

    public static <T> ApiResponse<T> error(String message, String errorCode) {
        return new ApiResponse<>(0, message, errorCode, null);
    }

    public static <T> ApiResponse<T> error(String message, String errorCode, T data) {
        return new ApiResponse<>(0, message, errorCode, data);
    }

    // Getters & setters
    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}

