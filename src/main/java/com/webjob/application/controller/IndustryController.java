package com.webjob.application.controller;

import com.webjob.application.dto.Request.IndustryFilter;
import com.webjob.application.dto.Request.IndustryRequest;
import com.webjob.application.dto.Response.ApiResponse;
import com.webjob.application.dto.Response.IndustryResponse;
import com.webjob.application.dto.Response.ResponseDTO;
import com.webjob.application.service.IndustryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/industries")
@RequiredArgsConstructor
public class IndustryController {
    private final IndustryService industryService;

    @PostMapping
    public ResponseEntity<ApiResponse<IndustryResponse>> create(@Valid @RequestBody IndustryRequest request) {
        ApiResponse<IndustryResponse> apiResponse = new ApiResponse<>(
                HttpStatus.CREATED.value(),
                null,
                "Tạo Industry thành công",
                industryService.create(request)
        );
        return new ResponseEntity<>(apiResponse, HttpStatus.CREATED);

    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<IndustryResponse>> update(@PathVariable Long id, @Valid @RequestBody IndustryRequest request) {
        ApiResponse<IndustryResponse> apiResponse = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Update Industry thành công",
                industryService.update(id, request)
        );

        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<IndustryResponse>> getById(@PathVariable Long id) {
        ApiResponse<IndustryResponse> apiResponse = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Get Industry by ID thành công",
                industryService.getById(id)
        );

        return ResponseEntity.ok(apiResponse);

    }

    @GetMapping("/clients")
    public ResponseEntity<ApiResponse<List<IndustryResponse>>> getAll() {
        ApiResponse<List<IndustryResponse>> apiResponse = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Get All Industry  thành công",
                industryService.getAll()
        );
        return ResponseEntity.ok(apiResponse);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> delete(@PathVariable Long id) {
        industryService.delete(id);
        ApiResponse<Object> apiResponse = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Deleted Industry by ID  thành công",
                null
        );
        return ResponseEntity.ok(apiResponse);
    }

    @PutMapping("/{id}/restore")
    public ResponseEntity<ApiResponse<IndustryResponse>> restore(@PathVariable Long id) {
        ApiResponse<IndustryResponse> apiResponse = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Restored Industry by ID  thành công",
                industryService.restore(id)
        );
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ResponseDTO<List<IndustryResponse>>>> getAllIndustries(
            @RequestParam(defaultValue = "0") int page
            , @RequestParam(defaultValue = "10") int size
            , @RequestBody(required = false) IndustryFilter filter) {

        ApiResponse<ResponseDTO<List<IndustryResponse>>> apiResponse = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Lấy danh sách Industries thành công",
                industryService.getIndustries(page, size, filter)
        );
        return ResponseEntity.ok(apiResponse);

    }



}
//git commit -m "feat(industry): implement complete industry management"
