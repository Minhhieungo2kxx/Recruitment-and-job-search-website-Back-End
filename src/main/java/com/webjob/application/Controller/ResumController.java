package com.webjob.application.Controller;

import com.webjob.application.Annotation.RateLimit;
import com.webjob.application.Models.Request.UpdateResumeDTO;
import com.webjob.application.Models.Response.*;
import com.webjob.application.Models.Entity.Resume;
import com.webjob.application.Services.ResumService;
import jakarta.validation.Valid;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/resumes")
public class ResumController {
    private final ResumService resumService;
    private final ModelMapper modelMapper;


    public ResumController(ResumService resumService, ModelMapper modelMapper) {
        this.resumService = resumService;
        this.modelMapper = modelMapper;
    }

    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "TOKEN")
    @PostMapping
    public ResponseEntity<?> createResume(@Valid @RequestBody Resume resume) {
        Resume resumeSave=resumService.saveResume(resume);
        ResumeResponse resumeResponse=modelMapper.map(resumeSave,ResumeResponse.class);
        if(resumeSave.getJob()!=null){
            resumeResponse.setCompanyName(resumeSave.getJob().getCompany().getName());
        }
        ApiResponse<?> apiResponse=new ApiResponse<>(HttpStatus.CREATED.value(), null,
                "Tạo Resume thành công",
                resumeResponse);
        return new ResponseEntity<>(apiResponse,HttpStatus.CREATED);

    }

    @RateLimit(maxRequests = 7, timeWindowSeconds = 60, keyType = "TOKEN")
    @PutMapping("/{id}")
    public ResponseEntity<?> editResume(@PathVariable Long id, @Valid @RequestBody UpdateResumeDTO dto) {
        Resume edit=resumService.editResume(id,dto);
        ResumeResponse resumeResponse=modelMapper.map(edit,ResumeResponse.class);
        if(edit.getJob()!=null){
            resumeResponse.setCompanyName(edit.getJob().getCompany().getName());
        }
        ApiResponse<?> apiResponse=new ApiResponse<>(HttpStatus.OK.value(), null,
                "Update Resume thành công",
                resumeResponse);
        return ResponseEntity.ok(apiResponse);

    }

    @RateLimit(maxRequests = 7, timeWindowSeconds = 60, keyType = "TOKEN")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteResumebyId(@PathVariable Long id) {
        Resume resume=resumService.getById(id);
        resumService.deleteResume(resume);
        ApiResponse<?> response = new ApiResponse<>(HttpStatus.OK.value(),
                null,
                "Delete Resume successful with "+id,
                null

        );
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @RateLimit(maxRequests = 15, timeWindowSeconds = 60, keyType = "TOKEN")
    @GetMapping("/{id}")
    public ResponseEntity<?> detailResumebyId(@PathVariable Long id) {
        Resume resume=resumService.getById(id);
        ResumeResponse resumeResponse=modelMapper.map(resume,ResumeResponse.class);
        if(resume.getJob()!=null){
            resumeResponse.setCompanyName(resume.getJob().getCompany().getName());
        }
        ApiResponse<?> response = new ApiResponse<>(HttpStatus.OK.value(),
                null,
                "Detail Resume successful with "+id,
                resumeResponse

        );
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @RateLimit(maxRequests = 15, timeWindowSeconds = 60, keyType = "TOKEN")
    @GetMapping
    public ResponseEntity<?> GetallPageList(@RequestParam(value ="page") String pageparam){

        ResponseDTO<?> respond=resumService.getPaginatedResumes(pageparam,"default");
        ApiResponse<?> response=new ApiResponse<>(HttpStatus.OK.value(), null,
                "Fetch all Resume Successful",
                respond
        );
        return ResponseEntity.ok(response);

    }

    @RateLimit(maxRequests = 15, timeWindowSeconds = 60, keyType = "TOKEN")
    @GetMapping("/by-user")
    public ResponseEntity<?> GetallResumebyUser(@RequestParam(value ="page") String pageparam){
        ResponseDTO<?> respond=resumService.getPaginatedResumes(pageparam,"by-user");
        ApiResponse<?> response=new ApiResponse<>(HttpStatus.OK.value(), null,
                "Fetch all Resumes by User Successful",
                respond
        );
        return ResponseEntity.ok(response);
    }

    @RateLimit(maxRequests = 15, timeWindowSeconds = 60, keyType = "TOKEN")
    @GetMapping("/by-companyHR")
    public ResponseEntity<?> GetallResumeHRcompany(@RequestParam(value ="page") String pageparam){

        ResponseDTO<?> respond=resumService.getPaginatedResumes(pageparam,"HRfrom-Company");
        ApiResponse<?> response=new ApiResponse<>(HttpStatus.OK.value(), null,
                "Fetch all Resumes from Company Successful",
                respond
        );
        return ResponseEntity.ok(response);
    }


}
