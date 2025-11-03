package com.humanbeingmanager.rest;

import com.humanbeingmanager.dto.*;
import com.humanbeingmanager.entity.HumanBeing;
import com.humanbeingmanager.mapper.EntityDtoMapper;
import com.humanbeingmanager.service.HumanBeingService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;
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

    @Inject
    private EntityDtoMapper mapper;

    @GET
    @Path("/sum-minutes-waiting")
    public Response getSumOfMinutesWaiting() {
        try {
            LOGGER.info("GET /api/special-operations/sum-minutes-waiting");
            Long sum = humanBeingService.getSumOfMinutesWaiting();
            return Response.ok(new java.util.HashMap<String, Long>() {{ put("sum", sum); }}).build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error calculating sum of minutes waiting", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                          .entity(ApiResponseDto.error("Error calculating sum: " + e.getMessage()))
                          .build();
        }
    }

    @GET
    @Path("/max-toothpick")
    public Response getMaxToothpick() {
        try {
            LOGGER.info("GET /api/special-operations/max-toothpick");
            HumanBeing humanBeing = humanBeingService.getMaxToothpick();
            return Response.ok(mapper.toDto(humanBeing)).build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting max toothpick", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                          .entity(ApiResponseDto.error("Error getting max toothpick: " + e.getMessage()))
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
                              .entity(ApiResponseDto.error("Substring parameter is required"))
                              .build();
            }
            List<HumanBeing> humanBeings = humanBeingService.getSoundtrackStartsWith(substring);
            List<HumanBeingDto> humanBeingDtos = humanBeings.stream()
                    .map(mapper::toDto)
                    .collect(Collectors.toList());
            return Response.ok(humanBeingDtos).build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting soundtrack starts with", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                          .entity(ApiResponseDto.error("Error getting soundtrack starts with: " + e.getMessage()))
                          .build();
        }
    }

    @DELETE
    @Path("/delete-heroes-without-toothpicks")
    public Response deleteHeroesWithoutToothpicks() {
        try {
            LOGGER.info("DELETE /api/special-operations/delete-heroes-without-toothpicks");
            int deletedCount = humanBeingService.deleteHeroesWithoutToothpicks();
            return Response.ok(deletedCount).build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error deleting heroes without toothpicks", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                          .entity(ApiResponseDto.error("Error deleting heroes: " + e.getMessage()))
                          .build();
        }
    }

    @PUT
    @Path("/set-all-mood-sadness")
    public Response setAllMoodToSadness() {
        try {
            LOGGER.info("PUT /api/special-operations/set-all-mood-sadness");
            int updatedCount = humanBeingService.setAllMoodToSadness();
            return Response.ok(updatedCount).build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error setting all mood to sadness", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                          .entity(ApiResponseDto.error("Error setting mood: " + e.getMessage()))
                          .build();
        }
    }
}
