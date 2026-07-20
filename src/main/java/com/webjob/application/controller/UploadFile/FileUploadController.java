package com.webjob.application.controller.UploadFile;

import com.webjob.application.annotation.RateLimit;
import com.webjob.application.service.UploadFileServer.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping("/api/v1/file")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileService fileService;

    //upload file Local
    @RateLimit(maxRequests = 10, timeWindowSeconds = 60, keyType = "TOKEN")
    @PostMapping("/server")
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file
            ,@RequestParam(value = "folder", defaultValue = "default") String folder) {
        return fileService.uploadFileServer(file, folder);
    }

    //Upload file dich vu ben thu 3 cloudinary (nhu kieu AWS)
    @RateLimit(maxRequests = 10, timeWindowSeconds = 60, keyType = "TOKEN")
    @PostMapping("/cloudinary")
    public ResponseEntity<?> uploadFileCloudinary(
            @RequestParam("file") MultipartFile file
            ,@RequestParam(value = "folder", defaultValue = "default") String folder
            ,Authentication authentication){
        return fileService.uploadFileCloudinary(file, folder, authentication);

    }


}

