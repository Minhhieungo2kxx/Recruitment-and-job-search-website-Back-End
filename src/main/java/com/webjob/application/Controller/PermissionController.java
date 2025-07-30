package com.webjob.application.Controller;


import com.webjob.application.Models.Job;
import com.webjob.application.Models.Permission;
import com.webjob.application.Models.Response.ApiResponse;
import com.webjob.application.Models.Response.MetaDTO;
import com.webjob.application.Models.Response.ResponseDTO;
import com.webjob.application.Models.Response.ResumeResponse;
import com.webjob.application.Models.Resume;
import com.webjob.application.Services.PermissionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
public class PermissionController {
    @Autowired
    private PermissionService permissionService;

    @PostMapping("/create/permission")
    public ResponseEntity<?> createPermission(@Valid @RequestBody Permission permission) {
        Permission save=permissionService.savePermission(permission);
        ApiResponse<?> apiResponse=new ApiResponse<>(HttpStatus.CREATED.value(), null,
                "Tạo Permission thành công",
                save);
        return new ResponseEntity<>(apiResponse,HttpStatus.CREATED);

    }
    @PutMapping("/edit/permission/{id}")
    public ResponseEntity<?> createPermission(@PathVariable Long id, @Valid @RequestBody Permission permission) {
        Permission edit=permissionService.editPermission(id,permission);
        ApiResponse<?> apiResponse=new ApiResponse<>(HttpStatus.CREATED.value(), null,
                "Edit Permission thành công",
                edit);
        return new ResponseEntity<>(apiResponse,HttpStatus.CREATED);

    }
    @GetMapping("/api/permissions")
    public ResponseEntity<?> GetallPageList(@RequestParam(value ="page") String pageparam){
        int page=0;
        int size=8;
        try {
            page = Integer.parseInt(pageparam);
            if (page <= 0)
                page = 1;
        } catch (NumberFormatException e) {
            // Nếu người dùng nhập sai, mặc định về trang đầu
            page = 1;
        }
        Page<Permission> pagelist=permissionService.getAllPage(page-1,size);
        int currentpage=pagelist.getNumber()+1;
        int pagesize=pagelist.getSize();
        int totalpage=pagelist.getTotalPages();
        Long totalItem=pagelist.getTotalElements();

        MetaDTO metaDTO=new MetaDTO(currentpage,pagesize,totalpage,totalItem);
        List<Permission> resumessList=pagelist.getContent();
        ResponseDTO<?> respond=new ResponseDTO<>(metaDTO,resumessList);
        ApiResponse<?> response=new ApiResponse<>(HttpStatus.OK.value(), null,
                "Fetch all PerMissions Successful",
                respond
        );
        return ResponseEntity.ok(response);

    }
    @DeleteMapping("/delete/permission/{id}")
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
