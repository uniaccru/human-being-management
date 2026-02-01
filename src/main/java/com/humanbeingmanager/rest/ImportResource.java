package com.humanbeingmanager.rest;

import com.humanbeingmanager.dto.*;
import com.humanbeingmanager.service.ImportService;
import com.humanbeingmanager.service.DistributedTransactionManager;
import com.humanbeingmanager.service.MinIOService;
import com.humanbeingmanager.dao.ImportHistoryDao;
import com.humanbeingmanager.entity.ImportHistory;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ejb.EJB;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;
import java.util.logging.Logger;
import java.util.logging.Level;

@Path("/import")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class ImportResource {

    private static final Logger LOGGER = Logger.getLogger(ImportResource.class.getName());

    @EJB
    private ImportService importService;

    @EJB
    private ImportHistoryDao importHistoryDao;
    
    @EJB
    private DistributedTransactionManager transactionManager;
    
    @EJB
    private MinIOService minIOService;
    
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @POST
    @Path("/humanbeings")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response importHumanBeings(List<HumanBeingDto> humanBeings) {
        try {
            LOGGER.log(Level.INFO, "POST /api/import/humanbeings - Importing {0} HumanBeings", 
                      humanBeings != null ? humanBeings.size() : 0);
            
            if (humanBeings == null || humanBeings.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                              .entity(ApiResponseDto.validationError("No HumanBeings data provided"))
                              .build();
            }

            // Backward compatibility when file storage is not used
            String transactionId = null;
            ImportResultDto result = importService.importHumanBeings(humanBeings, transactionId);

            String username = System.getProperty("user.name", "Unknown");
            String status = result.isSuccess() ? "SUCCESS" : "FAILED";
            ImportHistory history = new ImportHistory(
                status,
                username,
                result.getSuccessfullyImported(),
                result.getTotalProcessed(),
                result.getFailed(),
                result.getErrorMessage(),
                null 
            );
            importHistoryDao.create(history);
            
            if (result.isSuccess()) {
                return Response.ok(ApiResponseDto.success("Import completed successfully", result)).build();
            } else {
                return Response.status(Response.Status.BAD_REQUEST)
                              .entity(ApiResponseDto.error("Import failed: " + result.getErrorMessage()))
                              .build();
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error importing HumanBeings", e);

            String errorMessage = "Import failed: " + e.getMessage();

            String username = System.getProperty("user.name", "Unknown");
            ImportHistory history = new ImportHistory(
                "FAILED",
                username,
                0,
                humanBeings != null ? humanBeings.size() : 0,
                humanBeings != null ? humanBeings.size() : 0,
                errorMessage,
                null
            );
            try {
                importHistoryDao.create(history);
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Failed to save import history", ex);
            }
            
            return Response.status(Response.Status.BAD_REQUEST)
                          .entity(ApiResponseDto.error(errorMessage))
                          .build();
        }
    }

    @GET
    @Path("/history")
    public Response getImportHistory() {
        try {
            LOGGER.log(Level.INFO, "GET /api/import/history - Retrieving import history");
            
            List<ImportHistory> historyList = importHistoryDao.findAll();
            
            List<ImportHistoryDto> historyDtos = historyList.stream()
                .map(history -> new ImportHistoryDto(
                    history.getId(),
                    history.getStatus(),
                    history.getUsername(),
                    history.getAddedCount(),
                    history.getTotalProcessed(),
                    history.getFailedCount(),
                    history.getErrorMessage(),
                    history.getFileKey(),
                    history.getCreatedAt()
                ))
                .collect(Collectors.toList());
            
            return Response.ok(historyDtos).build();
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error retrieving import history", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                          .entity(ApiResponseDto.error("Error retrieving import history: " + e.getMessage()))
                          .build();
        }
    }
    
    @POST
    @Path("/humanbeings/file")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response importHumanBeingsFromFile(MultipartFormDataInput input) { 
        
        LOGGER.info("========== START FILE IMPORT ==========");
        LOGGER.info("Timestamp: " + new java.util.Date());
        LOGGER.info("MinIO Endpoint: " + System.getenv("MINIO_ENDPOINT"));
        LOGGER.info("MinIO Bucket: " + System.getenv("MINIO_BUCKET_NAME"));
        
        String transactionId = null;
        String fileKey = null;
        
        try {
            LOGGER.info("Parsing multipart form data");
            Map<String, List<InputPart>> formData = input.getFormDataMap();
            LOGGER.info("Form data keys: " + (formData != null ? formData.keySet() : "null"));
            
            if (formData == null || !formData.containsKey("file")) {
                return Response.status(Response.Status.BAD_REQUEST)
                              .entity(ApiResponseDto.validationError("No file provided"))
                              .build();
            }
            
            List<InputPart> inputParts = formData.get("file");
            if (inputParts == null || inputParts.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                              .entity(ApiResponseDto.validationError("No file provided"))
                              .build();
            }
            
            InputPart inputPart = inputParts.get(0);
            LOGGER.info("Getting file input stream");
            InputStream fileInputStream = inputPart.getBody(InputStream.class, null);
            
            String fileName = getFileName(inputPart);
            String contentType = inputPart.getMediaType().toString();
            LOGGER.info("File name: " + fileName);
            LOGGER.info("Content type: " + contentType);
            if (contentType == null || contentType.isEmpty()) {
                contentType = "application/json";
            }
            
            // Read file content into buffer
            java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
            byte[] data = new byte[8192];
            int nRead;
            while ((nRead = fileInputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            byte[] fileBytes = buffer.toByteArray();
            long fileSize = fileBytes.length;
            
            LOGGER.log(Level.INFO, "POST /api/import/humanbeings/file - Uploading file: {0}, size: {1}", 
                      new Object[]{fileName, fileSize});
            
            // Validate JSON before upload
            List<HumanBeingDto> humanBeings;
            try {
                String fileContent = new String(fileBytes);
                humanBeings = objectMapper.readValue(fileContent, 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, HumanBeingDto.class));
            } catch (Exception e) {
                return Response.status(Response.Status.BAD_REQUEST)
                              .entity(ApiResponseDto.error("Invalid JSON format: " + e.getMessage()))
                              .build();
            }
            
            if (humanBeings == null || humanBeings.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                              .entity(ApiResponseDto.validationError("No HumanBeings data in file"))
                              .build();
            }
            
            // Phase 1: Prepare MinIO (upload with temp key)
            try {
                java.io.ByteArrayInputStream fileStream = new java.io.ByteArrayInputStream(fileBytes);
                transactionId = transactionManager.prepareMinIO(fileStream, contentType, fileSize);
                LOGGER.info("Phase 1 (Prepare) - MinIO: Transaction ID: " + transactionId);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Phase 1 (Prepare) - MinIO failed", e);
                transactionManager.handleMinIOFailure(transactionId != null ? transactionId : "unknown");
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                              .entity(ApiResponseDto.error("Failed to upload file to storage: " + e.getMessage()))
                              .build();
            }
            
            // Phase 1: Prepare Database (check readiness)
            LOGGER.info("Phase 1 (Prepare) - Database: Checking readiness for transaction: " + transactionId);
            boolean dbPrepared = false;
            try {
                dbPrepared = transactionManager.prepareDatabase(transactionId);
                if (!dbPrepared) {
                    LOGGER.severe("Phase 1 (Prepare) - Database: NOT READY for transaction: " + transactionId);
                    transactionManager.handleDatabaseFailure(transactionId);
                    return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                                  .entity(ApiResponseDto.error("Database is not available"))
                                  .build();
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Phase 1 (Prepare) - Database readiness check failed", e);
                transactionManager.handleDatabaseFailure(transactionId);
                return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                              .entity(ApiResponseDto.error("Database readiness check failed: " + e.getMessage()))
                              .build();
            }
            
            LOGGER.info("Phase 1 (Prepare) - Database: Executing import for transaction: " + transactionId);
            LOGGER.info("Number of HumanBeings to import: " + humanBeings.size());
            ImportResultDto result;
            try {
                LOGGER.info("Calling transactionManager.executeImportInTransaction");
                result = transactionManager.executeImportInTransaction(transactionId, humanBeings);
                LOGGER.info("Import completed. Success: " + result.isSuccess());
                
                if (!result.isSuccess()) {
                    LOGGER.warning("Database import failed, rolling back transaction: " + transactionId);
                    transactionManager.handleDatabaseFailure(transactionId);
                    return Response.status(Response.Status.BAD_REQUEST)
                                  .entity(ApiResponseDto.error("Import failed: " + result.getErrorMessage()))
                                  .build();
                }
                
                LOGGER.info("Phase 1 (Prepare) - Database: Import completed successfully, transaction prepared (NOT committed), ready for Phase 2");
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Phase 1 (Prepare) - Database import failed", e);
                transactionManager.handleDatabaseFailure(transactionId);
                return Response.status(Response.Status.BAD_REQUEST)
                              .entity(ApiResponseDto.error("Import failed: " + e.getMessage()))
                              .build();
            }
            
            // Phase 2: Commit (MinIO and database)
            try {
                fileKey = transactionManager.commit(transactionId);
                LOGGER.info("Phase 2 (Commit) - Transaction committed: " + transactionId + ", File key: " + fileKey);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Phase 2 (Commit) - Failed", e);
                // Transaction manager will handle rollback
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                              .entity(ApiResponseDto.error("Failed to commit transaction: " + e.getMessage()))
                              .build();
            }
            
            // Save import history
            String username = System.getProperty("user.name", "Unknown");
            String status = result.isSuccess() ? "SUCCESS" : "FAILED";
            ImportHistory history = new ImportHistory(
                status,
                username,
                result.getSuccessfullyImported(),
                result.getTotalProcessed(),
                result.getFailed(),
                result.getErrorMessage(),
                fileKey
            );
            importHistoryDao.create(history);
            
            if (result.isSuccess()) {
                return Response.ok(ApiResponseDto.success("Import completed successfully", result)).build();
            } else {
                return Response.status(Response.Status.BAD_REQUEST)
                              .entity(ApiResponseDto.error("Import failed: " + result.getErrorMessage()))
                              .build();
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error importing HumanBeings from file", e);
            
            // Rollback transaction if it was started
            if (transactionId != null) {
                try {
                    transactionManager.rollback(transactionId);
                } catch (Exception rollbackEx) {
                    LOGGER.log(Level.WARNING, "Failed to rollback transaction", rollbackEx);
                }
            }
            
            String errorMessage = "Import failed: " + e.getMessage();
            String username = System.getProperty("user.name", "Unknown");
            ImportHistory history = new ImportHistory(
                "FAILED",
                username,
                0,
                0,
                0,
                errorMessage,
                null
            );
            try {
                importHistoryDao.create(history);
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Failed to save import history", ex);
            }
            
            return Response.status(Response.Status.BAD_REQUEST)
                          .entity(ApiResponseDto.error(errorMessage))
                          .build();
        }
    }
    
    @GET
    @Path("/file/{id}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadImportFile(@PathParam("id") Long importId) {
        try {
            LOGGER.log(Level.INFO, "GET /api/import/file/{0} - Downloading file", importId);
            
            ImportHistory history = importHistoryDao.findById(importId)
                .orElseThrow(() -> new jakarta.ws.rs.NotFoundException("Import history not found: " + importId));
            
            if (history.getFileKey() == null || history.getFileKey().isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND)
                              .entity("No file associated with this import")
                              .build();
            }
            
            InputStream fileStream = minIOService.downloadFile(history.getFileKey());
            
            return Response.ok(fileStream)
                          .header("Content-Disposition", "attachment; filename=\"import_" + importId + ".json\"")
                          .header("Content-Type", "application/json")
                          .build();
            
        } catch (jakarta.ws.rs.NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                          .entity(e.getMessage())
                          .build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error downloading import file", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                          .entity("Error downloading file: " + e.getMessage())
                          .build();
        }
    }
    
    private String getFileName(InputPart inputPart) {
        String header = inputPart.getHeaders().getFirst("Content-Disposition");
        if (header == null || header.isEmpty()) {
            return "unknown";
        }
        String[] contentDisposition = header.split(";");
        for (String filename : contentDisposition) {
            if (filename.trim().startsWith("filename")) {
                String[] name = filename.split("=");
                return name.length > 1 ? name[1].trim().replaceAll("\"", "") : "unknown";
            }
        }
        return "unknown";
    }
}
