package com.webjob.application.controller;


import com.webjob.application.annotation.RateLimit;
import com.webjob.application.models.Entity.Permission;
import com.webjob.application.dto.Response.ApiResponse;
import com.webjob.application.dto.Response.ResponseDTO;
import com.webjob.application.service.PermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/permissions") // base path chuẩn
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "TOKEN")
    @PostMapping
    public ResponseEntity<ApiResponse<Object>> createPermission(@Valid @RequestBody Permission permission) {
        Permission save=permissionService.createPermission(permission);
        ApiResponse<Object> apiResponse=ApiResponse.builder()
                .statusCode(HttpStatus.CREATED.value())
                .error(null)
                .message("Tạo Permission thành công")
                .data(save)
                .build();
        return new ResponseEntity<>(apiResponse,HttpStatus.CREATED);


    }

    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "TOKEN")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> updatePermission(@PathVariable Long id, @Valid @RequestBody Permission permission) {
        Permission edit=permissionService.editPermission(id,permission);
        ApiResponse<Object> apiResponse=ApiResponse.builder()
                .statusCode(HttpStatus.OK.value())
                .error(null)
                .message("Edit Permission thành công")
                .data(edit)
                .build();
        return new ResponseEntity<>(apiResponse,HttpStatus.OK);




    }

    @RateLimit(maxRequests = 10, timeWindowSeconds = 60, keyType = "TOKEN")
    @GetMapping
    public ResponseEntity<ApiResponse<ResponseDTO<List<Permission>>>> GetallPageList(
            @RequestParam(defaultValue = "0") int page
            ,@RequestParam(defaultValue = "10") int size)
    {

        ApiResponse<ResponseDTO<List<Permission>>> response=new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Fetch all PerMissions Successful",
                permissionService.getPaginated(page,size)
        );
        return ResponseEntity.ok(response);

    }

    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "TOKEN")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> deletePermissionById(@PathVariable Long id) {
        permissionService.deletePerMission(id);
        ApiResponse<Object> response = new ApiResponse<>(HttpStatus.OK.value(),
                null,
                "Delete Permission successful with "+id,
                null

        );
        return ResponseEntity.status(HttpStatus.OK).body(response);

    }

}
