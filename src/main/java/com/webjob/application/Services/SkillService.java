package com.webjob.application.Services;


import com.webjob.application.Models.Skill;
import com.webjob.application.Models.User;
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


}
