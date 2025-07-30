package com.webjob.application.Services;

import com.webjob.application.Models.Permission;
import com.webjob.application.Models.Resume;
import com.webjob.application.Models.Role;
import com.webjob.application.Repository.PermissionRepository;
import com.webjob.application.Repository.RoleRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class PermissionService {

    @Autowired
    private PermissionRepository permissionRepository;
    @Autowired
    private RoleRepository roleRepository;



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
    @Transactional
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


}
