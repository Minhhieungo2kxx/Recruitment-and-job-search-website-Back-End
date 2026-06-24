package com.webjob.application.service;


import com.webjob.application.dto.Request.SkillRequest;
import com.webjob.application.dto.Response.ApiResponse;
import com.webjob.application.dto.Response.MetaDTO;
import com.webjob.application.dto.Response.ResponseDTO;
import com.webjob.application.models.Entity.Skill;
import com.webjob.application.repository.SkillRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SkillService {
    private final SkillRepository skillRepository;


    public boolean checkNameskill(String name){
        boolean exist=skillRepository.existsByName(name);
        if (exist){
            throw new IllegalArgumentException("Skill name "+name+" da ton tai");
        }
        return false;
    }

    public Skill handle(Skill skill){
        return skillRepository.save(skill);
    }

    public boolean checkById(Long id) {
        boolean exists = skillRepository.existsById(id);
        if (!exists) {
            throw new IllegalArgumentException("Không tồn tại Skill với ID: " + id);
        }
        return true;
    }
    public Optional<Skill> getbyID(Long id){
        return skillRepository.findById(id);
    }

    public Page<Skill> getAllPage(int page, int size){
        Sort.Direction direction=Sort.Direction.ASC;
        Sort sort=Sort.by(direction,"name");
        Pageable pageable= PageRequest.of(page,size,sort);
        return skillRepository.findAll(pageable);
    }
    public ResponseDTO<?> getAllPageList(String pageparam,String type){
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
        Page<Skill> pagelist=getAllPage(page-1,size);
        int currentpage=pagelist.getNumber()+1;
        int pagesize=pagelist.getSize();
        int totalpage=pagelist.getTotalPages();
        Long totalItem=pagelist.getTotalElements();

        MetaDTO metaDTO=new MetaDTO(currentpage,pagesize,totalpage,totalItem);
        ResponseDTO<?> respond=new ResponseDTO<>(metaDTO,pagelist.getContent());
        return respond;
    }
    @Transactional
    public void deleteSkill(Long id) {
        Skill skill = skillRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Skill không tồn tại với id: " + id));

        // Gỡ bỏ quan hệ với jobs
        if (skill.getJobs() != null) {
            skill.getJobs().forEach(job -> job.getSkills().remove(skill));
            skill.getJobs().clear(); // để tránh cascade lỗi
        }

        // Gỡ bỏ quan hệ với subscribers
        if (skill.getSubscribers() != null) {
            skill.getSubscribers().forEach(subscriber -> subscriber.getSkills().remove(skill));
            skill.getSubscribers().clear();
        }
        skillRepository.delete(skill);
    }
    @Transactional
    public ResponseEntity<?> create_Skill(Skill skill) {
        checkNameskill(skill.getName());
        Skill save=handle(skill);
        ApiResponse<?> response = new ApiResponse<>(
                HttpStatus.CREATED.value(),
                null,
                "Create Skill successful",
                save

        );
        return new ResponseEntity<>(response, HttpStatus.CREATED);

    }
    @Transactional
    public ResponseEntity<?> Edit_Skill(Long id,  SkillRequest skillRequest) {
        Skill update=getbyID(id).orElseThrow(() -> new IllegalArgumentException("Skill not found with ID: " + id));

        if(skillRequest.getName() !=null & !skillRequest.getName().isEmpty()) {
            checkNameskill(skillRequest.getName());
            update.setName(skillRequest.getName());
        }
        handle(update);
        ApiResponse<?> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Edit Skill successful",
                update

        );
        return new ResponseEntity<>(response, HttpStatus.OK);

    }


}
