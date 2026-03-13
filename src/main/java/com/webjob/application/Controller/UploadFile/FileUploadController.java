package com.webjob.application.Controller.UploadFile;

import com.webjob.application.Annotation.RateLimit;
import com.webjob.application.Config.UploadfileServer.UploadFile;
import com.webjob.application.Dto.Response.ApiResponse;
import com.webjob.application.Dto.Response.UploadFileResponse;
import com.webjob.application.Service.UploadFileServer.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/file")
@Slf4j
public class FileUploadController {
    private final UploadFile uploadFile;

    private final FileService fileService;

    public FileUploadController(UploadFile uploadFile, FileService fileService) {
        this.uploadFile = uploadFile;
        this.fileService = fileService;

    }

    //    upload file Local
    @RateLimit(maxRequests = 10, timeWindowSeconds = 60, keyType = "TOKEN")
    @PostMapping("/server")
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "default") String folder) throws IOException {
            String uploadedFileName = uploadFile.getnameFile(file, folder);
            UploadFileResponse uploadFileResponse =UploadFileResponse.builder()
                    .fileName(uploadedFileName).uploadedAt(Instant.now())
                    .fileSize(file.getSize())
                    .contentType(file.getContentType())
                    .folder(folder).build();
            ApiResponse<?> apiResponse = new ApiResponse<>(HttpStatus.OK.value(), null,
                    "Tải file thành công!",
                    uploadFileResponse
            );
            return ResponseEntity.ok(apiResponse);

    }

    //    Upload file dich vu ben thu 3 cloudinary (nhu kieu AWS)
    @RateLimit(maxRequests = 10, timeWindowSeconds = 60, keyType = "TOKEN")
    @PostMapping("/cloudinary")
    public ResponseEntity<?> uploadFileCloudinary(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "default") String folder, Authentication authentication) throws IOException {
            Map<String,String> uploadedFileName = fileService.uploadFile(file, folder,authentication);
            UploadFileResponse uploadFileResponse =UploadFileResponse.builder()
                    .fileName(uploadedFileName.get("url"))
                    .public_id(uploadedFileName.get("publicId"))
                    .resourceType(uploadedFileName.get("resourceType"))
                    .uploadedAt(Instant.now())
                    .fileSize(file.getSize())
                    .contentType(file.getContentType())
                    .folder(folder).build();
            ApiResponse<?> apiResponse = new ApiResponse<>(HttpStatus.OK.value(), null,
                    "Tải file thành công lên Cloudinary",
                    uploadFileResponse
            );
            return ResponseEntity.ok(apiResponse);

        }

    }

