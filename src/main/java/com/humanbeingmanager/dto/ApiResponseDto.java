package com.humanbeingmanager.dto;

import java.util.List;

public class ApiResponseDto<T> {
    private boolean success;
    private String message;
    private T data;
    private long timestamp;

    public ApiResponseDto() {
        this.timestamp = System.currentTimeMillis();
    }

    public ApiResponseDto(boolean success, String message, T data) {
        this();
        this.success = success;
        this.message = message;
        this.data = data;
    }

    // Статические методы для удобства
    public static <T> ApiResponseDto<T> success(T data) {
        return new ApiResponseDto<>(true, "Success", data);
    }

    public static <T> ApiResponseDto<T> success(String message, T data) {
        return new ApiResponseDto<>(true, message, data);
    }

    public static <T> ApiResponseDto<T> error(String message) {
        return new ApiResponseDto<>(false, message, null);
    }

    public static <T> ApiResponseDto<T> validationError(String message) {
        return new ApiResponseDto<>(false, "Validation error: " + message, null);
    }

    // Getters and setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}


