package com.webjob.application.Controller;

import com.webjob.application.Models.Company;
import com.webjob.application.Models.Request.Userrequest;
import com.webjob.application.Models.Response.ApiResponse;
import com.webjob.application.Models.Response.MetaDTO;
import com.webjob.application.Models.Response.ResponseDTO;
import com.webjob.application.Models.Response.UserDTO;
import com.webjob.application.Models.User;
import com.webjob.application.Services.CompanyService;
import com.webjob.application.Services.UserService;
import jakarta.validation.Valid;
import org.modelmapper.ModelMapper;
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

    private final CompanyService companyService;


    public UserController(UserService userService, ModelMapper modelMapper, CompanyService companyService) {
        this.userService = userService;
        this.modelMapper = modelMapper;
        this.companyService = companyService;
    }

    @PostMapping("/create/user")
    public ResponseEntity<ApiResponse<UserDTO>> create(@Valid @RequestBody Userrequest userrequest) {
        try {
            Company company = companyService.getbyID(userrequest.getCompany().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Company not found with ID: " + userrequest.getCompany().getId()));

            // Tạo user và ánh xạ dữ liệu
            User user = modelMapper.map(userrequest, User.class);
            user.setCompany(company); // Gán lại company sau khi map để không bị ghi đè

            // Xử lý và phản hồi
            User userSaved = userService.handle(user);
            UserDTO userDTO = modelMapper.map(userSaved, UserDTO.class);

            ApiResponse<UserDTO> response = new ApiResponse<>(
                    HttpStatus.CREATED.value(),
                    null,
                    "Create USER successful",
                    userDTO
            );

            return new ResponseEntity<>(response, HttpStatus.CREATED);

        } catch (IllegalArgumentException ex) {

            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ApiResponse<>(HttpStatus.CONFLICT.value(), null, ex.getMessage(), null));
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
            ApiResponse<List<UserDTO>> response = new ApiResponse<>(
                    HttpStatus.CREATED.value(),
                    null,
                    "Get List USER successful",
                    userDTOList
            );
            return ResponseEntity.ok(response);
        }

    }


    @PutMapping("user/edit/{id}")
    public ResponseEntity<?> editUserById(@PathVariable Long id,@Valid @RequestBody Userrequest userrequest) {
        try {
            userService.checkById(id);
            Company company = companyService.getbyID(userrequest.getCompany().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Company not found with ID: " + userrequest.getCompany().getId()));

            User user=userService.getbyID(id).orElseThrow(() -> new IllegalArgumentException("User not found with ID: " +id));
            modelMapper.map(userrequest,user);
            user.setCompany(company);
            User updatedUser = userService.handleUpdate(user);
            UserDTO userDTO=modelMapper.map(updatedUser, UserDTO.class);
            ApiResponse<UserDTO> response = new ApiResponse<>(
                    HttpStatus.CREATED.value(),
                    null,
                    "Edit USER successful",
                    userDTO

            );
            return ResponseEntity.ok(response);

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
            if (canfind.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found with ID: " + id);
            }
            User edit = canfind.get();
            userService.delete(edit);
            ApiResponse<Object> response = new ApiResponse<>(
                    HttpStatus.NO_CONTENT.value(),
                    null,
                    "Delete USER successful",
                    null

            );
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
            User user=userService.getbyID(id).orElseThrow(() -> new IllegalArgumentException("User not found with ID: " +id));
            UserDTO userDTO=modelMapper.map(user,UserDTO.class);
            ApiResponse<?> response = new ApiResponse<>(
                    HttpStatus.CREATED.value(),
                    null,
                    "Detail USER successful",
                    userDTO

            );
            return ResponseEntity.ok(response);

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
        List<User> userList=pagelist.getContent();
        List<UserDTO> userDTOList=new ArrayList<>();
        for (User user:userList){
            UserDTO userDTO=modelMapper.map(user,UserDTO.class);
            userDTOList.add(userDTO);
        }
        ResponseDTO<?> respond=new ResponseDTO<>(metaDTO,userDTOList);
        ApiResponse<?> response=new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "fetch all user",
                respond
        );
        return ResponseEntity.ok(response);

    }



}


//|         Cách viết                              | Khi nào dùng                                         |           | Ưu điểm                |
//        | -------------------------------------- | --------------------------------------------------- | --------------------------------- |
//        | `new ResponseEntity<>(body, status)`   | Khi bạn **cần chỉ định rõ ràng** status hoặc header | Linh hoạt, chính xác              |
//        | `ResponseEntity.ok(body)`              | Khi bạn trả về **HTTP 200 OK** đơn giản             | Gọn gàng, dễ đọc, phổ biến nhất   |
//        | `ResponseEntity.status(...).body(...)` | Khi bạn cần **status tùy chỉnh** nhưng vẫn rõ ràng  | Cân bằng giữa rõ ràng và ngắn gọn |
//
