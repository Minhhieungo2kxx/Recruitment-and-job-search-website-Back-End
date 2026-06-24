package com.webjob.application.service;

import com.webjob.application.dto.Response.ApiResponse;
import com.webjob.application.models.Entity.Permission;
import com.webjob.application.dto.Response.MetaDTO;
import com.webjob.application.dto.Response.ResponseDTO;
import com.webjob.application.models.Entity.Role;
import com.webjob.application.repository.PermissionRepository;
import com.webjob.application.repository.RoleRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PermissionService {


    private final PermissionRepository permissionRepository;

    private final RoleRepository roleRepository;




    public Permission savePermission(Permission permission){
        boolean exist= permissionRepository.existsByApiPathAndMethodAndModule(
                permission.getApiPath(), permission.getMethod(), permission.getModule()
        );
        if(exist){
            throw new IllegalArgumentException("Permission already exists.");
        }
        return permissionRepository.save(permission);

    }

    public Permission editPermission(Long id,Permission permission){
        Permission edit=getByID(id);
        boolean exist= permissionRepository.existsByApiPathAndMethodAndModule(
                permission.getApiPath(), permission.getMethod(), permission.getModule()
        );
        if(exist){
            throw new IllegalArgumentException("Permission already exists.");
        }
        edit.setName(permission.getName());
        edit.setApiPath(permission.getApiPath());
        edit.setMethod(permission.getMethod());
        edit.setModule(permission.getModule());
        return permissionRepository.save(edit);
    }



    public Permission getByID(Long id){
        Permission permissionID=permissionRepository.findById(id)
                .orElseThrow(()-> new IllegalArgumentException("Permission not found with "+id));
        return permissionID;
    }

    public void deletePerMission(Long id){
        Permission permission=getByID(id);

        for(Role role: permission.getRoles()){
            role.getPermissions().remove(permission);
            roleRepository.save(role);
        }
        permissionRepository.delete(permission);

    }
    public Page<Permission> getAllPage(int page, int size){
        Sort.Direction direction=Sort.Direction.ASC;
        Sort sort=Sort.by(direction,"name");
        Pageable pageable= PageRequest.of(page,size,sort);
        return permissionRepository.findAll(pageable);
    }
    public ResponseDTO<?> getPaginated(String pageparam, String type) {
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
        Page<Permission> pagelist=getAllPage(page-1,size);
        int currentpage=pagelist.getNumber()+1;
        int pagesize=pagelist.getSize();
        int totalpage=pagelist.getTotalPages();
        Long totalItem=pagelist.getTotalElements();

        MetaDTO metaDTO=new MetaDTO(currentpage,pagesize,totalpage,totalItem);
        List<Permission> resumessList=pagelist.getContent();
        ResponseDTO<?> respond=new ResponseDTO<>(metaDTO,resumessList);
        return respond;
    }
    @Transactional
    public ResponseEntity<?> create_Permission( Permission permission) {
        Permission save=savePermission(permission);
        ApiResponse<?> apiResponse=new ApiResponse<>(HttpStatus.CREATED.value(), null,
                "Tạo Permission thành công",
                save);
        return new ResponseEntity<>(apiResponse,HttpStatus.CREATED);

    }
    @Transactional
    public ResponseEntity<?> edit_Permission( Long id,Permission permission) {
        Permission edit=editPermission(id,permission);
        ApiResponse<?> apiResponse=new ApiResponse<>(HttpStatus.CREATED.value(), null,
                "Edit Permission thành công",
                edit);
        return new ResponseEntity<>(apiResponse,HttpStatus.CREATED);

    }
    public ResponseEntity<?> all_PageList( String pageparam){
        ResponseDTO<?> respond=getPaginated(pageparam,"default");
        ApiResponse<?> response=new ApiResponse<>(HttpStatus.OK.value(), null,
                "Fetch all PerMissions Successful",
                respond
        );
        return ResponseEntity.ok(response);

    }
    @Transactional
    public ResponseEntity<?> delete_PermissionbyId(@PathVariable Long id) {
        Permission permission=getByID(id);
        deletePerMission(id);
        ApiResponse<Object> response = new ApiResponse<>(HttpStatus.OK.value(),
                null,
                "Delete Permission successful with "+id,
                null

        );
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }


}
