package com.humanbeingmanager.rest;

import com.humanbeingmanager.entity.HumanBeing;
import com.humanbeingmanager.entity.Car;
import com.humanbeingmanager.service.HumanBeingService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.logging.Level;

@Path("/humanbeings")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class HumanBeingResource {

    private static final Logger LOGGER = Logger.getLogger(HumanBeingResource.class.getName());

    @Inject
    private HumanBeingService humanBeingService;

    @GET
    public Response getAllHumanBeings(@QueryParam("page") @DefaultValue("0") int page,
                                     @QueryParam("size") @DefaultValue("10") int size,
                                     @QueryParam("filterColumn") String filterColumn,
                                     @QueryParam("filterValue") String filterValue,
                                     @QueryParam("sortColumn") String sortColumn,
                                     @QueryParam("sortDirection") @DefaultValue("asc") String sortDirection) {
        try {
            LOGGER.log(Level.INFO, "GET /api/humanbeings - Retrieving HumanBeings (page: {0}, size: {1}, filter: {2}={3}, sort: {4} {5})", 
                      new Object[]{page, size, filterColumn, filterValue, sortColumn, sortDirection});
            List<HumanBeing> humanBeings = humanBeingService.getAllHumanBeings(page, size, filterColumn, filterValue, sortColumn, sortDirection);
            Long totalCount = humanBeingService.getHumanBeingCount(filterColumn, filterValue);
            return Response.ok(new PaginatedResponse(humanBeings, totalCount, page, size)).build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error retrieving HumanBeings", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                          .entity(new ErrorResponse("Error retrieving HumanBeings: " + e.getMessage()))
                          .build();
        }
    }

    @GET
    @Path("/{id}")
    public Response getHumanBeingById(@PathParam("id") Long id) {
        try {
            LOGGER.log(Level.INFO, "GET /api/humanbeings/{0} - Retrieving HumanBeing by ID", id);
            Optional<HumanBeing> humanBeing = humanBeingService.getHumanBeingById(id);
            
            if (humanBeing.isPresent()) {
                return Response.ok(humanBeing.get()).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                              .entity(new ErrorResponse("HumanBeing with ID " + id + " not found"))
                              .build();
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error retrieving HumanBeing with ID: " + id, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                          .entity(new ErrorResponse("Error retrieving HumanBeing: " + e.getMessage()))
                          .build();
        }
    }

    @POST
    public Response createHumanBeing(HumanBeing humanBeing) {
        try {
            LOGGER.log(Level.INFO, "POST /api/humanbeings - Creating new HumanBeing: {0}", 
                      humanBeing != null ? humanBeing.getName() : "null");
            
            if (humanBeing == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                              .entity(new ValidationErrorResponse("HumanBeing data is required", "VALIDATION_ERROR"))
                              .build();
            }

            HumanBeing created = humanBeingService.createHumanBeing(humanBeing);
            return Response.status(Response.Status.CREATED).entity(created).build();
            
        } catch (HumanBeingService.ValidationException e) {
            LOGGER.log(Level.WARNING, "Validation error creating HumanBeing", e);
            return Response.status(Response.Status.BAD_REQUEST)
                          .entity(new ValidationErrorResponse("Validation error: " + e.getMessage(), "VALIDATION_ERROR"))
                          .build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error creating HumanBeing", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                          .entity(new ErrorResponse("Error creating HumanBeing: " + e.getMessage()))
                          .build();
        }
    }

    @PUT
    @Path("/{id}")
    public Response updateHumanBeing(@PathParam("id") Long id, HumanBeing humanBeing) {
        try {
            LOGGER.log(Level.INFO, "PUT /api/humanbeings/{0} - Updating HumanBeing", id);
            
            if (humanBeing == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                              .entity(new ValidationErrorResponse("HumanBeing data is required", "VALIDATION_ERROR"))
                              .build();
            }

            humanBeing.setId(id);
            
            HumanBeing updated = humanBeingService.updateHumanBeing(humanBeing);
            return Response.ok(updated).build();
            
        } catch (HumanBeingService.ValidationException e) {
            LOGGER.log(Level.WARNING, "Validation error updating HumanBeing", e);
            return Response.status(Response.Status.BAD_REQUEST)
                          .entity(new ValidationErrorResponse("Validation error: " + e.getMessage(), "VALIDATION_ERROR"))
                          .build();
        } catch (HumanBeingService.EntityNotFoundException e) {
            LOGGER.log(Level.WARNING, "HumanBeing not found for update", e);
            return Response.status(Response.Status.NOT_FOUND)
                          .entity(new ErrorResponse(e.getMessage()))
                          .build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating HumanBeing with ID: " + id, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                          .entity(new ErrorResponse("Error updating HumanBeing: " + e.getMessage()))
                          .build();
        }
    }


    @DELETE
    @Path("/{id}")
    public Response deleteHumanBeing(@PathParam("id") Long id) {
        try {
            LOGGER.log(Level.INFO, "DELETE /api/humanbeings/{0} - Deleting HumanBeing", id);
            
            boolean deleted = humanBeingService.deleteHumanBeing(id);
            
            if (deleted) {
                return Response.noContent().build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                              .entity(new ErrorResponse("HumanBeing with ID " + id + " not found"))
                              .build();
            }
            
        } catch (HumanBeingService.EntityNotFoundException e) {
            LOGGER.log(Level.WARNING, "HumanBeing not found for deletion", e);
            return Response.status(Response.Status.NOT_FOUND)
                          .entity(new ErrorResponse(e.getMessage()))
                          .build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error deleting HumanBeing with ID: " + id, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                          .entity(new ErrorResponse("Error deleting HumanBeing: " + e.getMessage()))
                          .build();
        }
    }


    @GET
    @Path("/count")
    public Response getCount() {
        try {
            LOGGER.info("GET /api/humanbeings/count - Getting count");
            Long count = humanBeingService.getHumanBeingCount();
            return Response.ok(new CountResponse(count)).build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting HumanBeing count", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                          .entity(new ErrorResponse("Error getting count: " + e.getMessage()))
                          .build();
        }
    }

    @GET
    @Path("/cars")
    public Response getAllCars() {
        try {
            LOGGER.info("GET /api/humanbeings/cars - Retrieving all Cars");
            List<Car> cars = humanBeingService.getAllCars();
            return Response.ok(cars).build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error retrieving all Cars", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                          .entity(new ErrorResponse("Error retrieving Cars: " + e.getMessage()))
                          .build();
        }
    }


    @POST
    @Path("/validate")
    public Response validateHumanBeing(HumanBeing humanBeing) {
        try {
            LOGGER.log(Level.INFO, "POST /api/humanbeings/validate - Validating HumanBeing");
            
            if (humanBeing == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                              .entity(new ValidationErrorResponse("HumanBeing data is required", "VALIDATION_ERROR"))
                              .build();
            }

            try {
                humanBeingService.createHumanBeing(humanBeing);
                return Response.ok(new ValidationErrorResponse("Validation successful", "SUCCESS")).build();
            } catch (HumanBeingService.ValidationException e) {
                return Response.ok(new ValidationErrorResponse("Validation failed: " + e.getMessage(), "VALIDATION_ERROR")).build();
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error validating HumanBeing", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                          .entity(new ErrorResponse("Error validating HumanBeing: " + e.getMessage()))
                          .build();
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


    public static class ValidationErrorResponse {
        private String message;
        private String type;
        private long timestamp;

        public ValidationErrorResponse() {
            this.timestamp = System.currentTimeMillis();
        }

        public ValidationErrorResponse(String message, String type) {
            this();
            this.message = message;
            this.type = type;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
    }

    public static class CountResponse {
        private Long count;

        public CountResponse() {}

        public CountResponse(Long count) {
            this.count = count;
        }

        public Long getCount() {
            return count;
        }

        public void setCount(Long count) {
            this.count = count;
        }
    }

    public static class PaginatedResponse {
        private List<HumanBeing> content;
        private Long totalElements;
        private int totalPages;
        private int currentPage;
        private int pageSize;

        public PaginatedResponse() {}

        public PaginatedResponse(List<HumanBeing> content, Long totalElements, int currentPage, int pageSize) {
            this.content = content;
            this.totalElements = totalElements;
            this.currentPage = currentPage;
            this.pageSize = pageSize;
            this.totalPages = (int) Math.ceil((double) totalElements / pageSize);
        }

        public List<HumanBeing> getContent() {
            return content;
        }

        public void setContent(List<HumanBeing> content) {
            this.content = content;
        }

        public Long getTotalElements() {
            return totalElements;
        }

        public void setTotalElements(Long totalElements) {
            this.totalElements = totalElements;
        }

        public int getTotalPages() {
            return totalPages;
        }

        public void setTotalPages(int totalPages) {
            this.totalPages = totalPages;
        }

        public int getCurrentPage() {
            return currentPage;
        }

        public void setCurrentPage(int currentPage) {
            this.currentPage = currentPage;
        }

        public int getPageSize() {
            return pageSize;
        }

        public void setPageSize(int pageSize) {
            this.pageSize = pageSize;
        }
    }
}
