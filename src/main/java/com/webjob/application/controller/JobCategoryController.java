package com.webjob.application.controller;

import com.webjob.application.annotation.RateLimit;
import com.webjob.application.dto.Request.JobCategoryRequest;
import com.webjob.application.dto.Request.JobCategorySearchRequest;
import com.webjob.application.dto.Response.*;
import com.webjob.application.service.JobCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/job-categories")
@RequiredArgsConstructor
public class JobCategoryController {

    private final JobCategoryService jobCategoryService;


    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "TOKEN")
    @PostMapping
    public ResponseEntity<ApiResponse<JobCategoryResponse>> create(@RequestBody @Valid JobCategoryRequest request) {
        ApiResponse<JobCategoryResponse> response = new ApiResponse<>(
                HttpStatus.CREATED.value(),
                null,
                "Create JobCategory successful",
                jobCategoryService.create(request)

        );
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "TOKEN")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<JobCategoryResponse>> update(@PathVariable Long id, @RequestBody @Valid JobCategoryRequest request) {
        ApiResponse<JobCategoryResponse> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Update JobCategory successful" + id,
                jobCategoryService.update(id, request)

        );
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "TOKEN")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<JobCategoryResponse>> getByID(@PathVariable Long id) {
        ApiResponse<JobCategoryResponse> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Get JobCategory successful" + id,
                jobCategoryService.getById(id)

        );
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "TOKEN")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> deleteByID(@PathVariable Long id) {
        jobCategoryService.delete(id);
        ApiResponse<Object> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Delete JobCategory successful with " + id,
                null

        );
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RateLimit(maxRequests = 15, timeWindowSeconds = 60, keyType = "IP")
    @GetMapping
    public ResponseEntity<ApiResponse<ResponseDTO<List<JobCategoryResponse>>>> getAllOrSearch(
            @ModelAttribute JobCategorySearchRequest request,
            @RequestParam(defaultValue = "1") int page
            , @RequestParam(defaultValue = "8") int size) {
        ApiResponse<ResponseDTO<List<JobCategoryResponse>>> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "fetch all JobCategories Successful",
                jobCategoryService.getAllPageList(page, size,request)
        );
        return ResponseEntity.ok(response);

    }

    @RateLimit(maxRequests = 15, timeWindowSeconds = 60, keyType = "IP")
    @GetMapping("/tree")
    public ResponseEntity<ApiResponse<List<JobCategoryTreeResponse>>> getTree() {
        ApiResponse<List<JobCategoryTreeResponse>> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Fetch job category tree successfully",
                jobCategoryService.getTree()
        );
        return ResponseEntity.ok(response);
    }
    @RateLimit(maxRequests = 15, timeWindowSeconds = 60, keyType = "IP")
    @GetMapping("/{id}/children")
    public ResponseEntity<ApiResponse<JobCategoryResponseChildren>> getJobCategoryChildrenById(@PathVariable Long id) {
        ApiResponse<JobCategoryResponseChildren> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Fetch job category Node Children tree successfully",
                jobCategoryService.getJobCategoriesChildren(id)
        );
        return ResponseEntity.ok(response);
    }


}
//git commit -m "feat(job-category): implement complete job category management"


//Để xác định chính xác kiểu dữ liệu cần đặt bên trong dấu <> của ResponseEntity
//, cần dựa vào một quy tắc cốt lõi:"Controller trả về cái gì cho Client thì kiểu dữ liệu trong <> phải là cái đó".
//Nói cách khác, kiểu dữ liệu trong <> chính là kiểu của phần Body (thân hàm) mà bạn sẽ truyền vào ResponseEntity.ok(body).