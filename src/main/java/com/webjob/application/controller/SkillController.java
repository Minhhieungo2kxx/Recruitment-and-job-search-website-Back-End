package com.webjob.application.controller;


import com.webjob.application.annotation.RateLimit;
import com.webjob.application.dto.Request.SkillRequest;
import com.webjob.application.dto.Request.SkillSearchRequest;
import com.webjob.application.dto.Response.ApiResponse;
import com.webjob.application.dto.Response.ResponseDTO;
import com.webjob.application.dto.Response.SkillOptionResponse;
import com.webjob.application.dto.Response.SkillResponse;
import com.webjob.application.service.SkillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/skills")
@RequiredArgsConstructor
public class SkillController {
    private final SkillService skillService;


    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "TOKEN")
    @PostMapping
    public ResponseEntity<ApiResponse<SkillResponse>> createSkill(@Valid @RequestBody SkillRequest skillRequest) {
        ApiResponse<SkillResponse> response = new ApiResponse<>(
                HttpStatus.CREATED.value(),
                null,
                "Create Skill successful",
                skillService.createSkill(skillRequest)

        );
        return new ResponseEntity<>(response, HttpStatus.CREATED);

    }

    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "TOKEN")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SkillResponse>> EditSkill(@PathVariable Long id, @Valid @RequestBody SkillRequest skillRequest) {
        ApiResponse<SkillResponse> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Edit Skill successful",
                skillService.updateSkill(id, skillRequest)

        );
        return new ResponseEntity<>(response, HttpStatus.OK);

    }

    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "TOKEN")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SkillResponse>> getSkillByID(@PathVariable Long id) {
        ApiResponse<SkillResponse> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Get Skill By ID successful",
                skillService.getSkillByID(id)

        );
        return new ResponseEntity<>(response, HttpStatus.OK);

    }

    @RateLimit(maxRequests = 15, timeWindowSeconds = 60, keyType = "IP")
    @GetMapping
    public ResponseEntity<ApiResponse<ResponseDTO<List<SkillResponse>>>> GetAllSkillPage(
            @RequestParam(defaultValue = "1") int page
            ,@RequestParam(defaultValue = "8") int size) {
        ApiResponse<ResponseDTO<List<SkillResponse>>> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "fetch all Skill Successful",
                skillService.getAllPageList(page, size)
        );
        return ResponseEntity.ok(response);

    }

    @RateLimit(maxRequests = 20, timeWindowSeconds = 60, keyType = "IP")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<ResponseDTO<List<SkillResponse>>>> searchSkill(
            @ModelAttribute SkillSearchRequest request
            ,@RequestParam(defaultValue = "1") int page
            ,@RequestParam(defaultValue = "8") int size) {
        ApiResponse<ResponseDTO<List<SkillResponse>>> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Search skill successfully",
                skillService.searchSkill(request, page, size)
        );
        return ResponseEntity.ok(response);
    }

    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "TOKEN")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> deleteSkill(@PathVariable Long id) {
        skillService.deleteSkill(id);
        ApiResponse<Object> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Delete Skill Successful",
                null
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/options")
    public ResponseEntity<ApiResponse<List<SkillOptionResponse>>> search(@RequestParam(required = false) String keyword) {
        ApiResponse<List<SkillOptionResponse>> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Search Skill Successful",
                skillService.searchSkillforSubscriber(keyword)
        );
        return ResponseEntity.ok(response);
    }

}
