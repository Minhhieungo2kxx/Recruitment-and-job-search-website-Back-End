package com.webjob.application.service;

import com.webjob.application.models.Entity.Permission;
import com.webjob.application.dto.Response.MetaDTO;
import com.webjob.application.dto.Response.ResponseDTO;
import com.webjob.application.repository.PermissionRepository;
import com.webjob.application.repository.RolePermissionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PermissionService {


    private final PermissionRepository permissionRepository;

    private final RolePermissionRepository rolePermissionRepository;




    @Transactional
    public Permission savePermission(Permission permission){
        boolean exist= permissionRepository.existsByApiPathAndMethodAndModule(
                permission.getApiPath(), permission.getMethod(), permission.getModule()
        );
        if(exist){
            throw new IllegalArgumentException("Permission already exists.");
        }
        return permissionRepository.save(permission);

    }
    @Transactional

    public Permission editPermission(Long id,Permission permission){
        Permission edit=getByID(id);

        edit.setName(permission.getName());
        edit.setApiPath(permission.getApiPath());
        edit.setMethod(permission.getMethod());
        edit.setModule(permission.getModule());
        edit.setCode(permission.getCode());
        return permissionRepository.save(edit);
    }



    public Permission getByID(Long id){
        Permission permissionID=permissionRepository.findById(id)
                .orElseThrow(()-> new IllegalArgumentException("Permission not found with "+id));
        return permissionID;
    }

    @Transactional
    public void deletePerMission(Long id){
        Permission permission=getByID(id);

        rolePermissionRepository.deleteByPermissionId(permission.getId());
        permissionRepository.delete(permission);

    }
    public Page<Permission> getAllPage(int page, int size){
        Sort.Direction direction=Sort.Direction.ASC;
        Sort sort=Sort.by(direction,"id");
        Pageable pageable= PageRequest.of(page,size,sort);
        return permissionRepository.findAll(pageable);
    }
    public ResponseDTO<List<Permission>> getPaginated(int page,int size) {
        size = Math.min(Math.max(size, 1), 50);
        page = Math.max(page, 1);
        Page<Permission> pagelist=getAllPage(page-1,size);
        int currentpage=pagelist.getNumber()+1;
        int pagesize=pagelist.getSize();
        int totalpage=pagelist.getTotalPages();
        Long totalItem=pagelist.getTotalElements();

        MetaDTO metaDTO=new MetaDTO(currentpage,pagesize,totalpage,totalItem);
        List<Permission> resumessList=pagelist.getContent();
        ResponseDTO<List<Permission>> respond=new ResponseDTO<>(metaDTO,resumessList);
        return respond;
    }

    @Transactional
    public Permission createPermission (Permission permission){
        return savePermission(permission);
    }




}
