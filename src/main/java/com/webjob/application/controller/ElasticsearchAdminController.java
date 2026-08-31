package com.webjob.application.controller;

import com.webjob.application.annotation.RateLimit;
import com.webjob.application.dto.Response.ApiResponse;
import com.webjob.application.elasticsearch.ElasticsearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/elasticsearch")
public class ElasticsearchAdminController {
    private final ElasticsearchService elasticsearchService;


    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "TOKEN")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @PostMapping("/migrate-jobs")
    public ResponseEntity<ApiResponse<Object>> triggerJobMigration() {
        elasticsearchService.migrateAllJobs();
        ApiResponse<Object> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Migrate Job successful",
                null
        );
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "TOKEN")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @PostMapping("/migrate-companies")
    public ResponseEntity<ApiResponse<Object>> triggerCompanyMigration() {
        elasticsearchService.migrateAllCompanies();
        ApiResponse<Object> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Migrate JobDocument successful",
                null
        );
        return new ResponseEntity<>(response, HttpStatus.OK);
    }




}
