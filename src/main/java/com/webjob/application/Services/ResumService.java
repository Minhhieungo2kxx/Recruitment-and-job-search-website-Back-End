package com.webjob.application.Services;


import com.webjob.application.Models.Job;
import com.webjob.application.Models.Request.UpdateResumeDTO;
import com.webjob.application.Models.Resume;
import com.webjob.application.Models.User;
import com.webjob.application.Repository.ResumeRepository;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class ResumService {
    @Autowired
    private JobService jobService;
    @Autowired
    private UserService userService;
    @Autowired
    private ResumeRepository resumeRepository;
    @Autowired
    private ModelMapper modelMapper;

    @Transactional
    public Resume saveResume(Resume resume){
        User user=userService.getbyID(resume.getUser().getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " +resume.getUser().getId()));
        Job job=jobService.getById(resume.getJob().getId());

        resume.setJob(job);
        resume.setUser(user);
        return resumeRepository.save(resume);

    }
    @Transactional
    public Resume editResume(Long id,UpdateResumeDTO updateResumeDTO){
        Resume update=getById(id);
        Instant instant=update.getCreatedAt();
        modelMapper.map(updateResumeDTO,update);
        update.setCreatedAt(instant);
        return resumeRepository.save(update);
    }

    public Resume getById(Long id){
        Resume resume=resumeRepository.findById(id).
                orElseThrow(() -> new IllegalArgumentException("Resume not found with ID: " +id));
        return resume;
    }
    @Transactional
    public void deleteResume(Resume resume){
        resumeRepository.delete(resume);
    }
    public Page<Resume> getAllPage(int page, int size){
        Sort.Direction direction=Sort.Direction.ASC;
        Sort sort=Sort.by(direction,"email");
        Pageable pageable= PageRequest.of(page,size,sort);
        return resumeRepository.findAll(pageable);

    }




}
