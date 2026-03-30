package com.unikly.jobservice.adapter.in.web;

import com.unikly.common.security.UserContext;
import com.unikly.jobservice.application.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/storage")
@RequiredArgsConstructor
@Tag(name = "Storage", description = "File uploads for jobs and proposals")
public class StorageController {

    private final StorageService storageService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a document")
    @ApiResponse(responseCode = "200", description = "Document uploaded, returns fileUrl")
    @ApiResponse(responseCode = "400", description = "Invalid file type or size")
    public ResponseEntity<Map<String, String>> uploadDocument(
            @RequestParam("type") String type,
            @RequestParam("file") MultipartFile file) {
        
        // type could be "proposals", "deliverables", "job-attachments"
        UUID userId = UserContext.getUserId();
        String folder = type + "/" + userId.toString();
        
        String fileUrl = storageService.uploadDocument(folder, file);
        return ResponseEntity.ok(Map.of("fileUrl", fileUrl));
    }
}
