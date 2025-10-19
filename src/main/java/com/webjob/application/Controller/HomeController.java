package com.webjob.application.Controller;

import com.webjob.application.Annotation.RateLimit;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;


@Controller
public class HomeController {

    @RateLimit(maxRequests = 20, timeWindowSeconds = 60, keyType = "IP")
    @GetMapping("/")
    @ResponseBody
    public String getHome() {
        return "Welcome SpringBoot RestFul API Beginner";
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


}
