package com.humanbeingmanager.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;

public class ImportHistoryDto {
    private Long id;
    private String status;
    private String username;
    private Integer addedCount;
    private Integer totalProcessed;
    private Integer failedCount;
    private String errorMessage;
    private String fileKey;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Date createdAt;

    public ImportHistoryDto() {}

    public ImportHistoryDto(Long id, String status, String username, Integer addedCount,
                           Integer totalProcessed, Integer failedCount, String errorMessage, String fileKey, Date createdAt) {
        this.id = id;
        this.status = status;
        this.username = username;
        this.addedCount = addedCount;
        this.totalProcessed = totalProcessed;
        this.failedCount = failedCount;
        this.errorMessage = errorMessage;
        this.fileKey = fileKey;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Integer getAddedCount() {
        return addedCount;
    }

    public void setAddedCount(Integer addedCount) {
        this.addedCount = addedCount;
    }

    public Integer getTotalProcessed() {
        return totalProcessed;
    }

    public void setTotalProcessed(Integer totalProcessed) {
        this.totalProcessed = totalProcessed;
    }

    public Integer getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(Integer failedCount) {
        this.failedCount = failedCount;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
    
    public String getFileKey() {
        return fileKey;
    }
    
    public void setFileKey(String fileKey) {
        this.fileKey = fileKey;
    }
}


