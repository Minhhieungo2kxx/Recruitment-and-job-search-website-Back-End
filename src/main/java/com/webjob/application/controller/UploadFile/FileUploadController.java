package com.webjob.application.controller.UploadFile;

import com.webjob.application.annotation.RateLimit;
import com.webjob.application.dto.Response.ApiResponse;
import com.webjob.application.dto.Response.UploadFileResponse;
import com.webjob.application.service.UploadFileServer.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping("/api/v1/file")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileService fileService;

    //upload file Local
//    @RateLimit(maxRequests = 10, timeWindowSeconds = 60, keyType = "TOKEN")
//    @PostMapping("/server")
//    public ResponseEntity<ApiResponse<UploadFileResponse>> uploadFile(
//            @RequestParam("file") MultipartFile file
//            , @RequestParam(value = "folder", defaultValue = "default") String folder) {
//        ApiResponse<UploadFileResponse> apiResponse = new ApiResponse<>(
//                HttpStatus.OK.value(),
//                null,
//                "Tải file thành công!",
//                fileService.uploadFileServer(file, folder)
//        );
//        return ResponseEntity.ok(apiResponse);
//    }

    //Upload file dich vu ben thu 3 cloudinary (nhu kieu AWS)
    @RateLimit(maxRequests = 10, timeWindowSeconds = 60, keyType = "TOKEN")
    @PostMapping("/cloudinary")
    public ResponseEntity<ApiResponse<UploadFileResponse>> uploadFileCloudinary(
            @RequestParam("file") MultipartFile file
            , @RequestParam(value = "folder", defaultValue = "default") String folder) {
        ApiResponse<UploadFileResponse> apiResponse = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Tải file thành công lên Cloudinary",
                fileService.uploadFileCloudinary(file, folder)
        );
        return ResponseEntity.ok(apiResponse);

    }


}

