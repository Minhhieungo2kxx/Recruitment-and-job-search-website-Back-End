package com.webjob.application.Controller;

import com.webjob.application.Models.Company;
import com.webjob.application.Models.Job;
import com.webjob.application.Models.Request.JobRequest;
import com.webjob.application.Models.Response.*;
import com.webjob.application.Models.Skill;
import com.webjob.application.Models.User;
import com.webjob.application.Services.CompanyService;
import com.webjob.application.Services.JobService;
import jakarta.validation.Valid;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
public class JobController {
    private final JobService jobService;

    private final ModelMapper modelMapper;

    private final CompanyService companyService;

    public JobController(JobService jobService, ModelMapper modelMapper, CompanyService companyService) {
        this.jobService = jobService;
        this.modelMapper = modelMapper;
        this.companyService = companyService;
    }
    @PostMapping("/create/job")
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
    @PutMapping("/edit/job/{id}")
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
    @GetMapping("/api/jobs")
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
        Page<Job> pagelist=jobService.getAllPage(page-1,size);
        int currentpage=pagelist.getNumber()+1;
        int pagesize=pagelist.getSize();
        int totalpage=pagelist.getTotalPages();
        Long totalItem=pagelist.getTotalElements();

        MetaDTO metaDTO=new MetaDTO(currentpage,pagesize,totalpage,totalItem);
        List<Job> jobsList=pagelist.getContent();
        ResponseDTO<?> respond=new ResponseDTO<>(metaDTO,jobsList);
        ApiResponse<?> response=new ApiResponse<>(HttpStatus.OK.value(), null,
                "fetch all Jobs",
                respond
        );
        return ResponseEntity.ok(response);

    }

    @GetMapping("/detail/job/{id}")
    public ResponseEntity<?> detailJob(@PathVariable Long id) {
        Job job=jobService.getById(id);
        ApiResponse<?> apiResponse=new ApiResponse<>(HttpStatus.OK.value(), null,
                "Detail job thành công with "+id,
                job);
        return new ResponseEntity<>(apiResponse,HttpStatus.OK);
    }
    @DeleteMapping("/delete/job/{id}")
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



}
