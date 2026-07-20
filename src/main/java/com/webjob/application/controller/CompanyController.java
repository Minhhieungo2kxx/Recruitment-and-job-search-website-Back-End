package com.webjob.application.controller;


import com.webjob.application.annotation.RateLimit;
import com.webjob.application.dto.Request.CompanyAdminSearchRequest;
import com.webjob.application.dto.Request.CompanyDTO;
import com.webjob.application.dto.Request.CompanySearchRequest;
import com.webjob.application.dto.Response.ApiResponse;
import com.webjob.application.dto.Response.CompanyResponse;
import com.webjob.application.dto.Response.ResponseDTO;
import com.webjob.application.service.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/companies")
public class CompanyController {
    private final CompanyService companyService;


    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "TOKEN")
    @PostMapping
    public ResponseEntity<ApiResponse<CompanyResponse>> create(@Valid @RequestBody CompanyDTO companyDTO) {
        ApiResponse<CompanyResponse> response = new ApiResponse<>(
                HttpStatus.CREATED.value(),
                null,
                "Create Company successful",
                companyService.createCompany(companyDTO)

        );
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }


    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "TOKEN")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CompanyResponse>> EditCompany(@PathVariable Long id, @Valid @RequestBody CompanyDTO companyDTO) {
        ApiResponse<CompanyResponse> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Edit Company successful",
                companyService.update(id, companyDTO)
        );
        return ResponseEntity.ok(response);
    }


    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "IP")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CompanyResponse>> getCompanyID(@PathVariable Long id) {
        ApiResponse<CompanyResponse> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Get By ID Company successful",
                companyService.getById(id)
        );
        return ResponseEntity.ok(response);

    }

    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "TOKEN")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> deleteCompanyId(@PathVariable Long id) {
        companyService.deleteCompanyById(id);
        ApiResponse<Object> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Delete Company by ID successful",
                null

        );
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/restore")
    public ResponseEntity<ApiResponse<Object>> restoreCompany(@PathVariable Long id) {
        companyService.restoreCompanyById(id);
        ApiResponse<Object> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Khôi phục công ty " + id + " thành công.",
                null
        );
        return ResponseEntity.ok(response);
    }

    @RateLimit(maxRequests = 20, timeWindowSeconds = 60, keyType = "IP")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<ResponseDTO<List<CompanyResponse>>>> getByCompaniesClient(
            @RequestParam(defaultValue = "1") int page
            , @RequestParam(defaultValue = "10") int size
            , @RequestBody(required = false) CompanySearchRequest request) {
        ApiResponse<ResponseDTO<List<CompanyResponse>>> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Search Company for Client successful",
                companyService.getCompanyClient(page, size, request)
        );
        return ResponseEntity.ok(response);
    }

    @RateLimit(maxRequests = 25, timeWindowSeconds = 60, keyType = "IP")
    @GetMapping()
    public ResponseEntity<ApiResponse<ResponseDTO<List<CompanyResponse>>>> getCompanyAdmin(
            @RequestParam(defaultValue = "1") int page
            , @RequestParam(defaultValue = "10") int size
            , @RequestBody(required = false) CompanyAdminSearchRequest request) {
        ApiResponse<ResponseDTO<List<CompanyResponse>>> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Search Company for Admin successful",
                companyService.getCompanyAdmin(page,size,request)
        );
        return ResponseEntity.ok(response);
    }

}
