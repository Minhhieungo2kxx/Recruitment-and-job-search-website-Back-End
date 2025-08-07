package com.webjob.application.Controller;

import com.webjob.application.Models.Entity.Subscriber;
import com.webjob.application.Models.Request.SubscriberRequest;
import com.webjob.application.Models.Response.ApiResponse;
import com.webjob.application.Services.SubscriberService;
import com.webjob.application.Services.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/subscribers")
public class SubscriberController {
    @Autowired
    private SubscriberService subscriberService;
    @Autowired
    private UserService userService;


    @PostMapping
    public ResponseEntity<?> createSubcriber(@Valid @RequestBody Subscriber subscriber) {
        Subscriber save=subscriberService.createSubciber(subscriber);
        ApiResponse<?> apiResponse=new ApiResponse<>(HttpStatus.CREATED.value(), null,
                "Tạo Subscriber thành công",
                save);
        return new ResponseEntity<>(apiResponse,HttpStatus.CREATED);

    }
    @PutMapping("/{id}")
    public ResponseEntity<?> editSubcriber(@PathVariable Long id, @RequestBody SubscriberRequest request) {
        Subscriber edit=subscriberService.updateSubciber(request);
        ApiResponse<?> apiResponse=new ApiResponse<>(HttpStatus.CREATED.value(), null,
                "Update Subscriber thành công",
                edit);
        return ResponseEntity.ok(apiResponse);

    }

    @GetMapping("/skills")
    public ResponseEntity<?> GetSkillSubcriber() {
        Subscriber subscriber=subscriberService.getbySkillSub();
        ApiResponse<?> apiResponse=new ApiResponse<>(HttpStatus.OK.value(), null,
                "Get All Skill Subscriber thành công",
                subscriber);
        return ResponseEntity.ok(apiResponse);
    }


    @GetMapping("/send-mails")
    public ResponseEntity<?> SendEmail() {
            subscriberService.sendSubscribersEmailJobs();
            return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), null,
                    "Send Email Subscriber with Test Successful", null));
    }








}
