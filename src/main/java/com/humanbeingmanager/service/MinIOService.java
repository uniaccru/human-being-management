package com.humanbeingmanager.service;

import io.minio.*;
import io.minio.errors.*;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.logging.Level;

@Singleton
@Startup
public class MinIOService {
    
    private static final Logger LOGGER = Logger.getLogger(MinIOService.class.getName());
    
    private MinioClient minioClient;
    private String bucketName;
    private String endpoint;
    private String accessKey;
    private String secretKey;
    
    @PostConstruct
    public void init() {
        endpoint = System.getProperty("MINIO_ENDPOINT", System.getenv("MINIO_ENDPOINT"));
        if (endpoint == null || endpoint.isEmpty()) {
            endpoint = "http://localhost:9000";
        }
        
        accessKey = System.getProperty("MINIO_ACCESS_KEY", System.getenv("MINIO_ACCESS_KEY"));
        if (accessKey == null || accessKey.isEmpty()) {
            accessKey = "minioadmin";
        }
        
        secretKey = System.getProperty("MINIO_SECRET_KEY", System.getenv("MINIO_SECRET_KEY"));
        if (secretKey == null || secretKey.isEmpty()) {
            secretKey = "minioadmin";
        }
        
        bucketName = System.getProperty("MINIO_BUCKET_NAME", System.getenv("MINIO_BUCKET_NAME"));
        if (bucketName == null || bucketName.isEmpty()) {
            bucketName = "import-files";
        }
        
        try {
            minioClient = MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(accessKey, secretKey)
                    .build();
            
            // Test connection with timeout
            try {
                // Try to check if bucket exists (this will test the connection)
                boolean found = minioClient.bucketExists(BucketExistsArgs.builder()
                        .bucket(bucketName)
                        .build());
                
                if (!found) {
                    minioClient.makeBucket(MakeBucketArgs.builder()
                            .bucket(bucketName)
                            .build());
                    LOGGER.info("Created MinIO bucket: " + bucketName);
                }
                
                LOGGER.info("MinIO service initialized successfully. Endpoint: " + endpoint + ", Bucket: " + bucketName);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "MinIO connection test failed, but service will continue. " +
                        "MinIO operations will fail until connection is established. Endpoint: " + endpoint, e);
                // Don't throw exception - allow application to start even if MinIO is unavailable
                // The service will fail gracefully when trying to use MinIO
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to create MinIO client. MinIO operations will not be available. " +
                    "Endpoint: " + endpoint, e);
            // Don't throw exception - allow application to start
            // Set minioClient to null so we can check it later
            minioClient = null;
        }
    }
    
    /**
     * Check if MinIO is available
     */
    private void checkMinIOAvailable() {
        if (minioClient == null) {
            throw new RuntimeException("MinIO client is not initialized. Check MinIO connection and configuration.");
        }
    }
    
    /**
     * Upload file to MinIO with a unique key
     * @param inputStream File input stream
     * @param contentType Content type
     * @param size File size
     * @return File key (object name) in MinIO
     */
    public String uploadFile(InputStream inputStream, String contentType, long size) 
            throws MinioException, IOException, NoSuchAlgorithmException, InvalidKeyException {
        checkMinIOAvailable();
        String fileKey = generateFileKey();
        
        try {
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(fileKey)
                    .stream(inputStream, size, -1)
                    .contentType(contentType)
                    .build()
            );
            
            LOGGER.info("File uploaded to MinIO with key: " + fileKey);
            return fileKey;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to upload file to MinIO", e);
            throw new RuntimeException("Failed to upload file to MinIO: " + e.getMessage(), e);
        }
    }
    
    /**
     * Upload file with temporary key (for two-phase commit)
     * @param inputStream File input stream
     * @param contentType Content type
     * @param size File size
     * @return Temporary file key
     */
    public String uploadFileTemporary(InputStream inputStream, String contentType, long size) 
            throws MinioException, IOException, NoSuchAlgorithmException, InvalidKeyException {
        checkMinIOAvailable();
        String tempKey = "temp/" + generateFileKey();
        return uploadFileWithKey(inputStream, contentType, size, tempKey);
    }
    
    /**
     * Upload file with specific key
     */
    private String uploadFileWithKey(InputStream inputStream, String contentType, long size, String key) 
            throws MinioException, IOException, NoSuchAlgorithmException, InvalidKeyException {
        checkMinIOAvailable();
        try {
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(key)
                    .stream(inputStream, size, -1)
                    .contentType(contentType)
                    .build()
            );
            
            LOGGER.info("File uploaded to MinIO with key: " + key);
            return key;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to upload file to MinIO", e);
            throw new RuntimeException("Failed to upload file to MinIO: " + e.getMessage(), e);
        }
    }
    
    /**
     * Commit temporary file (rename from temp/ to final location)
     */
    public String commitFile(String tempKey) throws MinioException, IOException, 
            NoSuchAlgorithmException, InvalidKeyException {
        checkMinIOAvailable();
        if (!tempKey.startsWith("temp/")) {
            return tempKey; // Already committed
        }
        
        String finalKey = tempKey.substring(5); // Remove "temp/" prefix
        
        try {
            // Copy object to new location
            minioClient.copyObject(
                CopyObjectArgs.builder()
                    .bucket(bucketName)
                    .object(finalKey)
                    .source(CopySource.builder()
                        .bucket(bucketName)
                        .object(tempKey)
                        .build())
                    .build()
            );
            
            // Delete temporary file
            deleteFile(tempKey);
            
            LOGGER.info("File committed from " + tempKey + " to " + finalKey);
            return finalKey;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to commit file in MinIO", e);
            throw new RuntimeException("Failed to commit file in MinIO: " + e.getMessage(), e);
        }
    }
    
    /**
     * Download file from MinIO
     */
    public InputStream downloadFile(String fileKey) throws MinioException, IOException, 
            NoSuchAlgorithmException, InvalidKeyException {
        checkMinIOAvailable();
        try {
            return minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(fileKey)
                    .build()
            );
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to download file from MinIO: " + fileKey, e);
            throw new RuntimeException("Failed to download file from MinIO: " + e.getMessage(), e);
        }
    }
    
    /**
     * Delete file from MinIO
     */
    public void deleteFile(String fileKey) throws MinioException, IOException, 
            NoSuchAlgorithmException, InvalidKeyException {
        if (minioClient == null) {
            LOGGER.warning("MinIO client not available, cannot delete file: " + fileKey);
            return;
        }
        try {
            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(fileKey)
                    .build()
            );
            LOGGER.info("File deleted from MinIO: " + fileKey);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to delete file from MinIO: " + fileKey, e);
            // Don't throw exception - file might not exist
        }
    }
    
    /**
     * Check if file exists
     */
    public boolean fileExists(String fileKey) {
        if (minioClient == null) {
            return false;
        }
        try {
            minioClient.statObject(
                StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(fileKey)
                    .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Generate unique file key
     */
    private String generateFileKey() {
        return UUID.randomUUID().toString() + "_" + System.currentTimeMillis() + ".json";
    }
    
    public String getBucketName() {
        return bucketName;
    }
}

