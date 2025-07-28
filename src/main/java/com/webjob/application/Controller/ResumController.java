package com.webjob.application.Controller;

import com.webjob.application.Models.Company;
import com.webjob.application.Models.Job;
import com.webjob.application.Models.Request.JobRequest;
import com.webjob.application.Models.Request.UpdateResumeDTO;
import com.webjob.application.Models.Response.*;
import com.webjob.application.Models.Resume;
import com.webjob.application.Models.Skill;
import com.webjob.application.Services.ResumService;
import jakarta.validation.Valid;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class ResumController {
    private final ResumService resumService;
    private final ModelMapper modelMapper;


    public ResumController(ResumService resumService, ModelMapper modelMapper) {
        this.resumService = resumService;
        this.modelMapper = modelMapper;
    }

    @PostMapping("/create/resume")
    public ResponseEntity<?> createJob(@Valid @RequestBody Resume resume) {
        Resume resumeSave=resumService.saveResume(resume);
        ResumeResponse resumeResponse=modelMapper.map(resumeSave,ResumeResponse.class);
        ApiResponse<?> apiResponse=new ApiResponse<>(HttpStatus.CREATED.value(), null,
                "Tạo Resume thành công",
                resumeResponse);
        return new ResponseEntity<>(apiResponse,HttpStatus.CREATED);

    }
    @PutMapping("/edit/resume/{id}")
    public ResponseEntity<?> createJob(@PathVariable Long id, @Valid @RequestBody UpdateResumeDTO dto) {
        Resume edit=resumService.editResume(id,dto);
        ResumeResponse resumeResponse=modelMapper.map(edit,ResumeResponse.class);
        ApiResponse<?> apiResponse=new ApiResponse<>(HttpStatus.OK.value(), null,
                "Update Resume thành công",
                resumeResponse);
        return ResponseEntity.ok(apiResponse);

    }
    @DeleteMapping("/delete/resume/{id}")
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

    @GetMapping("/detail/resume/{id}")
    public ResponseEntity<?> detailResumebyId(@PathVariable Long id) {
        Resume resume=resumService.getById(id);
        ResumeResponse resumeResponse=modelMapper.map(resume,ResumeResponse.class);
        ApiResponse<?> response = new ApiResponse<>(HttpStatus.OK.value(),
                null,
                "Detail Resume successful with "+id,
                resumeResponse

        );
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
    @GetMapping("/api/resumes")
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
        Page<Resume> pagelist=resumService.getAllPage(page-1,size);
        int currentpage=pagelist.getNumber()+1;
        int pagesize=pagelist.getSize();
        int totalpage=pagelist.getTotalPages();
        Long totalItem=pagelist.getTotalElements();

        MetaDTO metaDTO=new MetaDTO(currentpage,pagesize,totalpage,totalItem);
        List<Resume> resumessList=pagelist.getContent();
        List<ResumeResponse> ResponseList=new ArrayList<>();
        for(Resume resume:resumessList){
            ResumeResponse resumeResponse=modelMapper.map(resume,ResumeResponse.class);
            ResponseList.add(resumeResponse);
        }
        ResponseDTO<?> respond=new ResponseDTO<>(metaDTO,ResponseList);
        ApiResponse<?> response=new ApiResponse<>(HttpStatus.OK.value(), null,
                "Fetch all Resume Successful",
                respond
        );
        return ResponseEntity.ok(response);

    }


}
