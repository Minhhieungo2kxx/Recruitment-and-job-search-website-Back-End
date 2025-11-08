package com.webjob.application.Controller.UploadFile;


import com.webjob.application.Annotation.RateLimit;
import com.webjob.application.Config.UploadfileServer.UploadProperties;

import com.webjob.application.Service.UploadFileServer.FileService;
import com.webjob.application.Util.Base64Util;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
