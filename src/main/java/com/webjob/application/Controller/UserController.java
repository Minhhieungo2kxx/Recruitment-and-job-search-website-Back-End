package com.webjob.application.Controller;

import com.webjob.application.Models.Dto.MetaDTO;
import com.webjob.application.Models.Dto.ResponsepageDTO;
import com.webjob.application.Models.Dto.UserDTO;
import com.webjob.application.Models.User;
import com.webjob.application.Services.UserService;
import jakarta.validation.Valid;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
public class UserController {

    private final UserService userService;
    private final ModelMapper modelMapper;


    public UserController(UserService userService, ModelMapper modelMapper) {
        this.userService = userService;
        this.modelMapper = modelMapper;
    }

    @PostMapping("/create/user")
    public ResponseEntity<?> create(@Valid @RequestBody User user) {
        try {
            User save_user = userService.handle(user);
            UserDTO userDTO=modelMapper.map(save_user, UserDTO.class);
            return new ResponseEntity<>(userDTO, HttpStatus.CREATED);

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
        }

    }


    @GetMapping("/all/user")
    public ResponseEntity<?> getAllUser() {
        List<User> list = userService.getAll();
        List<UserDTO> userDTOList=new ArrayList<>();
        if (list.isEmpty()) {
            return ResponseEntity.noContent().build();

        }
        else {
            for(User user:list){
                UserDTO userDTO=modelMapper.map(user,UserDTO.class);
                userDTOList.add(userDTO);
            }
            return ResponseEntity.ok(userDTOList);
        }

    }


    @PutMapping("user/edit/{id}")
    public ResponseEntity<?> editUserById(@PathVariable Long id, @Valid @RequestBody User user) {
        try {
            userService.checkById(id);
            Optional<User> canFind = userService.getbyID(id);
            if (canFind.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found with ID: " + id);
            }
            User existingUser = canFind.get();
            modelMapper.typeMap(User.class, User.class)
                    .addMappings(mapper -> mapper.skip(User::setId));

            modelMapper.map(user, existingUser);
            User updatedUser = userService.handleUpdate(existingUser);
            return ResponseEntity.ok(modelMapper.map(updatedUser, UserDTO.class));

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected error: " + ex.getMessage());
        }
    }

    @DeleteMapping("user/delete/{id}")
    public ResponseEntity<?> deleteUserbyId(@PathVariable Long id) {
        try {
            userService.checkById(id);
            Optional<User> canfind = userService.getbyID(id);
            User edit = canfind.get();
            userService.delete(edit);
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();

        } catch (
                IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        }
    }

    @GetMapping("user/detail/{id}")
    public ResponseEntity<?> getUserbyID(@PathVariable Long id) {
        try {
            userService.checkById(id);
            Optional<User> findId = userService.getbyID(id);
            User userid = findId.get();
            return ResponseEntity.ok(userid);

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());

        }


    }
    @GetMapping("/api/users")
    public ResponseEntity<?> GetallPageList(@RequestParam(value ="page") String pageparam){
        int page=0;
        int size=3;
        try {
            page = Integer.parseInt(pageparam);
            if (page <= 0)
                page = 1;
        } catch (NumberFormatException e) {
            // Nếu người dùng nhập sai, mặc định về trang đầu
            page = 1;
        }
        Page<User> pagelist=userService.getAllPage(page-1,size);
        int currentpage=pagelist.getNumber()+1;
        int pagesize=pagelist.getSize();
        int totalpage=pagelist.getTotalPages();
        Long totalItem=pagelist.getTotalElements();

        MetaDTO metaDTO=new MetaDTO(currentpage,pagesize,totalpage,totalItem);
        ResponsepageDTO<User> respon=new ResponsepageDTO<>(metaDTO,pagelist.getContent());
        return ResponseEntity.ok(respon);

    }



}


//|         Cách viết                              | Khi nào dùng                                         |           | Ưu điểm                |
//        | -------------------------------------- | --------------------------------------------------- | --------------------------------- |
//        | `new ResponseEntity<>(body, status)`   | Khi bạn **cần chỉ định rõ ràng** status hoặc header | Linh hoạt, chính xác              |
//        | `ResponseEntity.ok(body)`              | Khi bạn trả về **HTTP 200 OK** đơn giản             | Gọn gàng, dễ đọc, phổ biến nhất   |
//        | `ResponseEntity.status(...).body(...)` | Khi bạn cần **status tùy chỉnh** nhưng vẫn rõ ràng  | Cân bằng giữa rõ ràng và ngắn gọn |
//
