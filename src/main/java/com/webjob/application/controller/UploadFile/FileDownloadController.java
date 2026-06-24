package com.webjob.application.controller.UploadFile;


import com.webjob.application.annotation.RateLimit;

import com.webjob.application.service.UploadFileServer.FileService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/download")
public class FileDownloadController {

    private final FileService fileService;

    public FileDownloadController(FileService fileService) {
        this.fileService = fileService;
    }

    @RateLimit(maxRequests = 10, timeWindowSeconds = 60, keyType = "TOKEN")
    @GetMapping("/{folder}/{filename}")
    public ResponseEntity<?> downloadFile(@PathVariable String folder, @PathVariable String filename) {
        return fileService.handledownloadFile(folder,filename);
    }
    @RateLimit(maxRequests = 10, timeWindowSeconds = 60, keyType = "TOKEN")
    @GetMapping("/cloud/{encodedUrl}")
    public ResponseEntity<?> proxyDownloadFromCloudinary(@PathVariable String encodedUrl) {
        return fileService.proxyDownloadCloudinary(encodedUrl);
    }




}
