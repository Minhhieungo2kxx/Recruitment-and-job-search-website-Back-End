package com.webjob.application.Controller;

import com.webjob.application.Models.Response.ApiResponse;
import com.webjob.application.Services.EmailService;
import com.webjob.application.Services.SubscriberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController

public class HomeController {
    @Value("${spring.mail.username}")
    private String username;


    @Autowired
    private EmailService emailService;
    @Autowired
    private SubscriberService subscriberService;


    @GetMapping("/")
    public String getHome(){
        return "Welcome SpringBoot RestFul API Beginner";
    }




}
