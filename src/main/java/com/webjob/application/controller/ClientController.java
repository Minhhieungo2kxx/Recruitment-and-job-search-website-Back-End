package com.webjob.application.controller;


import com.webjob.application.annotation.RateLimit;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
public class ClientController {

    @RateLimit(maxRequests = 20, timeWindowSeconds = 60, keyType = "IP")
    @GetMapping("/")
    public String getHome() {
        return "home/index";
    }

    @RateLimit(maxRequests = 10, timeWindowSeconds = 60, keyType = "IP")
    @GetMapping("/login-chat")
    public String getLogin() {
        return "clients/loginchat";
    }

    @RateLimit(maxRequests = 10, timeWindowSeconds = 60, keyType = "IP")
    @GetMapping("/chat")
    public String getChat() {
        return "clients/chatconversation";
    }

    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "IP")
    @GetMapping("/login-success")
    public String showLoginSuccessPage() {
        return "clients/login-success"; // Trả về trang xử lý thành công OAuth2
    }


    @GetMapping("/reset-password")
    public String showResetPasswordForm() {
        return "clients/reset-password"; // Trả về trang hiển thị form đặt lại mật khẩu
    }





}
