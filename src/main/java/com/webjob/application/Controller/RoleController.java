package com.webjob.application.Controller;

import com.webjob.application.Models.*;
import com.webjob.application.Models.Request.JobRequest;
import com.webjob.application.Models.Response.*;
import com.webjob.application.Services.RoleService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
public class RoleController {
    @Autowired
    private RoleService roleService;


    @PostMapping("/create/role")
    public ResponseEntity<?> createRole(@Valid @RequestBody Role role) {
        Role save=roleService.createRole(role);
        ApiResponse<?> apiResponse=new ApiResponse<>(HttpStatus.CREATED.value(), null,
                "Tạo Role thành công",
                save);
        return new ResponseEntity<>(apiResponse,HttpStatus.CREATED);
    }
    @PutMapping("/edit/role/{id}")
    public ResponseEntity<?> editRole(@PathVariable Long id, @Valid @RequestBody Role role) {
        Role edit=roleService.EditRole(id,role);
        ApiResponse<?> apiResponse=new ApiResponse<>(HttpStatus.OK.value(), null,
                "Update Role thành công",
                edit);
        return new ResponseEntity<>(apiResponse,HttpStatus.OK);
    }
    @GetMapping("/api/roles")
    public ResponseEntity<?> GetallPageList(@RequestParam(value ="page") String pageparam){
        ResponseDTO<?> respond=roleService.getPaginated(pageparam,"default");
        ApiResponse<?> response=new ApiResponse<>(HttpStatus.OK.value(), null,
                "fetch all Roles Successful",
                respond
        );
        return ResponseEntity.ok(response);

    }
    @DeleteMapping("/delete/role/{id}")
    public ResponseEntity<?> deleteRolebyId(@PathVariable Long id) {
        roleService.deleteRole(id);
        ApiResponse<Object> response = new ApiResponse<>(HttpStatus.OK.value(),
                null,
                "Delete Role successful with "+id,
                null

        );
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
    @GetMapping("/detail/role/{id}")
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
