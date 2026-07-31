package com.webjob.application.dto.Response;

import lombok.Builder;
import lombok.Data;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;

@Data
@Builder
public class FileDownloadResponseDto {
    /**
     * Stream dữ liệu từ Cloudinary
     */
    private InputStreamResource resource;

    /**
     * MIME type
     */
    private String contentType;

    /**
     * Tên file đã encode
     */
    private String encodedFileName;

    /**
     * HTTP status trả về từ Cloudinary
     */
    private Integer statusCode;
}
