package com.webjob.application.controller;

import com.webjob.application.annotation.RateLimit;
import com.webjob.application.dto.Request.*;
import com.webjob.application.dto.Response.ApiResponse;
import com.webjob.application.dto.Response.ResponseDTO;
import com.webjob.application.dto.Response.UserDTO;
import com.webjob.application.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;


    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "TOKEN")
    @PostMapping
    public ResponseEntity<ApiResponse<UserDTO>> create(@Valid @RequestBody Userrequest userrequest) {

        ApiResponse<UserDTO> response = new ApiResponse<>(
                HttpStatus.CREATED.value(),
                null,
                "Create USER successful",
                userService.createUser(userrequest)
        );
        return new ResponseEntity<>(response, HttpStatus.CREATED);

    }

    @RateLimit(maxRequests = 10, timeWindowSeconds = 60, keyType = "TOKEN")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDTO>> editUserById(@PathVariable Long id, @Valid @RequestBody UserRequestUpdate userrequest) {

        ApiResponse<UserDTO> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Edit USER successful",
                userService.updateUser(id, userrequest)

        );
        return ResponseEntity.ok(response);
    }

    @RateLimit(maxRequests = 5, timeWindowSeconds = 300, keyType = "TOKEN")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> deleteUserbyId(@PathVariable Long id) {
        userService.deleteUserById(id);
        ApiResponse<Object> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Delete USER successful",
                null
        );
        return ResponseEntity.ok(response);

    }


    @RateLimit(maxRequests = 30, timeWindowSeconds = 60, keyType = "IP")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDTO>> getUserbyID(@PathVariable Long id) {

        ApiResponse<UserDTO> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Detail USER successful",
                userService.getUserByID(id)

        );
        return ResponseEntity.ok(response);

    }


    @RateLimit(maxRequests = 20, timeWindowSeconds = 60, keyType = "IP")
    @GetMapping
    public ResponseEntity<ApiResponse<ResponseDTO<List<UserDTO>>>> GetallPageList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "8") int size) {
        ApiResponse<ResponseDTO<List<UserDTO>>> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "fetch all user",
                userService.getAllUser(page,size)
        );
        return ResponseEntity.ok(response);

    }

    @RateLimit(maxRequests = 10, timeWindowSeconds = 60, keyType = "TOKEN")
    @PutMapping("/setting/{id}")
    public ResponseEntity<ApiResponse<UserSetting>> SettingUser(@PathVariable Long id, @RequestBody UserSetting userSetting) {
        ApiResponse<UserSetting> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Setting USER successful",
                userService.settingUser(id, userSetting)
        );
        return ResponseEntity.ok(response);
    }

    @RateLimit(maxRequests = 3, timeWindowSeconds = 300, keyType = "TOKEN")
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<UserSetting>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest httprequest,
            Authentication authentication) {
        userService.changePassword(request, httprequest, authentication);
        ApiResponse<UserSetting> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Thay doi mat khau successful,Vui long dang nhap lai",
                null

        );
        return ResponseEntity.ok(response);

    }

    @PutMapping("/{id}/password")
    public ResponseEntity<ApiResponse<Object>> updatePassword(
            @PathVariable Long id,
            @Valid @RequestBody PasswordRequest request) {

        userService.updatePassword(id, request);
        ApiResponse<Object> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Đổi mật khẩu thành công",
                null
        );
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    @RateLimit(maxRequests = 10, timeWindowSeconds = 60, keyType = "TOKEN")
    @PutMapping("/{id}/restore")
    public ResponseEntity<ApiResponse<UserDTO>> restoreUser(@PathVariable Long id) {

        ApiResponse<UserDTO> apiResponse = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Restore User thành công",
                userService.restoreUser(id));
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }


}


