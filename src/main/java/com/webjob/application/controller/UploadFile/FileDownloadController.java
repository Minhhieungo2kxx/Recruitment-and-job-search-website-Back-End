package com.webjob.application.controller.UploadFile;


import com.webjob.application.annotation.RateLimit;

import com.webjob.application.dto.Response.FileDownloadResponseDto;
import com.webjob.application.service.UploadFileServer.FileService;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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

//    @RateLimit(maxRequests = 10, timeWindowSeconds = 60, keyType = "TOKEN")
//    @GetMapping("/{folder}/{filename}")
//    public ResponseEntity<Resource> downloadFile(@PathVariable String folder, @PathVariable String filename) {
//        FileDownloadResponseDto dto = fileService.downloadFile(folder, filename);
//        return ResponseEntity.ok()
//                .contentType(MediaType.parseMediaType(dto.getContentType()))
//                .header(
//                        HttpHeaders.CONTENT_DISPOSITION,
//                        "attachment; filename*=UTF-8''" + dto.getEncodedFilename()
//                )
//                .contentLength(dto.getFileSize())
//                .body(dto.getResource());
//    }
    @RateLimit(maxRequests = 10, timeWindowSeconds = 60, keyType = "TOKEN")
    @GetMapping("/cloud/{encodedUrl}")
    public ResponseEntity<?> proxyDownloadFromCloudinary(@PathVariable String encodedUrl) {
        FileDownloadResponseDto dto = fileService.proxyDownloadCloudinary(encodedUrl);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + dto.getEncodedFileName()
                )
                .contentType(MediaType.parseMediaType(dto.getContentType()))
                .body(dto.getResource());
    }




}
