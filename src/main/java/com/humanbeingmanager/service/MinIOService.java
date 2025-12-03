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
import okhttp3.OkHttpClient;
import java.util.concurrent.TimeUnit;

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
            // Create OkHttpClient with timeouts to prevent hanging
            OkHttpClient httpClient = new OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)  // Connection timeout: 10 seconds
                    .writeTimeout(30, TimeUnit.SECONDS)     // Write timeout: 30 seconds
                    .readTimeout(30, TimeUnit.SECONDS)      // Read timeout: 30 seconds
                    .build();
            
            minioClient = MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(accessKey, secretKey)
                    .httpClient(httpClient)
                    .build();
            
            // Test connection with timeout
            try {
                // Try to check if bucket exists (this will test the connection)
                // This operation will timeout after 10 seconds if MinIO is unreachable
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
                        "MinIO operations will fail until connection is established. Endpoint: " + endpoint + 
                        ". Error: " + e.getMessage(), e);
                // Don't throw exception - allow application to start even if MinIO is unavailable
                // The service will fail gracefully when trying to use MinIO
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to create MinIO client. MinIO operations will not be available. " +
                    "Endpoint: " + endpoint + ". Error: " + e.getMessage(), e);
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
            String errorMsg = String.format(
                "MinIO client is not initialized. Endpoint: %s, Bucket: %s. " +
                "Check if MinIO is running and accessible from WildFly server. " +
                "Verify MINIO_ENDPOINT environment variable is set correctly.",
                endpoint, bucketName
            );
            LOGGER.severe(errorMsg);
            throw new RuntimeException(errorMsg);
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
            Throwable cause = e.getCause();
            String errorMsg;
            if (cause instanceof java.net.SocketTimeoutException || e instanceof java.net.SocketTimeoutException) {
                errorMsg = String.format(
                    "Connection timeout: Cannot connect to MinIO at %s. " +
                    "WildFly server cannot reach MinIO. Test from WildFly server: curl %s/minio/health/live",
                    endpoint, endpoint
                );
            } else if (cause instanceof java.util.concurrent.TimeoutException) {
                errorMsg = String.format(
                    "Timeout while uploading file to MinIO. Endpoint: %s. " +
                    "MinIO may be unreachable or slow. Check network connectivity.",
                    endpoint
                );
            } else if (cause instanceof java.net.ConnectException || e instanceof java.net.ConnectException) {
                errorMsg = String.format(
                    "Connection refused: Cannot connect to MinIO at %s. " +
                    "Check if MinIO is running and endpoint is correct.",
                    endpoint
                );
            } else {
                errorMsg = "Failed to upload file to MinIO: " + e.getMessage();
            }
            LOGGER.log(Level.SEVERE, errorMsg, e);
            throw new RuntimeException(errorMsg, e);
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
            Throwable cause = e.getCause();
            String errorMsg;
            if (cause instanceof java.util.concurrent.TimeoutException) {
                errorMsg = String.format(
                    "Timeout while uploading file to MinIO. Endpoint: %s. " +
                    "MinIO may be unreachable or slow. Check network connectivity.",
                    endpoint
                );
            } else if (cause instanceof java.net.ConnectException) {
                errorMsg = String.format(
                    "Cannot connect to MinIO at %s. Check if MinIO is running and endpoint is correct.",
                    endpoint
                );
            } else {
                errorMsg = "Failed to upload file to MinIO: " + e.getMessage();
            }
            LOGGER.log(Level.SEVERE, errorMsg, e);
            throw new RuntimeException(errorMsg, e);
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
            Throwable cause = e.getCause();
            String errorMsg;
            if (cause instanceof java.net.SocketTimeoutException || e instanceof java.net.SocketTimeoutException) {
                errorMsg = String.format(
                    "Connection timeout: Cannot connect to MinIO at %s while committing file.",
                    endpoint
                );
            } else if (cause instanceof java.util.concurrent.TimeoutException) {
                errorMsg = String.format(
                    "Timeout while committing file in MinIO. Endpoint: %s. " +
                    "MinIO may be unreachable or slow.",
                    endpoint
                );
            } else if (cause instanceof java.net.ConnectException || e instanceof java.net.ConnectException) {
                errorMsg = String.format(
                    "Connection refused: Cannot connect to MinIO at %s while committing file.",
                    endpoint
                );
            } else {
                errorMsg = "Failed to commit file in MinIO: " + e.getMessage();
            }
            LOGGER.log(Level.SEVERE, errorMsg, e);
            throw new RuntimeException(errorMsg, e);
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
            Throwable cause = e.getCause();
            String errorMsg;
            if (cause instanceof java.net.SocketTimeoutException || e instanceof java.net.SocketTimeoutException) {
                errorMsg = String.format(
                    "Connection timeout: Cannot connect to MinIO at %s while downloading file.",
                    endpoint
                );
            } else if (cause instanceof java.util.concurrent.TimeoutException) {
                errorMsg = String.format(
                    "Timeout while downloading file from MinIO. Endpoint: %s, FileKey: %s.",
                    endpoint, fileKey
                );
            } else if (cause instanceof java.net.ConnectException || e instanceof java.net.ConnectException) {
                errorMsg = String.format(
                    "Connection refused: Cannot connect to MinIO at %s while downloading file.",
                    endpoint
                );
            } else {
                errorMsg = "Failed to download file from MinIO: " + e.getMessage();
            }
            LOGGER.log(Level.SEVERE, errorMsg, e);
            throw new RuntimeException(errorMsg, e);
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

