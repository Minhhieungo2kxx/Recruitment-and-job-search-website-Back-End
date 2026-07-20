package com.webjob.application.controller;

import com.webjob.application.annotation.RateLimit;
import com.webjob.application.dto.Request.ApplyRequest;
import com.webjob.application.dto.Request.UpdateApplicationStatusRequest;
import com.webjob.application.dto.Response.*;
import com.webjob.application.enums.ResumeStatus;
import com.webjob.application.service.ApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
public class ApplicationController {
    private final ApplicationService applicationService;

    //    apply vao 1 job user (create application and resume) for user
    @RateLimit(maxRequests = 10, timeWindowSeconds = 60, keyType = "TOKEN")
    @PostMapping
    public ResponseEntity<ApiResponse<ApplicationResponse>> applyJob(@Valid @RequestBody ApplyRequest request) {

        ApiResponse<ApplicationResponse> apiResponse = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Ứng tuyển thành công",
                applicationService.apply(request)
        );
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    //    lay ra danh sach application cv ung vien nop vao job cua cong ty hr (get All for Hr)
    @RateLimit(maxRequests = 8, timeWindowSeconds = 60, keyType = "TOKEN")
    @GetMapping("/by-company")
    public ResponseEntity<ApiResponse<ResponseDTO<List<ApplicationHRResponse>>>> getAllResumeByCompany(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        ApiResponse<ResponseDTO<List<ApplicationHRResponse>>> apiResponse = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Lấy danh sách CV thuộc công ty Hr thành công !",
                applicationService.listResumeHRCompany(page, size)
        );
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);

    }

    //    cap nhat trang thai application ung vien (update for hr)
    @RateLimit(maxRequests = 8, timeWindowSeconds = 60, keyType = "TOKEN")
    @PutMapping("/applications/{id}/status")
    public ResponseEntity<ApiResponse<ApplicationResponse>> updateApplicationStatus(@PathVariable Long id
            , @Valid @RequestBody UpdateApplicationStatusRequest request) {

        ApiResponse<ApplicationResponse> apiResponse = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Cập nhật trạng thái ứng tuyển thành công",
                applicationService.updateStatusByHr(id, request)
        );
        return ResponseEntity.ok(apiResponse);
    }

    //    lay ra danh sach lich su application for user (Get All for user)
    @RateLimit(maxRequests = 15, timeWindowSeconds = 60, keyType = "TOKEN")
    @GetMapping("/history-applied")
    public ResponseEntity<ApiResponse<ResponseDTO<List<ResumeHistoryResponse>>>> getResumeHistoryApplied(
            @RequestParam(defaultValue = "0") int page
            , @RequestParam(defaultValue = "10") int size
            , @RequestParam(required = false) ResumeStatus status) {
        ApiResponse<ResponseDTO<List<ResumeHistoryResponse>>> apiResponse = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Fetch all History Applied Resumes Apply Successful",
                applicationService.getHistoryAppliedClient(page, size, status)
        );
        return ResponseEntity.ok(apiResponse);
    }

    //    detail application for user
    @RateLimit(maxRequests = 7, timeWindowSeconds = 60, keyType = "TOKEN")
    @GetMapping("me/{id}")
    public ResponseEntity<ApiResponse<ApplicationUserDetailResponse>> getApplicationDetail(@PathVariable Long id) {
        ApiResponse<ApplicationUserDetailResponse> apiResponse = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Detail Application successful",
                applicationService.getApplicationDetailForUser(id)
        );
        return ResponseEntity.ok(apiResponse);
    }

    //    detail for hr
    @RateLimit(maxRequests = 7, timeWindowSeconds = 60, keyType = "TOKEN")
    @GetMapping("/hr/{id}")
    public ResponseEntity<ApiResponse<ApplicationHrDetailResponse>> getApplicationDetailForHr(@PathVariable Long id) {
        ApiResponse<ApplicationHrDetailResponse> apiResponse = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Detail Application successful",
                applicationService.getApplicationDetailForHR(id)
        );
        return ResponseEntity.ok(apiResponse);
    }
//lay all application for admin
@RateLimit(maxRequests =10,timeWindowSeconds = 60, keyType = "TOKEN")
    @GetMapping("/admin")
    public ResponseEntity<ApiResponse<ResponseDTO<List<AdminApplicationResponse>>>> getAllApplicationForAdmin(
            @RequestParam(defaultValue = "0") int page
            , @RequestParam(defaultValue = "10") int size
            , @RequestParam(required = false) ResumeStatus status) {
        ApiResponse<ResponseDTO<List<AdminApplicationResponse>>> apiResponse = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Get All Application for admin successful",
                applicationService.getAllApplicationsForAdmin(page, size, status)
        );
        return ResponseEntity.ok(apiResponse);
    }

    @RateLimit(maxRequests =10,timeWindowSeconds = 60, keyType = "TOKEN")
    @DeleteMapping("/{id}")
    public ResponseEntity< ApiResponse<Object>> clientDeleteApplication(@PathVariable Long id) {
        applicationService.deleteByClient(id);
        ApiResponse<Object> apiResponse = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Deleted Applied for Client Successful",
               null
        );
        return ResponseEntity.ok(apiResponse);

    }
    @RateLimit(maxRequests =10,timeWindowSeconds = 60, keyType = "TOKEN")
    @DeleteMapping("/{id}/admin")
    public ResponseEntity<ApiResponse<Object>> adminDeleteApplication(@PathVariable Long id) {
        applicationService.deleteByAdmin(id);
        ApiResponse<Object> apiResponse = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Deleted Applied for Admin Successful",
                null
        );
        return ResponseEntity.ok(apiResponse);
    }




}

// user:create,get all,detail,delete
//hr:update status application,get all,detail
//admin: get all, delete

//git commit -m "feat(application): implement application lifecycle management"