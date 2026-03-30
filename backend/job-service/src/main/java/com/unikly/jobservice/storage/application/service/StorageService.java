package com.unikly.jobservice.storage.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {

    private static final Set<String> ALLOWED_DOC_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "image/jpeg",
            "image/png",
            "image/webp"
    );
    private static final long MAX_DOC_SIZE = 20 * 1024 * 1024L; // 20 MB

    private final S3Client s3Client;

    @Value("${minio.bucket}")
    private String bucket;

    @Value("${minio.public-url}")
    private String publicUrl;

    /**
     * Uploads a document (proposal attachment, deliverable). Returns the public URL.
     */
    public String uploadDocument(String folder, MultipartFile file) {
        validateDocument(file);
        String key = folder + "/" + UUID.randomUUID() + getExtension(file);
        return upload(key, file);
    }

    private String upload(String key, MultipartFile file) {
        try {
            PutObjectRequest req = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(req, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            String url = publicUrl + "/" + bucket + "/" + key;
            log.info("Uploaded file to MinIO: {}", url);
            return url;
        } catch (IOException e) {
            log.error("Failed to upload file to MinIO", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "File upload failed");
        }
    }

    private void validateDocument(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
        }
        if (!ALLOWED_DOC_TYPES.contains(file.getContentType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only PDF, Word documents, and images are allowed");
        }
        if (file.getSize() > MAX_DOC_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Document must be smaller than 20 MB");
        }
    }

    private String getExtension(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name != null && name.contains(".")) {
            return name.substring(name.lastIndexOf('.'));
        }
        return "";
    }
}
