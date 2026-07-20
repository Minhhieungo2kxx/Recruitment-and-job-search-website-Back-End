package com.webjob.application.controller;

import com.webjob.application.annotation.RateLimit;
import com.webjob.application.dto.Request.RoleRequest;
import com.webjob.application.dto.Response.ApiResponse;
import com.webjob.application.dto.Response.ResponseDTO;
import com.webjob.application.dto.Response.RoleResponse;
import com.webjob.application.models.Entity.Role;

import com.webjob.application.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;


    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "TOKEN")
    @PostMapping
    public ResponseEntity<ApiResponse<RoleResponse>> createRole(@Valid @RequestBody RoleRequest request) {
        RoleResponse response = roleService.createRole(request);
        ApiResponse<RoleResponse> apiResponse = new ApiResponse<>(
                HttpStatus.CREATED.value(),
                null,
                "Tạo Role thành công",
                response
        );
        return new ResponseEntity<>(apiResponse, HttpStatus.CREATED);
    }

    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "TOKEN")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RoleResponse>> editRole(@PathVariable Long id
            ,@Valid @RequestBody RoleRequest request) {

        ApiResponse<RoleResponse> apiResponse = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Update Role thành công",
                roleService.editRole(id, request));
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    @RateLimit(maxRequests = 10, timeWindowSeconds = 60, keyType = "TOKEN")
    @GetMapping
    public ResponseEntity<ApiResponse<ResponseDTO<List<RoleResponse>>>> GetallPageList(
            @RequestParam(defaultValue = "1") int page
            ,@RequestParam(defaultValue = "8") int size) {

        ApiResponse<ResponseDTO<List<RoleResponse>>> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "fetch all Roles Successful",
                roleService.getPaginated(page,size)
        );
        return ResponseEntity.ok(response);

    }

    @RateLimit(maxRequests = 3, timeWindowSeconds = 60, keyType = "TOKEN")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> deleteRole_byId(@PathVariable Long id) {
        roleService.deleteRole(id);
        ApiResponse<Object> response = new ApiResponse<>(HttpStatus.OK.value(),
                null,
                "Delete Role successful with " + id,
                null

        );
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @RateLimit(maxRequests = 10, timeWindowSeconds = 60, keyType = "TOKEN")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Role>> detailRole_byId(@PathVariable Long id) {

        ApiResponse<Role> response = new ApiResponse<>(HttpStatus.OK.value(),
                null,
                "Detail Role successful with " + id,
                roleService.detailById(id)

        );
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @RateLimit(maxRequests = 10, timeWindowSeconds = 60, keyType = "TOKEN")
    @PutMapping("/{id}/restore")
    public ResponseEntity<ApiResponse<RoleResponse>> restoreRole(@PathVariable Long id) {
        RoleResponse restore = roleService.restoreRole(id);
        ApiResponse<RoleResponse> apiResponse = new ApiResponse<>(HttpStatus.OK.value(), null,
                "Restore Role thành công",
                restore);
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }
}
