package com.webjob.application.Service;


import com.webjob.application.Model.Entity.Permission;
import com.webjob.application.Model.Entity.Role;
import com.webjob.application.Dto.Response.MetaDTO;
import com.webjob.application.Dto.Response.ResponseDTO;
import com.webjob.application.Repository.PermissionRepository;
import com.webjob.application.Repository.RoleRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RoleService {
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PermissionRepository permissionRepository;



    @Transactional
    public Role createRole(Role role){
        boolean exists = roleRepository.existsByName(role.getName());
        if(exists){
            throw new IllegalArgumentException("Role đã tồn tại với name: " + role.getName());
        }

        List<Permission> validPermissions =getValidPerMissions(role.getPermissions());
        // Nếu validPermissions là rỗng thì gán giá trị [] cho role.setPermissions()
        if (validPermissions.isEmpty()) {
            role.setPermissions(new ArrayList<>()); // Đặt một danh sách trống
        } else {
            role.setPermissions(validPermissions); // Nếu có quyền hợp lệ thì sử dụng danh sách đó
        }

        return roleRepository.save(role);
    }
    @Transactional
    public Role EditRole(Long id, Role role){
        Role update=getByid(id).orElseThrow(()-> new IllegalArgumentException("Role not found with "+id));
//        boolean exists = roleRepository.existsByName(role.getName());
//        if(exists){
//            throw new IllegalArgumentException("Role đã tồn tại với name: " + role.getName());
//        }

        List<Permission> validPermissions =getValidPerMissions(role.getPermissions());
        update.setName(role.getName());
        update.setDescription(role.getDescription());
        update.setActive(role.isActive());

        // Nếu validPermissions là rỗng thì gán giá trị [] cho role.setPermissions()
        if (validPermissions.isEmpty()) {
            update.setPermissions(new ArrayList<>()); // Đặt một danh sách trống
        } else {
            update.setPermissions(validPermissions); // Nếu có quyền hợp lệ thì sử dụng danh sách đó
        }
        return roleRepository.save(update);
    }
    public Page<Role> getAllPage(int page, int size){
//        Sort.Direction direction=Sort.Direction.ASC;
//        Sort sort=Sort.by(direction,"name");
        Pageable pageable= PageRequest.of(page,size);
        return roleRepository.findAll(pageable);
    }




    private List<Permission> getValidPerMissions(List<Permission> permissionList) {
        List<Long> ids = permissionList.stream()
                .map(Permission::getId)
                .collect(Collectors.toList());
        return permissionRepository.findByIdIn(ids);
    }
    public Optional<Role> getByid(Long id){
       return roleRepository.findById(id);
    }
    @Transactional
    public void deleteRole(Long id){
        Role role=getByid(id).orElseThrow(()->new IllegalArgumentException("Role not found with "+id));
        role.getPermissions().clear();
        roleRepository.delete(role);
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
        Page<Role> pagelist=getAllPage(page-1,size);
        int currentpage=pagelist.getNumber()+1;
        int pagesize=pagelist.getSize();
        int totalpage=pagelist.getTotalPages();
        Long totalItem=pagelist.getTotalElements();

        MetaDTO metaDTO=new MetaDTO(currentpage,pagesize,totalpage,totalItem);
        List<Role> jobsList=pagelist.getContent();
        ResponseDTO<?> respond=new ResponseDTO<>(metaDTO,jobsList);
        return respond;
    }


}
