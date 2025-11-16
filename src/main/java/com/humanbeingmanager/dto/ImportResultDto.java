package com.humanbeingmanager.dto;

import java.util.List;

public class ImportResultDto {
    private boolean success;
    private String errorMessage;
    private int totalProcessed;
    private int successfullyImported;
    private int failed;
    private List<String> errors;

    public ImportResultDto() {}

    public ImportResultDto(boolean success, String errorMessage, int totalProcessed, 
                       int successfullyImported, int failed, List<String> errors) {
        this.success = success;
        this.errorMessage = errorMessage;
        this.totalProcessed = totalProcessed;
        this.successfullyImported = successfullyImported;
        this.failed = failed;
        this.errors = errors;
    }

    public static ImportResultDto success(int totalProcessed, int successfullyImported) {
        return new ImportResultDto(true, null, totalProcessed, successfullyImported, 0, null);
    }

    public static ImportResultDto failure(String errorMessage, int totalProcessed,
                                     int successfullyImported, int failed, List<String> errors) {
        return new ImportResultDto(false, errorMessage, totalProcessed, successfullyImported, failed, errors);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public int getTotalProcessed() {
        return totalProcessed;
    }

    public void setTotalProcessed(int totalProcessed) {
        this.totalProcessed = totalProcessed;
    }

    public int getSuccessfullyImported() {
        return successfullyImported;
    }

    public void setSuccessfullyImported(int successfullyImported) {
        this.successfullyImported = successfullyImported;
    }

    public int getFailed() {
        return failed;
    }

    public void setFailed(int failed) {
        this.failed = failed;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }
}

