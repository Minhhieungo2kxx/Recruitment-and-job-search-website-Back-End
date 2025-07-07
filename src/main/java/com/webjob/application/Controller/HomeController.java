package com.webjob.application.Controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController

public class HomeController {

    @GetMapping("/home")
    public String getHome(){
        return "Welcome SpringBoot RestFul API";
    }
}
