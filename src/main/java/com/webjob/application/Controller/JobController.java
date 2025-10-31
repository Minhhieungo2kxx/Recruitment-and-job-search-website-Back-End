package com.webjob.application.Controller;

import com.webjob.application.Annotation.RateLimit;
import com.webjob.application.Dto.Response.ApiResponse;
import com.webjob.application.Dto.Response.JobApplicantInfoResponse;
import com.webjob.application.Dto.Response.JobResponse;
import com.webjob.application.Dto.Response.ResponseDTO;
import com.webjob.application.Model.Entity.Company;
import com.webjob.application.Model.Entity.Job;
import com.webjob.application.Model.Entity.User;
import com.webjob.application.Dto.Request.JobRequest;
import com.webjob.application.Dto.Request.Search.JobFiltersearch;

import com.webjob.application.Model.Entity.Skill;
import com.webjob.application.Service.CompanyService;
import com.webjob.application.Service.JobService;
import com.webjob.application.Service.PaymentService;
import com.webjob.application.Service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/jobs") // Base URL chuẩn RESTful
@CrossOrigin(origins = "*")
@Slf4j
public class JobController {
    private final JobService jobService;

    private final ModelMapper modelMapper;

    private final CompanyService companyService;

    private final UserService userService;
    private final PaymentService paymentService;

    public JobController(JobService jobService, ModelMapper modelMapper, CompanyService companyService, UserService userService, PaymentService paymentService) {
        this.jobService = jobService;
        this.modelMapper = modelMapper;
        this.companyService = companyService;
        this.userService = userService;
        this.paymentService = paymentService;
    }

    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "TOKEN")
    @PostMapping
    public ResponseEntity<?> createJob(@Valid @RequestBody JobRequest request) {
        jobService.checkNameJob(request.getName());
        Job created = jobService.createJob(request);

        JobResponse response=modelMapper.map(created,JobResponse.class);
        List<String> skillNames = created.getSkills().stream().map(Skill::getName)
                .collect(Collectors.toList());
        response.setSkills(skillNames);
        Company company=companyService.getbyID(request.getCompanyId())
                .orElseThrow(() -> new IllegalArgumentException("Company not found with ID: " +request.getCompanyId()));
        if (company!=null){
            response.setCompanyName(company.getName());
        }
        ApiResponse<?> apiResponse=new ApiResponse<>(HttpStatus.CREATED.value(), null,
                "Tạo job thành công",
                response);

        return new ResponseEntity<>(apiResponse,HttpStatus.CREATED);

    }
    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "TOKEN")
    @PutMapping("/{id}")
    public ResponseEntity<?> editJob(@PathVariable Long id, @Valid @RequestBody JobRequest request) {

        Job update = jobService.updateJob(id,request);

        JobResponse response=modelMapper.map(update,JobResponse.class);
        List<String> skillNames = update.getSkills().stream().map(Skill::getName)
                .collect(Collectors.toList());
        response.setSkills(skillNames);
        Company company=companyService.getbyID(request.getCompanyId())
                .orElseThrow(() -> new IllegalArgumentException("Company not found with ID: " +request.getCompanyId()));
        if (company!=null){
            response.setCompanyName(company.getName());
        }
        ApiResponse<?> apiResponse=new ApiResponse<>(HttpStatus.OK.value(), null,
                "Update job thành công",
                response);
        return new ResponseEntity<>(apiResponse,HttpStatus.OK);

    }
    @RateLimit(maxRequests = 10, timeWindowSeconds = 60, keyType = "IP")
    @GetMapping
    public ResponseEntity<?> GetallPageList(@ModelAttribute JobFiltersearch jobFiltersearch){
        ResponseDTO<?> respond=jobService.getPaginated(jobFiltersearch,"default");
        ApiResponse<?> response=new ApiResponse<>(HttpStatus.OK.value(), null,
                "fetch all Jobs",
                respond
        );
        return ResponseEntity.ok(response);

    }

    @RateLimit(maxRequests = 15, timeWindowSeconds = 60, keyType = "IP")
    @GetMapping("/{id}")
    public ResponseEntity<?> detailJob(@PathVariable Long id) {
        Job job=jobService.getById(id);
        ApiResponse<?> apiResponse=new ApiResponse<>(HttpStatus.OK.value(), null,
                "Detail job thành công with "+id,
                job);
        return new ResponseEntity<>(apiResponse,HttpStatus.OK);
    }

    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "TOKEN")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteJobbyId(@PathVariable Long id) {
           Job job=jobService.getById(id);
           jobService.deleteJob(job);
            ApiResponse<Object> response = new ApiResponse<>(HttpStatus.OK.value(),
                    null,
                    "Delete Job successful with "+id,
                    null

            );
            return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @RateLimit(maxRequests = 10, timeWindowSeconds = 60, keyType = "IP")
    @GetMapping("/search")
    public ResponseEntity<?> GetallSearch(@ModelAttribute JobFiltersearch jobFiltersearch){
        ResponseDTO<?> respond=jobService.getPaginated(jobFiltersearch,"filter-job");
        ApiResponse<?> response=new ApiResponse<>(HttpStatus.OK.value(), null,
                "Filter all Jobs with condition Succesful",
                respond
        );
        return ResponseEntity.ok(response);

    }

    @RateLimit(maxRequests = 10, timeWindowSeconds = 60, keyType = "TOKEN")
    @GetMapping("/{jobId}/applicant-info")
    public ResponseEntity<?> getJobApplicantInfo(@PathVariable Long jobId) {

        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            User userHR = userService.getbyEmail(email);
            Long userId = userHR.getId();
            JobApplicantInfoResponse response = paymentService.getJobApplicantInfo(userId, jobId);

            ApiResponse<?> apiResponse=new ApiResponse<>(
                    HttpStatus.OK.value(),
                    null,
                    "Lấy thông tin ứng viên thành công",
                    response
            );
            return ResponseEntity.ok(apiResponse);

        } catch (Exception e) {
            log.error("Error getting job applicant info: {}", e.getMessage());
            ApiResponse<?> apiResponse=new ApiResponse<>(
                    HttpStatus.BAD_REQUEST.value(),
                    e.getMessage(),
                    "Lỗi lấy thông tin ứng viên",
                    null
            );
            return ResponseEntity.badRequest().body(apiResponse);
        }
    }



}
