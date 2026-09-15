package com.webjob.application.service;


import com.webjob.application.dto.Request.SkillRequest;
import com.webjob.application.dto.Request.SkillSearchRequest;
import com.webjob.application.dto.Response.*;
import com.webjob.application.mapper.SkillMapper;
import com.webjob.application.models.Entity.Skill;
import com.webjob.application.models.Entity.SkillAlias;
import com.webjob.application.repository.*;
import com.webjob.application.service.Specification.SkillSpecification;

import com.webjob.application.utils.common.UtilFormat;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SkillService {
    private final SkillRepository skillRepository;

    private final ModelMapper modelMapper;

    private final SkillMapper skillMapper;
    private final JobSkillRepository jobSkillRepository;
    private final SubscriberSkillRepository subscriberSkillRepository;
    private final JobCategorySkillRepository jobCategorySkillRepository;

    private final SkillAliasRepository skillAliasRepository;


    public boolean checkNameskill(String name) {
        boolean exist = skillRepository.existsByName(name);
        if (exist) {
            throw new IllegalArgumentException("Skill name " + name + " da ton tai");
        }
        return false;
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

    @Transactional(readOnly = true)
    public ResponseDTO<List<SkillResponse>> getAllPageList(int page,int size) {

        size = Math.min(Math.max(size, 1), 50);
        page = Math.max(page, 1);
        Page<Skill> pagelist = getAllPage(page - 1, size);


        List<Skill> skills = pagelist.getContent();

        List<Long> skillIds = skills.stream().map(Skill::getId).toList();

        // Chỉ query aliases khi page có Skill
        List<SkillAlias> aliases = skillIds.isEmpty()
                ? Collections.emptyList()
                : skillAliasRepository.findAllBySkillIds(skillIds);

        Map<Long, List<SkillAlias>> aliasesBySkillId =
                aliases.stream()
                        .collect(Collectors.groupingBy(
                                alias -> alias.getSkill().getId()
                        ));

        List<SkillResponse> responses = skills.stream()
                .map(skill -> skillMapper.toResponsePage(
                        skill,
                        aliasesBySkillId.getOrDefault(
                                skill.getId(),
                                Collections.emptyList()
                        )
                ))
                .toList();

        int currentpage = pagelist.getNumber() + 1;
        int pagesize = pagelist.getSize();
        int totalpage = pagelist.getTotalPages();
        Long totalItem = pagelist.getTotalElements();

        MetaDTO metaDTO = new MetaDTO(currentpage, pagesize, totalpage, totalItem);
        ResponseDTO<List<SkillResponse>> respond = new ResponseDTO<>(metaDTO, responses);
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
        List<SkillAlias> skillAliasList=new ArrayList<>();


        if (skillRequest.getAliases() != null) {
            for (SkillRequest.SkillAliasRequest request : skillRequest.getAliases()) {
                skillAliasList.add(
                        SkillAlias.builder()
                                .skill(skill)
                                .alias(request.getAlias())
                                .aliasType(request.getAliasType())
                                .status(request.getStatus())
                                .normalizedAlias(UtilFormat.normalize(request.getAlias()))
                                .build()
                );
            }
        }

        skill.setAliases(skillAliasList);

        Skill save = skillRepository.save(skill);
        return skillMapper.toResponse(save);
    }

    @Transactional
    public SkillResponse updateSkill(Long id, SkillRequest skillRequest) {

        Skill skill = getbyID(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Skill not found with ID: " + id));

        if (skillRequest.getName() != null && !skillRequest.getName().isBlank()) {
            skill.setName(skillRequest.getName());
        }
        if (skillRepository.existsByNameAndIdNot(skill.getName(), id)) {
            throw new IllegalArgumentException(
                    "Skill name '" + skill.getName() + "' already exists");
        }

        if (skillRequest.getDescription() != null && !skillRequest.getDescription().isBlank()) {
            skill.setDescription(skillRequest.getDescription());
        }

        if (skillRequest.getStatus() != null) {
            skill.setStatus(skillRequest.getStatus());
        }

        if (skillRequest.getAliases() != null) {

            Set<String> normalizedAliases = new HashSet<>();

            List<SkillAlias> newAliases = new ArrayList<>();

            for (SkillRequest.SkillAliasRequest request : skillRequest.getAliases()) {

                String alias = request.getAlias();

                if (alias == null || alias.isBlank()) {
                    throw new IllegalArgumentException("Alias must not be blank");
                }

                String normalized =
                        UtilFormat.normalize(alias);

                if (!normalizedAliases.add(normalized)) {
                    throw new IllegalArgumentException("Duplicate alias: " + alias);
                }

                SkillAlias skillAlias = SkillAlias.builder()
                        .skill(skill)
                        .alias(alias)
                        .aliasType(request.getAliasType())
                        .status(request.getStatus())
                        .normalizedAlias(normalized)
                        .build();

                newAliases.add(skillAlias);
            }

            // Remove old references from Hibernate's managed collection
            skill.getAliases().clear();

            // Delete old rows directly
            skillAliasRepository.deleteBySkillId(id);

            // Make sure DELETE is executed now
            skillAliasRepository.flush();

            // Add completely new entities
            skill.getAliases().addAll(newAliases);
        }

        Skill saved = skillRepository.save(skill);

        return skillMapper.toResponse(saved);
    }







    public SkillResponse getSkillByID(Long id) {
        Skill skill=skillRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Skill not found with ID: " + id));
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


    @Transactional(readOnly = true)
    public ResponseDTO<List<SkillResponse>> searchSkill(
            SkillSearchRequest request,
            int page,
            int size
    ) {
        if (request == null) {
            request = new SkillSearchRequest();
        }

        size = Math.min(Math.max(size, 1), 50);
        page = Math.max(page, 1);

        Pageable pageable = PageRequest.of(
                page - 1,
                size,
                Sort.by("id")
        );

        Page<Skill> pages = searchSkills(request, pageable);

        List<Skill> skills = pages.getContent();

        List<Long> skillIds = skills.stream()
                .map(Skill::getId)
                .toList();

        // Chỉ query aliases khi page có Skill
        List<SkillAlias> aliases = skillIds.isEmpty()
                ? Collections.emptyList()
                : skillAliasRepository.findAllBySkillIds(skillIds);

        Map<Long, List<SkillAlias>> aliasesBySkillId =
                aliases.stream()
                        .collect(Collectors.groupingBy(
                                alias -> alias.getSkill().getId()
                        ));

        List<SkillResponse> responses = skills.stream()
                .map(skill -> skillMapper.toResponsePage(
                        skill,
                        aliasesBySkillId.getOrDefault(
                                skill.getId(),
                                Collections.emptyList()
                        )
                ))
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
