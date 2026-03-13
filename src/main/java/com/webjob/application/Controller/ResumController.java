package com.webjob.application.Controller;

import com.webjob.application.Annotation.RateLimit;
import com.webjob.application.Dto.Request.UpdateResumeUser;
import com.webjob.application.Dto.Response.ApiResponse;
import com.webjob.application.Dto.Response.ResponseDTO;
import com.webjob.application.Dto.Response.ResumeResponse;
import com.webjob.application.Model.Enums.ResumeStatus;
import com.webjob.application.Dto.Request.UpdateResumeHR;

import com.webjob.application.Model.Entity.Resume;
import com.webjob.application.Service.ResumService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/resumes")
@RequiredArgsConstructor
public class ResumController {
    private final ResumService resumService;
    private final ModelMapper modelMapper;



    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "TOKEN")
    @PostMapping
    public ResponseEntity<?> createResume(@Valid @RequestBody Resume resume, Authentication authentication) {
        Resume resumeSave=resumService.saveResume(resume,authentication);
        ResumeResponse resumeResponse=modelMapper.map(resumeSave,ResumeResponse.class);
        if(resumeSave.getJob()!=null){
            resumeResponse.setCompanyName(resumeSave.getJob().getCompany().getName());
        }
        ApiResponse<?> apiResponse=new ApiResponse<>(HttpStatus.CREATED.value(), null,
                "Tạo Resume thành công",
                resumeResponse);
        return new ResponseEntity<>(apiResponse,HttpStatus.CREATED);

    }

    @RateLimit(maxRequests = 8, timeWindowSeconds = 60, keyType = "TOKEN")
    @PreAuthorize("hasRole('HR')")
    @PutMapping("/hr/{id}/status")
    public ResponseEntity<?> updateResumeStatusHR(
            @PathVariable Long id, @Valid @RequestBody UpdateResumeHR updateResumeDTO, Authentication authentication) {
        Resume edit=resumService.updateStatusByHR(id,updateResumeDTO.getStatus(),authentication);
        ResumeResponse resumeResponse=modelMapper.map(edit,ResumeResponse.class);
        if(edit.getJob()!=null){
            resumeResponse.setCompanyName(edit.getJob().getCompany().getName());
        }
        ApiResponse<?> apiResponse=new ApiResponse<>(HttpStatus.OK.value(), null,
                "Cập nhật trạng thái Resume ứng viên thành công",
                resumeResponse);
        return ResponseEntity.ok(apiResponse);

    }
    @RateLimit(maxRequests = 8, timeWindowSeconds = 60, keyType = "TOKEN")
    @PreAuthorize("hasRole('USER')")
    @PutMapping("/user/{id}")
    public ResponseEntity<?> updateResumeUser(
            @PathVariable Long id, @Valid @RequestBody UpdateResumeUser updateResumeDTO,Authentication authentication) throws IOException {
        Resume edit=resumService.updateResumeUser(id,updateResumeDTO,authentication);
        ResumeResponse resumeResponse=modelMapper.map(edit,ResumeResponse.class);
        if(edit.getJob()!=null){
            resumeResponse.setCompanyName(edit.getJob().getCompany().getName());
        }
        ApiResponse<?> apiResponse=new ApiResponse<>(HttpStatus.OK.value(), null,
                "Cập nhật Resume thành công",
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
    public ResponseEntity<?> GetallPageList(
            @RequestParam(value ="page") String pageparam,Authentication authentication){
        ResponseDTO<?> respond=resumService.getPaginatedResumes(pageparam,"default",authentication);
        ApiResponse<?> response=new ApiResponse<>(HttpStatus.OK.value(), null,
                "Fetch all Resume Successful",
                respond
        );
        return ResponseEntity.ok(response);

    }

    @RateLimit(maxRequests = 15, timeWindowSeconds = 60, keyType = "TOKEN")
    @GetMapping("/by-user")
    public ResponseEntity<?> GetallResumebyUser(
            @RequestParam(value ="page") String pageparam,Authentication authentication){
        ResponseDTO<?> respond=resumService.getPaginatedResumes(pageparam,"by-user",authentication);
        ApiResponse<?> response=new ApiResponse<>(HttpStatus.OK.value(), null,
                "Fetch all Resumes by User Successful",
                respond
        );
        return ResponseEntity.ok(response);
    }

    @RateLimit(maxRequests = 15, timeWindowSeconds = 60, keyType = "TOKEN")
    @GetMapping("/by-companyHR")
    public ResponseEntity<?> GetallResumeHRcompany(
            @RequestParam(value ="page") String pageparam,Authentication authentication){

        ResponseDTO<?> respond=resumService.getPaginatedResumes(pageparam,"HRfrom-Company",authentication);
        ApiResponse<?> response=new ApiResponse<>(HttpStatus.OK.value(), null,
                "Fetch all Resumes from Company Successful",
                respond
        );
        return ResponseEntity.ok(response);
    }

    @RateLimit(maxRequests = 15, timeWindowSeconds = 60, keyType = "TOKEN")
    @GetMapping("/history-applied")
    public ResponseEntity<?> getResumeHistory(
            @RequestParam(defaultValue = "0") String page,
            @RequestParam(required = false) ResumeStatus status,Authentication authentication){
        ResponseDTO<?> respond=resumService.getUserResumeHistory(page,status,authentication);
        ApiResponse<?> response=ApiResponse.builder().statusCode(HttpStatus.OK.value())
                .error(null)
                .message("Fetch all History Applied Resumes Apply Successful")
                .data(respond)
                .build();
        return ResponseEntity.ok(response);
    }


}
