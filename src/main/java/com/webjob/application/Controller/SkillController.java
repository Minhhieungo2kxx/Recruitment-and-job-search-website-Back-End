package com.webjob.application.Controller;


import com.webjob.application.Models.Company;
import com.webjob.application.Models.Request.SkillRequest;
import com.webjob.application.Models.Request.Userrequest;
import com.webjob.application.Models.Response.ApiResponse;
import com.webjob.application.Models.Response.MetaDTO;
import com.webjob.application.Models.Response.ResponseDTO;
import com.webjob.application.Models.Response.UserDTO;
import com.webjob.application.Models.Skill;
import com.webjob.application.Models.User;
import com.webjob.application.Services.SkillService;
import jakarta.validation.Valid;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
public class SkillController {
    private final SkillService skillService;
    private final ModelMapper modelMapper;


    public SkillController(SkillService skillService, ModelMapper modelMapper) {
        this.skillService = skillService;
        this.modelMapper = modelMapper;
    }

    @PostMapping("/create/skill")
    public ResponseEntity<?> createSkill(@Valid @RequestBody Skill skill) {
        skillService.checkNameskill(skill.getName());
        Skill save=skillService.handle(skill);
        ApiResponse<?> response = new ApiResponse<>(
                HttpStatus.CREATED.value(),
                null,
                "Create Skill successful",
                save

        );
        return new ResponseEntity<>(response, HttpStatus.CREATED);

    }
    @PutMapping("/edit/skill/{id}")
    public ResponseEntity<?> EditSkill(@PathVariable Long id,@Valid @RequestBody SkillRequest skillRequest) {
        Skill canfind=skillService.getbyID(id).orElseThrow(() -> new IllegalArgumentException("Skill not found with ID: " + id));
        skillService.checkNameskill(skillRequest.getName());
        modelMapper.map(skillRequest,canfind);
        ApiResponse<?> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Edit Skill successful",
                canfind

        );
        return new ResponseEntity<>(response, HttpStatus.OK);

    }
    @GetMapping("/api/skill")
    public ResponseEntity<?> GetallPageList(@RequestParam(value ="page") String pageparam){
        int page=0;
        int size=8;
        try {
            page = Integer.parseInt(pageparam);
            if (page <= 0)
                page = 1;
        } catch (NumberFormatException e) {
            // Nếu người dùng nhập sai, mặc định về trang đầu
            page = 1;
        }
        Page<Skill> pagelist=skillService.getAllPage(page-1,size);
        int currentpage=pagelist.getNumber()+1;
        int pagesize=pagelist.getSize();
        int totalpage=pagelist.getTotalPages();
        Long totalItem=pagelist.getTotalElements();

        MetaDTO metaDTO=new MetaDTO(currentpage,pagesize,totalpage,totalItem);
        ResponseDTO<?> respond=new ResponseDTO<>(metaDTO,pagelist.getContent());
        ApiResponse<?> response=new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "fetch all Skill Successful",
                respond
        );
        return ResponseEntity.ok(response);

    }

}
