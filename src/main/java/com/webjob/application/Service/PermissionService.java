package com.webjob.application.Service;

import com.webjob.application.Model.Entity.Permission;
import com.webjob.application.Dto.Response.MetaDTO;
import com.webjob.application.Dto.Response.ResponseDTO;
import com.webjob.application.Model.Entity.Role;
import com.webjob.application.Repository.PermissionRepository;
import com.webjob.application.Repository.RoleRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

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


}
