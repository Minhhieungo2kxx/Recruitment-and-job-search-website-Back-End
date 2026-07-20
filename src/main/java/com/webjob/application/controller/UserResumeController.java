package com.webjob.application.controller;

import com.webjob.application.annotation.RateLimit;
import com.webjob.application.dto.Request.UpdateNameAnDefaultCVRequest;
import com.webjob.application.dto.Request.UploadResumeRequest;
import com.webjob.application.dto.Response.AdminResumeResponse;
import com.webjob.application.dto.Response.ApiResponse;
import com.webjob.application.dto.Response.ResponseDTO;
import com.webjob.application.dto.Response.UserResumeResponse;
import com.webjob.application.service.UserResumeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/user-resumes")
@RequiredArgsConstructor
public class UserResumeController {
    private final UserResumeService userResumeService;

    //    lay danh sach cv cu da tao cua user khi apply vao job de checkbox chon or tao moi (user)
    @RateLimit(maxRequests = 15, timeWindowSeconds = 60, keyType = "TOKEN")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<UserResumeResponse>>> getMyResumes() {

        ApiResponse<List<UserResumeResponse>> apiResponse = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Get All CV User thanh cong",
                userResumeService.getMyResumes()
        );
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    //    lay danh sach cv  da tao cua user + phan trang cho user (get All)
    @RateLimit(maxRequests = 15, timeWindowSeconds = 60, keyType = "TOKEN")
    @GetMapping("/by-user")
    public ResponseEntity<ApiResponse<ResponseDTO<List<UserResumeResponse>>>> getMyResumesPageList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        ApiResponse<ResponseDTO<List<UserResumeResponse>>> apiResponse = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Get All My resumes pageList thanh cong",
                userResumeService.getMyResumesPageList(page, size)
        );
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);

    }

    //    lay danh sach cv all user + phan trang cho admin (get All admin)
    @RateLimit(maxRequests = 15, timeWindowSeconds = 60, keyType = "TOKEN")
    @GetMapping("/admin")
    public ResponseEntity<ApiResponse<ResponseDTO<List<AdminResumeResponse>>>> getAllCvAdmin(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        ApiResponse<ResponseDTO<List<AdminResumeResponse>>> apiResponse = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Get All resumes thanh cong",
                userResumeService.getAllCvAdmin(page, size)
        );
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);

    }

    //    danh cho user xem detail Resumme (detail user)
    @RateLimit(maxRequests = 15, timeWindowSeconds = 60, keyType = "TOKEN")
    @PutMapping("/me/{id}")
    public ResponseEntity<ApiResponse<UserResumeResponse>> getByResumeByUserId(@PathVariable Long id) {
        ApiResponse<UserResumeResponse> apiResponse = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Get Resumes By User by Id thanh cong",
                userResumeService.getById(id)
        );
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);

    }

//    tao cv cho user
    @RateLimit(maxRequests = 15, timeWindowSeconds = 60, keyType = "TOKEN")
    @PostMapping
    public ResponseEntity<ApiResponse<UserResumeResponse>> uploadResume(@Valid @RequestBody UploadResumeRequest request) {
        ApiResponse<UserResumeResponse> api = new ApiResponse<>(
                HttpStatus.CREATED.value(),
                null,
                "Upload CV thành công",
                userResumeService.createResume(request)
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(api);
    }

//    update ten va default cho user
    @RateLimit(maxRequests = 15, timeWindowSeconds = 60, keyType = "TOKEN")
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResumeResponse>> updateNameAndDefaultCV(@PathVariable Long id
            , @Valid @RequestBody UpdateNameAnDefaultCVRequest request) {
        ApiResponse<UserResumeResponse> api = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Update Resume Name and Default thành công",
                userResumeService.updateResume(id, request)
        );
        return ResponseEntity.status(HttpStatus.OK).body(api);
    }
    @RateLimit(maxRequests = 7, timeWindowSeconds = 60, keyType = "TOKEN")
    @DeleteMapping("/my-resumes/{id}")
    public ResponseEntity<ApiResponse<Object>> deleteMyResume(@PathVariable Long id) {

        userResumeService.deleteMyResume(id);
        ApiResponse<Object> api = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Deleted Resume for Client Successful",
                null
        );
        return ResponseEntity.status(HttpStatus.OK).body(api);
    }
    @RateLimit(maxRequests = 7, timeWindowSeconds = 60, keyType = "TOKEN")
    @DeleteMapping("/admin/resumes/{id}")
    public ResponseEntity<ApiResponse<Object>> deleteResume(@PathVariable Long id) {

        userResumeService.deleteResumeByAdmin(id);
        ApiResponse<Object> api = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Deleted Resume for Admin Successful",
                null
        );
        return ResponseEntity.status(HttpStatus.OK).body(api);


    }

}
//user:get all,detail,create,update
//admin:get all,detail,

//git commit -m "feat(resume): implement user resume lifecycle management"
