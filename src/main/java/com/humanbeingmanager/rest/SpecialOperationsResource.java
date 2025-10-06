package com.humanbeingmanager.rest;

import com.humanbeingmanager.entity.HumanBeing;
import com.humanbeingmanager.service.HumanBeingService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;


@Path("/special-operations")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SpecialOperationsResource {

    private static final Logger LOGGER = Logger.getLogger(SpecialOperationsResource.class.getName());

    @Inject
    private HumanBeingService humanBeingService;

    @GET
    @Path("/sum-minutes-waiting")
    public Response getSumOfMinutesWaiting() {
        try {
            LOGGER.info("GET /api/special-operations/sum-minutes-waiting");
            Long sum = humanBeingService.getSumOfMinutesWaiting();
            return Response.ok(new SumResponse(sum)).build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error calculating sum of minutes waiting", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                          .entity(new ErrorResponse("Error calculating sum: " + e.getMessage()))
                          .build();
        }
    }

    @GET
    @Path("/max-toothpick")
    public Response getMaxToothpick() {
        try {
            LOGGER.info("GET /api/special-operations/max-toothpick");
            HumanBeing humanBeing = humanBeingService.getMaxToothpick();
            return Response.ok(humanBeing).build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting max toothpick", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                          .entity(new ErrorResponse("Error getting max toothpick: " + e.getMessage()))
                          .build();
        }
    }

    @GET
    @Path("/soundtrack-starts-with")
    public Response getSoundtrackStartsWith(@QueryParam("substring") String substring) {
        try {
            LOGGER.log(Level.INFO, "GET /api/special-operations/soundtrack-starts-with?substring={0}", substring);
            if (substring == null || substring.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                              .entity(new ErrorResponse("Substring parameter is required"))
                              .build();
            }
            List<HumanBeing> humanBeings = humanBeingService.getSoundtrackStartsWith(substring);
            return Response.ok(humanBeings).build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting soundtrack starts with", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                          .entity(new ErrorResponse("Error getting soundtrack starts with: " + e.getMessage()))
                          .build();
        }
    }

    @DELETE
    @Path("/delete-heroes-without-toothpicks")
    public Response deleteHeroesWithoutToothpicks() {
        try {
            LOGGER.info("DELETE /api/special-operations/delete-heroes-without-toothpicks");
            int deletedCount = humanBeingService.deleteHeroesWithoutToothpicks();
            return Response.ok(new DeleteResponse(deletedCount)).build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error deleting heroes without toothpicks", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                          .entity(new ErrorResponse("Error deleting heroes: " + e.getMessage()))
                          .build();
        }
    }

    @PUT
    @Path("/set-all-mood-sadness")
    public Response setAllMoodToSadness() {
        try {
            LOGGER.info("PUT /api/special-operations/set-all-mood-sadness");
            int updatedCount = humanBeingService.setAllMoodToSadness();
            return Response.ok(new UpdateResponse(updatedCount)).build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error setting all mood to sadness", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                          .entity(new ErrorResponse("Error setting mood: " + e.getMessage()))
                          .build();
        }
    }

    public static class SumResponse {
        private Long sum;

        public SumResponse() {}

        public SumResponse(Long sum) {
            this.sum = sum;
        }

        public Long getSum() {
            return sum;
        }

        public void setSum(Long sum) {
            this.sum = sum;
        }
    }


    public static class DeleteResponse {
        private int deletedCount;

        public DeleteResponse() {}

        public DeleteResponse(int deletedCount) {
            this.deletedCount = deletedCount;
        }

        public int getDeletedCount() {
            return deletedCount;
        }

        public void setDeletedCount(int deletedCount) {
            this.deletedCount = deletedCount;
        }
    }


    public static class UpdateResponse {
        private int updatedCount;

        public UpdateResponse() {}

        public UpdateResponse(int updatedCount) {
            this.updatedCount = updatedCount;
        }

        public int getUpdatedCount() {
            return updatedCount;
        }

        public void setUpdatedCount(int updatedCount) {
            this.updatedCount = updatedCount;
        }
    }

    public static class ErrorResponse {
        private String message;
        private long timestamp;

        public ErrorResponse() {
            this.timestamp = System.currentTimeMillis();
        }

        public ErrorResponse(String message) {
            this();
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
    }
}
