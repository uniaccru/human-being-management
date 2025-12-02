package com.humanbeingmanager.rest;

import com.humanbeingmanager.dto.*;
import com.humanbeingmanager.entity.HumanBeing;
import com.humanbeingmanager.entity.Car;
import com.humanbeingmanager.mapper.EntityDtoMapper;
import com.humanbeingmanager.service.HumanBeingService;
import com.humanbeingmanager.exception.ValidationException;
import com.humanbeingmanager.exception.EntityNotFoundException;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ejb.EJB;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.logging.Logger;
import java.util.logging.Level;

@Path("/humanbeings")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class HumanBeingResource {

    private static final Logger LOGGER = Logger.getLogger(HumanBeingResource.class.getName());

    @EJB
    private HumanBeingService humanBeingService;

    @Inject
    private EntityDtoMapper mapper;

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
            List<HumanBeingDto> humanBeingDtos = humanBeings.stream()
                    .map(mapper::toDto)
                    .collect(Collectors.toList());
            return Response.ok(new PaginatedResponseDto<>(humanBeingDtos, totalCount, page, size)).build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error retrieving HumanBeings", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                          .entity(ApiResponseDto.error("Error retrieving HumanBeings: " + e.getMessage()))
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
                return Response.ok(mapper.toDto(humanBeing.get())).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                              .entity(ApiResponseDto.error("HumanBeing with ID " + id + " not found"))
                              .build();
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error retrieving HumanBeing with ID: " + id, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                          .entity(ApiResponseDto.error("Error retrieving HumanBeing: " + e.getMessage()))
                          .build();
        }
    }

    @POST
    public Response createHumanBeing(HumanBeingDto humanBeingDto) {
        try {
            LOGGER.log(Level.INFO, "POST /api/humanbeings - Creating new HumanBeing: {0}", 
                      humanBeingDto != null ? humanBeingDto.getName() : "null");
            
            if (humanBeingDto == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                              .entity(ApiResponseDto.validationError("HumanBeing data is required"))
                              .build();
            }

            HumanBeing humanBeing = mapper.toEntity(humanBeingDto);
            HumanBeing created = humanBeingService.createHumanBeing(humanBeing);
            return Response.status(Response.Status.CREATED).entity(mapper.toDto(created)).build();
            
        } catch (ValidationException e) {
            LOGGER.log(Level.WARNING, "Validation error creating HumanBeing", e);
            return Response.status(Response.Status.BAD_REQUEST)
                          .entity(ApiResponseDto.validationError(e.getMessage()))
                          .build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error creating HumanBeing", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                          .entity(ApiResponseDto.error("Error creating HumanBeing: " + e.getMessage()))
                          .build();
        }
    }

    @PUT
    @Path("/{id}")
    public Response updateHumanBeing(@PathParam("id") Long id, HumanBeingDto humanBeingDto) {
        try {
            LOGGER.log(Level.INFO, "PUT /api/humanbeings/{0} - Updating HumanBeing", id);
            
            if (humanBeingDto == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                              .entity(ApiResponseDto.validationError("HumanBeing data is required"))
                              .build();
            }

            humanBeingDto.setId(id);
            HumanBeing humanBeing = mapper.toEntity(humanBeingDto);
            HumanBeing updated = humanBeingService.updateHumanBeing(humanBeing);
            return Response.ok(mapper.toDto(updated)).build();
            
        } catch (ValidationException e) {
            LOGGER.log(Level.WARNING, "Validation error updating HumanBeing", e);
            return Response.status(Response.Status.BAD_REQUEST)
                          .entity(ApiResponseDto.validationError(e.getMessage()))
                          .build();
        } catch (EntityNotFoundException e) {
            LOGGER.log(Level.WARNING, "HumanBeing not found for update", e);
            return Response.status(Response.Status.NOT_FOUND)
                          .entity(ApiResponseDto.error(e.getMessage()))
                          .build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating HumanBeing with ID: " + id, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                          .entity(ApiResponseDto.error("Error updating HumanBeing: " + e.getMessage()))
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
                              .entity(ApiResponseDto.error("HumanBeing with ID " + id + " not found"))
                              .build();
            }
            
        } catch (EntityNotFoundException e) {
            LOGGER.log(Level.WARNING, "HumanBeing not found for deletion", e);
            return Response.status(Response.Status.NOT_FOUND)
                          .entity(ApiResponseDto.error(e.getMessage()))
                          .build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error deleting HumanBeing with ID: " + id, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                          .entity(ApiResponseDto.error("Error deleting HumanBeing: " + e.getMessage()))
                          .build();
        }
    }


    @GET
    @Path("/count")
    public Response getCount() {
        try {
            LOGGER.info("GET /api/humanbeings/count - Getting count");
            Long count = humanBeingService.getHumanBeingCount();
            return Response.ok(ApiResponseDto.success(count)).build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting HumanBeing count", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                          .entity(ApiResponseDto.error("Error getting count: " + e.getMessage()))
                          .build();
        }
    }

    @GET
    @Path("/cars")
    public Response getAllCars() {
        try {
            LOGGER.info("GET /api/humanbeings/cars - Retrieving all Cars");
            List<Car> cars = humanBeingService.getAllCars();
            List<CarDto> carDtos = cars.stream()
                    .map(mapper::toDto)
                    .collect(Collectors.toList());
            return Response.ok(carDtos).build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error retrieving all Cars", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                          .entity(ApiResponseDto.error("Error retrieving Cars: " + e.getMessage()))
                          .build();
        }
    }


    @POST
    @Path("/validate")
    public Response validateHumanBeing(HumanBeingDto humanBeingDto) {
        try {
            LOGGER.log(Level.INFO, "POST /api/humanbeings/validate - Validating HumanBeing");
            
            if (humanBeingDto == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                              .entity(ApiResponseDto.validationError("HumanBeing data is required"))
                              .build();
            }

            try {
                HumanBeing humanBeing = mapper.toEntity(humanBeingDto);
                humanBeingService.createHumanBeing(humanBeing);
                return Response.ok(ApiResponseDto.success("Validation successful")).build();
            } catch (ValidationException e) {
                return Response.ok(ApiResponseDto.validationError(e.getMessage())).build();
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error validating HumanBeing", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                          .entity(ApiResponseDto.error("Error validating HumanBeing: " + e.getMessage()))
                          .build();
        }
    }
}
