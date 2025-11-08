package com.webjob.application.Config.UploadfileServer;


import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

@Component
@Data
public class UploadProperties {
    @Value("${upload.base-dir}")
    private String baseDir;
    @Value("${spring.servlet.multipart.max-file-size}")
    private DataSize maxFileSize;
    @Value("${spring.servlet.multipart.max-request-size}")
    private DataSize maxRequestSize;


}

