package com.humanbeingmanager.entity;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "import_history")
public class ImportHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "status", nullable = false)
    private String status; // SUCCESS, FAILED
    
    @Column(name = "username")
    private String username;
    
    @Column(name = "added_count")
    private Integer addedCount;
    
    @Column(name = "total_processed", nullable = false)
    private Integer totalProcessed;
    
    @Column(name = "failed_count")
    private Integer failedCount;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "created_at", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;
    
    public ImportHistory() {
        this.createdAt = new Date();
    }
    
    public ImportHistory(String status, String username, Integer addedCount, 
                        Integer totalProcessed, Integer failedCount, String errorMessage) {
        this();
        this.status = status;
        this.username = username;
        this.addedCount = addedCount;
        this.totalProcessed = totalProcessed;
        this.failedCount = failedCount;
        this.errorMessage = errorMessage;
    }
    
    // Getters and Setters
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
}


