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

@RestController
@RequestMapping("/api/v1/permissions") // base path chuẩn
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "TOKEN")
    @PostMapping
    public ResponseEntity<?> createPermission(@Valid @RequestBody Permission permission) {
        return permissionService.create_Permission(permission);

    }

    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "TOKEN")
    @PutMapping("/{id}")
    public ResponseEntity<?> updatePermission(@PathVariable Long id, @Valid @RequestBody Permission permission) {

       return permissionService.edit_Permission(id,permission);

    }

    @RateLimit(maxRequests = 10, timeWindowSeconds = 60, keyType = "TOKEN")
    @GetMapping
    public ResponseEntity<?> GetallPageList(@RequestParam(value ="page") String pageparam){
        return permissionService.all_PageList(pageparam);

    }

    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "TOKEN")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePermissionbyId(@PathVariable Long id) {
        return permissionService.delete_PermissionbyId(id);
    }

}
