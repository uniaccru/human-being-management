package com.humanbeingmanager.rest;

import com.humanbeingmanager.dto.*;
import com.humanbeingmanager.service.HumanBeingService;
import com.humanbeingmanager.service.ImportResult;
import com.humanbeingmanager.dao.ImportHistoryDao;
import com.humanbeingmanager.entity.ImportHistory;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ejb.EJB;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;
import java.util.logging.Logger;
import java.util.logging.Level;

@Path("/import")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ImportResource {

    private static final Logger LOGGER = Logger.getLogger(ImportResource.class.getName());

    @EJB
    private HumanBeingService humanBeingService;

    @EJB
    private ImportHistoryDao importHistoryDao;

    @POST
    @Path("/humanbeings")
    public Response importHumanBeings(List<HumanBeingDto> humanBeings) {
        try {
            LOGGER.log(Level.INFO, "POST /api/import/humanbeings - Importing {0} HumanBeings", 
                      humanBeings != null ? humanBeings.size() : 0);
            
            if (humanBeings == null || humanBeings.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                              .entity(ApiResponseDto.validationError("No HumanBeings data provided"))
                              .build();
            }

            ImportResult result = humanBeingService.importHumanBeings(humanBeings);

            String username = System.getProperty("user.name", "Unknown");
            String status = result.isSuccess() ? "SUCCESS" : "FAILED";
            ImportHistory history = new ImportHistory(
                status,
                username,
                result.getSuccessfullyImported(),
                result.getTotalProcessed(),
                result.getFailed(),
                result.getErrorMessage()
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
                errorMessage
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
}
