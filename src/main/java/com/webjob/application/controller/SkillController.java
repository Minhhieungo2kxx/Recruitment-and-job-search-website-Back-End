package com.webjob.application.controller;


import com.webjob.application.annotation.RateLimit;
import com.webjob.application.dto.Request.SkillRequest;
import com.webjob.application.dto.Response.ApiResponse;
import com.webjob.application.dto.Response.ResponseDTO;
import com.webjob.application.models.Entity.Skill;
import com.webjob.application.service.SkillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/skills")
@RequiredArgsConstructor
public class SkillController {
    private final SkillService skillService;



    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "TOKEN")
    @PostMapping
    public ResponseEntity<?> createSkill(@Valid @RequestBody Skill skill) {
        return skillService.create_Skill(skill);

    }

    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "TOKEN")
    @PutMapping("/{id}")
    public ResponseEntity<?> EditSkill(@PathVariable Long id,@Valid @RequestBody SkillRequest skillRequest) {
       return skillService.Edit_Skill(id,skillRequest);

    }

    @RateLimit(maxRequests = 15, timeWindowSeconds = 60, keyType = "IP")
    @GetMapping
    public ResponseEntity<?> Get_allPageList(@RequestParam(value ="page") String pageparam){
        ResponseDTO<?> respond=skillService.getAllPageList(pageparam,"default");
        ApiResponse<?> response=new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "fetch all Skill Successful",
                respond
        );
        return ResponseEntity.ok(response);

    }

    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "TOKEN")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSkill(@PathVariable Long id) {
        skillService.deleteSkill(id);
        ApiResponse<?> response=new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Delete Skill Successful",
                null
        );
        return ResponseEntity.ok(response);
    }

}
