package com.webjob.application.Service;


import com.webjob.application.Dto.Response.MetaDTO;
import com.webjob.application.Dto.Response.ResponseDTO;
import com.webjob.application.Model.Entity.Skill;
import com.webjob.application.Repository.SkillRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class SkillService {
    private final SkillRepository skillRepository;

    public SkillService(SkillRepository skillRepository) {
        this.skillRepository = skillRepository;
    }
    public boolean checkNameskill(String name){
        boolean exist=skillRepository.existsByName(name);
        if (exist){
            throw new IllegalArgumentException("Skill name "+name+" da ton tai");
        }
        return false;
    }
    @Transactional
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


}
