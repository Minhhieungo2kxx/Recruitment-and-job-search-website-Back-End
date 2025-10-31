package com.webjob.application.Controller;


import com.webjob.application.Annotation.RateLimit;
import com.webjob.application.Model.Entity.Permission;
import com.webjob.application.Dto.Response.ApiResponse;
import com.webjob.application.Dto.Response.ResponseDTO;
import com.webjob.application.Service.PermissionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/permissions") // base path chuẩn
public class PermissionController {

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "TOKEN")
    @PostMapping
    public ResponseEntity<?> createPermission(@Valid @RequestBody Permission permission) {
        Permission save=permissionService.savePermission(permission);
        ApiResponse<?> apiResponse=new ApiResponse<>(HttpStatus.CREATED.value(), null,
                "Tạo Permission thành công",
                save);
        return new ResponseEntity<>(apiResponse,HttpStatus.CREATED);

    }

    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "TOKEN")
    @PutMapping("/{id}")
    public ResponseEntity<?> createPermission(@PathVariable Long id, @Valid @RequestBody Permission permission) {
        Permission edit=permissionService.editPermission(id,permission);
        ApiResponse<?> apiResponse=new ApiResponse<>(HttpStatus.CREATED.value(), null,
                "Edit Permission thành công",
                edit);
        return new ResponseEntity<>(apiResponse,HttpStatus.CREATED);

    }

    @RateLimit(maxRequests = 10, timeWindowSeconds = 60, keyType = "TOKEN")
    @GetMapping
    public ResponseEntity<?> GetallPageList(@RequestParam(value ="page") String pageparam){
        ResponseDTO<?> respond=permissionService.getPaginated(pageparam,"default");
        ApiResponse<?> response=new ApiResponse<>(HttpStatus.OK.value(), null,
                "Fetch all PerMissions Successful",
                respond
        );
        return ResponseEntity.ok(response);

    }

    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "TOKEN")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePermissionbyId(@PathVariable Long id) {
        Permission permission=permissionService.getByID(id);
        permissionService.deletePerMission(id);
        ApiResponse<Object> response = new ApiResponse<>(HttpStatus.OK.value(),
                null,
                "Delete Permission successful with "+id,
                null

        );
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

}
