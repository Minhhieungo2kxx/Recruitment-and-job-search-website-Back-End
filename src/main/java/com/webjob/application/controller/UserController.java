package com.webjob.application.controller;

import com.webjob.application.annotation.RateLimit;
import com.webjob.application.dto.Request.ChangePasswordRequest;
import com.webjob.application.dto.Request.UserSetting;
import com.webjob.application.dto.Request.Userrequest;
import com.webjob.application.dto.Response.ApiResponse;
import com.webjob.application.dto.Response.ResponseDTO;
import com.webjob.application.dto.Response.UserDTO;
import com.webjob.application.models.Entity.User;
import com.webjob.application.service.CompanyService;
import com.webjob.application.service.Socket.PresenceService;
import com.webjob.application.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;


    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "IP")
    @PostMapping
    public ResponseEntity<ApiResponse<UserDTO>> create(@Valid @RequestBody Userrequest userrequest) {
        return userService.create_User(userrequest);

    }

    @RateLimit(maxRequests = 10, timeWindowSeconds = 60, keyType = "TOKEN")
    @PutMapping("/{id}")
    public ResponseEntity<?> editUserById(@PathVariable Long id, @Valid @RequestBody Userrequest userrequest) {
        return userService.edit_UserById(id,userrequest);
    }

    @RateLimit(maxRequests = 5, timeWindowSeconds = 300, keyType = "TOKEN")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUserbyId(@PathVariable Long id) {
        return userService.delete_UserbyId(id);

    }


    @RateLimit(maxRequests = 30, timeWindowSeconds = 60, keyType = "IP")
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserbyID(@PathVariable Long id) {

        return userService.get_UserbyID(id);

    }


    @RateLimit(maxRequests = 20, timeWindowSeconds = 60, keyType = "IP")
    @GetMapping
    public ResponseEntity<?> GetallPageList(@RequestParam(value = "page") String pageparam) {
        return userService.Get_allPageList(pageparam);

    }

    @RateLimit(maxRequests = 10, timeWindowSeconds = 60, keyType = "TOKEN")
    @PutMapping("/setting")
    public ResponseEntity<?> SettingUser(@Valid @RequestBody UserSetting userSetting, Authentication authentication) {

        return userService.Setting_User(userSetting,authentication);
    }

    @RateLimit(maxRequests = 3, timeWindowSeconds = 300, keyType = "TOKEN")
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @Valid @RequestBody ChangePasswordRequest request, HttpServletRequest httprequest, Authentication authentication) {
        return userService.change_forPassword(request,httprequest,authentication);
    }


}


