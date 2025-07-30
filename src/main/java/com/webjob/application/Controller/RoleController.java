package com.webjob.application.Controller;

import com.webjob.application.Models.Company;
import com.webjob.application.Models.Job;
import com.webjob.application.Models.Request.JobRequest;
import com.webjob.application.Models.Response.ApiResponse;
import com.webjob.application.Models.Response.JobResponse;
import com.webjob.application.Models.Response.MetaDTO;
import com.webjob.application.Models.Response.ResponseDTO;
import com.webjob.application.Models.Role;
import com.webjob.application.Models.Skill;
import com.webjob.application.Services.RoleService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
        Page<Role> pagelist=roleService.getAllPage(page-1,size);
        int currentpage=pagelist.getNumber()+1;
        int pagesize=pagelist.getSize();
        int totalpage=pagelist.getTotalPages();
        Long totalItem=pagelist.getTotalElements();

        MetaDTO metaDTO=new MetaDTO(currentpage,pagesize,totalpage,totalItem);
        List<Role> jobsList=pagelist.getContent();
        ResponseDTO<?> respond=new ResponseDTO<>(metaDTO,jobsList);
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
}
