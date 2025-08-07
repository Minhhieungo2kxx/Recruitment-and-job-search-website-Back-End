package com.webjob.application.Controller.UploadFile;


import com.webjob.application.Models.Response.ApiResponse;
import com.webjob.application.Models.Response.UploadFileResponse;
import com.webjob.application.Services.UploadFileServer.CloudinaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;

@RestController
@RequestMapping("/api/v1/files")
public class UploadCloudinaryController {
    @Autowired
    private CloudinaryService cloudinaryService;

    @PostMapping("/cloudinary")  // sửa typo từ "clodinary" thành "cloudinary"
    public ResponseEntity<?> uploadFileCloudinary(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "default") String folder) {

        try {
            String uploadedFileName =cloudinaryService.uploadFile(file,folder);
            UploadFileResponse uploadFileResponse=new UploadFileResponse(uploadedFileName, Instant.now(),folder);
            ApiResponse<?> apiResponse = new ApiResponse<>(HttpStatus.OK.value(), null,
                    "Tải file thành công!",
                    uploadFileResponse
            );
            return ResponseEntity.ok(apiResponse);

        } catch (IllegalStateException | IOException e) {

            ApiResponse<?> apiResponse = new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), e.getMessage(),
                    "Tải file Lỗi",
                    null
            );
            return ResponseEntity.badRequest().body(apiResponse);
        }


    }


}
