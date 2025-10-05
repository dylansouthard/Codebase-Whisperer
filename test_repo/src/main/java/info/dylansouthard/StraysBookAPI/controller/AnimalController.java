package info.dylansouthard.StraysBookAPI.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import info.dylansouthard.StraysBookAPI.constants.ApiRoutes;
import info.dylansouthard.StraysBookAPI.dto.careEvent.CareEventDTO;
import info.dylansouthard.StraysBookAPI.dto.careEvent.CreateSimpleCareEventDTO;
import info.dylansouthard.StraysBookAPI.dto.common.PaginatedResponseDTO;
import info.dylansouthard.StraysBookAPI.dto.friendo.*;
import info.dylansouthard.StraysBookAPI.errors.ErrorCodes;
import info.dylansouthard.StraysBookAPI.errors.ErrorFactory;
import info.dylansouthard.StraysBookAPI.model.user.User;
import info.dylansouthard.StraysBookAPI.service.AnimalService;
import info.dylansouthard.StraysBookAPI.service.CareEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
public class AnimalController {
    @Autowired
    private AnimalService animalService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private CareEventService careEventService;

    @PostMapping(path = ApiRoutes.ANIMALS.CREATE, consumes = "multipart/form-data")
    @Operation(summary = "Register a new animal", description = "Creates and registers a new animal. Requires authentication.")
    @ApiResponse(responseCode = "201", description = "Animal created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request. Error Code: " + ErrorCodes.INVALID_CREATE + ", " + ErrorCodes.INVALID_IMAGE_FORMAT + ", " + ErrorCodes.MISSING_IMAGE)
    @ApiResponse(responseCode = "401", description = "Unauthorized. Error Code: " + ErrorCodes.AUTH)
    @ApiResponse(responseCode = "500", description = "Server error. Error Codes: " + ErrorCodes.INTERNAL_SERVER_ERROR + ", " + ErrorCodes.IMAGE_PROCESSING_ERROR)
    public ResponseEntity<AnimalDTO> createAnimal(
            @RequestBody(
                    description = "Animal data as JSON",
                    required = true,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CreateAnimalDTO.class))
            )
            @RequestPart("body") String body,
            @RequestPart("image") org.springframework.web.multipart.MultipartFile image,
            @AuthenticationPrincipal User user
    ) throws JsonProcessingException {
        if (user == null) throw ErrorFactory.auth();
        CreateAnimalDTO dto = objectMapper.readValue(body, CreateAnimalDTO.class);
        AnimalDTO created = animalService.createAnimal(dto, image, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }


    @GetMapping(ApiRoutes.ANIMALS.NEARBY)
    @Operation(summary = "Find animals near a location", description = "Retrieve a list of animals near the provided latitude and longitude.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Animals found and returned"),
            @ApiResponse(responseCode = "400", description = "Invalid coordinates. Error Code: INVALID_COORDINATES"),
            @ApiResponse(responseCode = "500", description = "Server error. Error Code: INTERNAL_SERVER_ERROR")
    })
    public ResponseEntity<List<AnimalSummaryDTO>> getNearbyAnimals(
            @RequestParam(required = false)  // Not required in code
            @Parameter(description = "Latitude", example = "13.7563", required = true) Double lat,

            @RequestParam(required = false)  // Not required in code
            @Parameter(description = "Longitude", example = "100.5018", required = true) Double lon,

            @RequestParam(defaultValue = "1000")
            @Parameter(description = "Radius in meters", example = "1000") Double radius,

            @AuthenticationPrincipal User user
    ) {
        List<AnimalSummaryDTO> animals = animalService.getAnimalsInArea(lat, lon, radius, user != null ? user.getId() : null);
        return ResponseEntity.ok(animals);
    }

    @GetMapping(ApiRoutes.ANIMALS.DETAIL)
    @Operation(summary = "Fetch detailed animal information", description = "Retrieve full animal details by ID.")
    @ApiResponse(responseCode = "200", description = "Animal found and returned")
    @ApiResponse(responseCode = "400", description = "Invalid parameters. Error Code: INVALID_PARAMS")
    @ApiResponse(responseCode = "404", description = "Animal not found. Error Code: ANIMAL_NOT_FOUND")
    @ApiResponse(responseCode = "500", description = "Server error. Error Code: INTERNAL_SERVER_ERROR")
    public ResponseEntity<AnimalDTO> getAnimalById(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal User user
    ) {
        AnimalDTO animal = animalService.fetchAnimalDetails(id, user != null ? user.getId() : null);
        return ResponseEntity.ok(animal);
    }

    @PatchMapping(ApiRoutes.ANIMALS.DETAIL)
    @Operation(summary = "Update an animal", description = "Updates allowed fields of an animal. Requires authentication.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Animal updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid update request. Error Code: INVALID_PARAMS or INVALID_UPDATE"),
            @ApiResponse(responseCode = "401", description = "Unauthorized. Error Code: AUTH"),
            @ApiResponse(responseCode = "403", description = "Forbidden. Error Code: AUTH_FORBIDDEN"),
            @ApiResponse(responseCode = "404", description = "Animal not found. Error Code: ANIMAL_NOT_FOUND"),
            @ApiResponse(responseCode = "500", description = "Server error. Error Code: INTERNAL_SERVER_ERROR")
    })
    public ResponseEntity<AnimalDTO> updateAnimal(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal User user,
            @org.springframework.web.bind.annotation.RequestBody UpdateAnimalDTO updateDTO
    ) {
        if (user == null) throw ErrorFactory.auth();
        AnimalDTO updated = animalService.updateAnimal(id, updateDTO, user);
        return ResponseEntity.ok(updated);
    }

    @PostMapping(ApiRoutes.ANIMALS.CARE_EVENTS)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Care Event added successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid update request. Error Code: INVALID_PARAMS or INVALID_UPDATE"),
            @ApiResponse(responseCode = "401", description = "Unauthorized. Error Code: AUTH"),
            @ApiResponse(responseCode = "403", description = "Forbidden. Error Code: AUTH_FORBIDDEN"),
            @ApiResponse(responseCode = "404", description = "Animal not found. Error Code: ANIMAL_NOT_FOUND"),
            @ApiResponse(responseCode = "500", description = "Server error. Error Code: INTERNAL_SERVER_ERROR")
    })
    public ResponseEntity<CareEventDTO> createSimpleCareEvent(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal User user,
            @org.springframework.web.bind.annotation.RequestBody CreateSimpleCareEventDTO createDTO
            )
    {
        if (user == null) throw ErrorFactory.auth();
        CareEventDTO careEvent = careEventService.createSimpleCareEvent(createDTO, id, user.getId());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(careEvent);
    }

    @GetMapping(ApiRoutes.ANIMALS.CARE_EVENTS)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Care events returned successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid update request. Error Code: INVALID_PARAMS"),
            @ApiResponse(responseCode = "404", description = "Animal not found. Error Code: ANIMAL_NOT_FOUND"),
            @ApiResponse(responseCode = "500", description = "Server error. Error Code: INTERNAL_SERVER_ERROR")
    })
    public ResponseEntity<PaginatedResponseDTO<CareEventDTO>> getCareEvents(
            @PathVariable("id") Long id,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "page_size", required = false) Integer pageSize

    )
    {
        PaginatedResponseDTO<CareEventDTO> careEvents = careEventService.getCareEventsForAnimal(id, page, pageSize);
        return ResponseEntity.ok(careEvents);
    }

    @PatchMapping(ApiRoutes.ANIMALS.IMAGE)
    @Operation(summary = "Update an animal's image", description = "Updates animal profile image. Requires primary caretaker authentication.")
    @ApiResponse(responseCode = "200", description = "Image updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request. Error Code: " + ErrorCodes.INVALID_CREATE + ", " + ErrorCodes.INVALID_IMAGE_FORMAT + ", " + ErrorCodes.MISSING_IMAGE)
    @ApiResponse(responseCode = "401", description = "Unauthorized. Error Code: " + ErrorCodes.AUTH)
    @ApiResponse(responseCode = "403", description = "Forbidden. Error Code: " + ErrorCodes.AUTH_FORBIDDEN)
    @ApiResponse(responseCode = "500", description = "Server error. Error Codes: " + ErrorCodes.INTERNAL_SERVER_ERROR + ", " + ErrorCodes.IMAGE_PROCESSING_ERROR)
    public ResponseEntity<ImageUpdateResponseDTO> updateAnimalImage(
            @PathVariable Long id,
            @RequestPart("image") MultipartFile image,
            @AuthenticationPrincipal User user
    ) {
        if (user == null) throw ErrorFactory.auth();
        ImageUpdateResponseDTO updated = animalService.updateAnimalImage(id, image, user);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping(ApiRoutes.ANIMALS.DETAIL)
    @Operation(summary = "Delete an animal", description = "Deletes an animal. Only allowed for the primary caretaker.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Animal deleted successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden. Error Code: AUTH_FORBIDDEN"),
            @ApiResponse(responseCode = "404", description = "Animal not found. Error Code: ANIMAL_NOT_FOUND"),
            @ApiResponse(responseCode = "500", description = "Server error. Error Code: INTERNAL_SERVER_ERROR")
    })
    public ResponseEntity<Void> deleteAnimal(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal User user
    ) {
        if (user == null) throw ErrorFactory.auth();
        animalService.deleteAnimal(id, user);
        return ResponseEntity.noContent().build();
    }


}
