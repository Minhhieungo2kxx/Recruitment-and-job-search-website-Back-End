package com.webjob.application.Controller;

import com.webjob.application.Annotation.RateLimit;
import com.webjob.application.Dto.Request.ChangePasswordRequest;
import com.webjob.application.Dto.Request.UserSetting;
import com.webjob.application.Dto.Request.Userrequest;
import com.webjob.application.Dto.Response.ApiResponse;
import com.webjob.application.Dto.Response.ResponseDTO;
import com.webjob.application.Dto.Response.UserDTO;
import com.webjob.application.Model.Entity.User;
import com.webjob.application.Service.CompanyService;
import com.webjob.application.Service.RoleService;
import com.webjob.application.Service.Socket.PresenceService;
import com.webjob.application.Service.UserService;
import jakarta.validation.Valid;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final ModelMapper modelMapper;

    private final CompanyService companyService;

    private final PresenceService presenceService;

    public UserController(UserService userService, ModelMapper modelMapper, CompanyService companyService, RoleService roleService, PresenceService presenceService) {
        this.userService = userService;
        this.modelMapper = modelMapper;
        this.companyService = companyService;

        this.presenceService = presenceService;
    }

    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "IP")
    @PostMapping
    public ResponseEntity<ApiResponse<UserDTO>> create(@Valid @RequestBody Userrequest userrequest) {

            // Tạo user và ánh xạ dữ liệu
            User user = modelMapper.map(userrequest, User.class);
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

        }

    @RateLimit(maxRequests = 10, timeWindowSeconds = 60, keyType = "TOKEN")
    @PutMapping("/{id}")
    public ResponseEntity<?> editUserById(@PathVariable Long id,@Valid @RequestBody Userrequest userrequest) {
            User user=userService.getbyID(id).orElseThrow(() -> new IllegalArgumentException("User not found with ID: " +id));
            Instant instant=user.getCreatedAt();
            modelMapper.map(userrequest,user);
            user.setCreatedAt(instant);
            User updatedUser = userService.handleUpdate(user);
            UserDTO userDTO=modelMapper.map(updatedUser, UserDTO.class);
            ApiResponse<UserDTO> response = new ApiResponse<>(
                    HttpStatus.OK.value(),
                    null,
                    "Edit USER successful",
                    userDTO

            );
            return ResponseEntity.ok(response);
    }

    @RateLimit(maxRequests = 5, timeWindowSeconds = 300, keyType = "TOKEN")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUserbyId(@PathVariable Long id) {
            userService.checkById(id);
            Optional<User> canfind = userService.getbyID(id);
            if (canfind.isEmpty()) {
                throw new UsernameNotFoundException("User not found with id: " + id);
            }
            User edit = canfind.get();
            userService.deleteUser(edit);
            ApiResponse<Object> response = new ApiResponse<>(
                    HttpStatus.NO_CONTENT.value(),
                    null,
                    "Delete USER successful",
                    null

            );
            return ResponseEntity.ok(response);

    }


    @RateLimit(maxRequests = 30, timeWindowSeconds = 60, keyType = "IP")
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserbyID(@PathVariable Long id) {
        try {
            userService.checkById(id);
            User user=userService.getbyID(id).orElseThrow(() -> new IllegalArgumentException("User not found with ID: " +id));
            UserDTO userDTO=modelMapper.map(user,UserDTO.class);
            ApiResponse<?> response = new ApiResponse<>(
                    HttpStatus.OK.value(),
                    null,
                    "Detail USER successful",
                    userDTO

            );
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());

        }


    }

    @RateLimit(maxRequests = 20, timeWindowSeconds = 60, keyType = "IP")
    @GetMapping
    public ResponseEntity<?> GetallPageList(@RequestParam(value ="page") String pageparam){
        ResponseDTO<?> respond=userService.getPaginatedResumes(pageparam,"default");
        ApiResponse<?> response=new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "fetch all user",
                respond
        );
        return ResponseEntity.ok(response);

    }

    @RateLimit(maxRequests = 10, timeWindowSeconds = 60, keyType = "TOKEN")
    @PutMapping("/setting")
    public ResponseEntity<?> SettingUser(@Valid @RequestBody UserSetting userSetting) {
        // Lấy thông tin người dùng hiện tại từ context
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.getbyEmail(userEmail);
        Instant instant=user.getCreatedAt();
        modelMapper.map(userSetting,user);
        user.setCreatedAt(instant);
        User updatedUser = userService.handleUpdate(user);
        UserSetting setting=modelMapper.map(updatedUser,UserSetting.class);
        ApiResponse<UserSetting> response = new ApiResponse<>(HttpStatus.OK.value(), null,
                "Setting USER successful",
                setting
        );
        return ResponseEntity.ok(response);
    }

    @RateLimit(maxRequests = 3, timeWindowSeconds = 300, keyType = "TOKEN")
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request);
        ApiResponse<UserSetting> response = new ApiResponse<>(HttpStatus.OK.value(),
                null,
                "Thay doi mat khau successful",
                null
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
