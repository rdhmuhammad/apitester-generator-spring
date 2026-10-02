package com.apitester.generator.controller;

import com.apitester.generator.dto.ApiResponse;
import com.apitester.generator.dto.CollectionContentResponse;
import com.apitester.generator.dto.CollectionMetadata;
import com.apitester.generator.dto.DocsContent;
import com.apitester.generator.dto.EnvironmentDto;
import com.apitester.generator.service.ApiTesterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/apitester/api/v1/collection")
@RequiredArgsConstructor
public class ApiTesterCollectionController {

    private final ApiTesterService apiTesterService;

    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<CollectionMetadata>>> listCollections() {
        return ResponseEntity.ok(ApiResponse.success(apiTesterService.listCollections()));
    }

    @GetMapping({"/read", "/read-selected"})
    public ResponseEntity<ApiResponse<CollectionContentResponse>> readSelectedCollection() throws IOException {
        return ResponseEntity.ok(ApiResponse.success(apiTesterService.readSelectedCollection()));
    }

    @GetMapping("/read/{id}")
    public ResponseEntity<ApiResponse<CollectionContentResponse>> readCollection(@PathVariable String id) throws IOException {
        return ResponseEntity.ok(ApiResponse.success(apiTesterService.readCollection(id)));
    }

    @PutMapping("/select/{id}")
    public ResponseEntity<ApiResponse<CollectionMetadata>> selectCollection(@PathVariable String id) {
        CollectionMetadata meta = apiTesterService.selectCollection(id);
        return ResponseEntity.ok(ApiResponse.success("Collection selected", meta));
    }

    @PutMapping({"/write", "/write-selected"})
    public ResponseEntity<ApiResponse<Void>> writeSelectedCollection(@RequestBody DocsContent content) throws IOException {
        apiTesterService.writeSelectedCollection(content);
        return ResponseEntity.ok(ApiResponse.successMessage("Collection written successfully"));
    }

    @PutMapping("/write/{id}")
    public ResponseEntity<ApiResponse<Void>> writeCollection(@PathVariable String id, @RequestBody DocsContent content) throws IOException {
        apiTesterService.writeCollection(id, content);
        return ResponseEntity.ok(ApiResponse.successMessage("Collection written successfully"));
    }

    @GetMapping("/get-active")
    public ResponseEntity<ApiResponse<CollectionMetadata>> getActiveCollection() {
        CollectionMetadata active = apiTesterService.getActiveCollection();
        if (active == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("No active collection"));
        }
        return ResponseEntity.ok(ApiResponse.success(active));
    }

    @GetMapping("/{id}/environments")
    public ResponseEntity<ApiResponse<EnvironmentDto.EnvironmentListResponse>> readEnvironments(@PathVariable String id) throws IOException {
        List<EnvironmentDto.EnvironmentEntry> entries = apiTesterService.readEnvironments(id);
        EnvironmentDto.EnvironmentListResponse response = EnvironmentDto.EnvironmentListResponse.builder()
                .environments(entries)
                .build();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}/environments")
    public ResponseEntity<ApiResponse<Void>> writeEnvironments(
            @PathVariable String id,
            @RequestBody EnvironmentDto.WriteEnvironmentsRequest request) throws IOException {
        apiTesterService.writeEnvironments(id, request.getEnvironments());
        return ResponseEntity.ok(ApiResponse.successMessage("Environments written successfully"));
    }

    @ExceptionHandler({IllegalArgumentException.class, org.springframework.http.converter.HttpMessageNotReadableException.class})
    public ResponseEntity<ApiResponse<Void>> handleBadRequestException(Exception e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage() != null ? e.getMessage() : "Invalid data."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception e) {
        log.error("Internal error in ApiTesterCollectionController: ", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.serverError(e.getMessage(), e.toString()));
    }
}
