package com.webjob.application.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;


@Controller
public class HomeController {
    @GetMapping("/")
    @ResponseBody
    public String getHome() {
        return "Welcome SpringBoot RestFul API Beginner";
    }

    @GetMapping("/login-chat")
    public String getLogin() {
        return "clients/loginchat";
    }

    @GetMapping("/chat")
    public String getChat() {
        return "clients/chatconversation";
    }

    @GetMapping("/login-success")
    public String showLoginSuccessPage() {
        return "clients/login-success"; // Trả về trang xử lý thành công OAuth2
    }


}
