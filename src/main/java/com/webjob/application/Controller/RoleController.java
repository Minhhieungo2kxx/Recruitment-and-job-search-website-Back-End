package com.webjob.application.Controller;

import com.webjob.application.Annotation.RateLimit;
import com.webjob.application.Dto.Response.ApiResponse;
import com.webjob.application.Dto.Response.ResponseDTO;
import com.webjob.application.Model.Entity.Role;

import com.webjob.application.Service.RoleService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/roles")
public class RoleController {
    @Autowired
    private RoleService roleService;


    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "TOKEN")
    @PostMapping
    public ResponseEntity<?> createRole(@Valid @RequestBody Role role) {
        Role save=roleService.createRole(role);
        ApiResponse<?> apiResponse=new ApiResponse<>(HttpStatus.CREATED.value(), null,
                "Tạo Role thành công",
                save);
        return new ResponseEntity<>(apiResponse,HttpStatus.CREATED);
    }

    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "TOKEN")
    @PutMapping("/{id}")
    public ResponseEntity<?> editRole(@PathVariable Long id, @Valid @RequestBody Role role) {
        Role edit=roleService.EditRole(id,role);
        ApiResponse<?> apiResponse=new ApiResponse<>(HttpStatus.OK.value(), null,
                "Update Role thành công",
                edit);
        return new ResponseEntity<>(apiResponse,HttpStatus.OK);
    }

    @RateLimit(maxRequests = 10, timeWindowSeconds = 60, keyType = "TOKEN")
    @GetMapping
    public ResponseEntity<?> GetallPageList(@RequestParam(value ="page") String pageparam){
        ResponseDTO<?> respond=roleService.getPaginated(pageparam,"default");
        ApiResponse<?> response=new ApiResponse<>(HttpStatus.OK.value(), null,
                "fetch all Roles Successful",
                respond
        );
        return ResponseEntity.ok(response);

    }
    @RateLimit(maxRequests = 3, timeWindowSeconds = 60, keyType = "TOKEN")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRolebyId(@PathVariable Long id) {
        roleService.deleteRole(id);
        ApiResponse<Object> response = new ApiResponse<>(HttpStatus.OK.value(),
                null,
                "Delete Role successful with "+id,
                null

        );
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @RateLimit(maxRequests = 10, timeWindowSeconds = 60, keyType = "TOKEN")
    @GetMapping("/{id}")
    public ResponseEntity<?> detailRolebyId(@PathVariable Long id) {
        Role role=roleService.getByid(id).orElseThrow(()->new IllegalArgumentException("Role not found with "+id));
        ApiResponse<?> response = new ApiResponse<>(HttpStatus.OK.value(),
                null,
                "Detail Role successful with "+id,
                role

        );
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
