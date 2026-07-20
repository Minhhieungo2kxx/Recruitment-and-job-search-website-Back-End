package com.webjob.application.service;


import com.webjob.application.dto.Request.SkillRequest;
import com.webjob.application.dto.Request.SkillSearchRequest;
import com.webjob.application.dto.Response.*;
import com.webjob.application.mapper.SkillMapper;
import com.webjob.application.models.Entity.Skill;
import com.webjob.application.repository.JobCategorySkillRepository;
import com.webjob.application.repository.JobSkillRepository;
import com.webjob.application.repository.SkillRepository;
import com.webjob.application.repository.SubscriberSkillRepository;
import com.webjob.application.service.Specification.SkillSpecification;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SkillService {
    private final SkillRepository skillRepository;

    private final ModelMapper modelMapper;

    private final SkillMapper skillMapper;
    private final JobSkillRepository jobSkillRepository;
    private final SubscriberSkillRepository subscriberSkillRepository;
    private final JobCategorySkillRepository jobCategorySkillRepository;


    public boolean checkNameskill(String name) {
        boolean exist = skillRepository.existsByName(name);
        if (exist) {
            throw new IllegalArgumentException("Skill name " + name + " da ton tai");
        }
        return false;
    }

    public Skill handle(Skill skill) {
        return skillRepository.save(skill);
    }

    public boolean checkById(Long id) {
        boolean exists = skillRepository.existsById(id);
        if (!exists) {
            throw new IllegalArgumentException("Không tồn tại Skill với ID: " + id);
        }
        return true;
    }

    public Optional<Skill> getbyID(Long id) {
        return skillRepository.findById(id);
    }


    public Page<Skill> getAllPage(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));
        return skillRepository.findAll(pageable);
    }

    public ResponseDTO<List<SkillResponse>> getAllPageList(int page,int size) {

        try {
            if (page <= 0)
                page = 1;
            if(size<=0){
                size=8;
            }
        } catch (NumberFormatException e) {
            // Nếu người dùng nhập sai, mặc định về trang đầu
            page = 1;
            size=8;
        }
        Page<Skill> pagelist = getAllPage(page - 1, size);
        List<SkillResponse> responseList = pagelist.getContent().stream()
                .map(skillMapper::toResponse)
                .toList();

        int currentpage = pagelist.getNumber() + 1;
        int pagesize = pagelist.getSize();
        int totalpage = pagelist.getTotalPages();
        Long totalItem = pagelist.getTotalElements();

        MetaDTO metaDTO = new MetaDTO(currentpage, pagesize, totalpage, totalItem);
        ResponseDTO<List<SkillResponse>> respond = new ResponseDTO<>(metaDTO, responseList);
        return respond;
    }

    @Transactional
    public void deleteSkill(Long id) {
        if (!skillRepository.existsById(id)) {
            throw new IllegalArgumentException("Skill không tồn tại với id: " + id);
        }

        jobSkillRepository.deleteBySkillId(id);

        subscriberSkillRepository.deleteBySkillId(id);

        jobCategorySkillRepository.deleteBySkillId(id);
        skillRepository.deleteById(id);
    }


    @Transactional
    public SkillResponse createSkill(SkillRequest skillRequest) {
        checkNameskill(skillRequest.getName());
        Skill skill = modelMapper.map(skillRequest, Skill.class);
        Skill save = handle(skill);
        return skillMapper.toResponse(save);

    }


    @Transactional
    public SkillResponse updateSkill(Long id, SkillRequest skillRequest) {
        Skill update = getbyID(id).orElseThrow(() -> new IllegalArgumentException("Skill not found with ID: " + id));

        if (skillRequest.getName() != null & !skillRequest.getName().isEmpty()) {

            update.setName(skillRequest.getName());
        }
        if (skillRequest.getDescription() != null && !skillRequest.getDescription().isEmpty()) {
            update.setDescription(skillRequest.getDescription());
        }
        if (skillRequest.getStatus() != null && !skillRequest.getStatus().toString().isEmpty()) {
            update.setStatus(skillRequest.getStatus());
        }


        return skillMapper.toResponse(handle(update));
    }

    public SkillResponse getSkillByID(Long id) {
        Skill skill = getbyID(id).orElseThrow(() -> new IllegalArgumentException("Skill not found with ID: " + id));
        return skillMapper.toResponse(skill);

    }


    public Page<Skill> searchSkills(SkillSearchRequest request, Pageable pageable) {
        Specification<Skill> spec = Specification.where(SkillSpecification.hasKeyword(request.getKeyword()))
                .and(SkillSpecification.hasStatus(request.getStatus()))
                .and(SkillSpecification.createdBy(request.getCreatedBy()))
                .and(SkillSpecification.createdAfter(request.getFromDate()))
                .and(SkillSpecification.createdBefore(request.getToDate()));

        return skillRepository.findAll(spec, pageable);
    }
    public ResponseDTO<List<SkillResponse>> searchSkill(SkillSearchRequest request, int page, int size) {
        if(request==null){
            request = new SkillSearchRequest();
        }
        try {
            if (page <= 0)
                page = 1;
            if(size<=0){
                size=8;
            }
        } catch (NumberFormatException e) {
            // Nếu người dùng nhập sai, mặc định về trang đầu
            page = 1;
            size=8;
        }
        Pageable pageable = PageRequest.of(page-1, size, Sort.by("id"));

        Page<Skill> pages = searchSkills(request,pageable);
        List<SkillResponse> responses = pages.getContent()
                .stream()
                .map(skillMapper::toResponse)
                .toList();
        MetaDTO meta = new MetaDTO(
                pages.getNumber() + 1,
                pages.getSize(),
                pages.getTotalPages(),
                pages.getTotalElements()
        );

        return new ResponseDTO<>(meta, responses);
    }
    public List<SkillOptionResponse> searchSkillforSubscriber(String keyword){


        Specification<Skill> spec =
                SkillSpecification.hasKeyword(keyword);

        return skillRepository.findAll(spec)
                .stream()
                .map(skill -> modelMapper.map(skill,SkillOptionResponse.class))
                .toList();
    }



}
