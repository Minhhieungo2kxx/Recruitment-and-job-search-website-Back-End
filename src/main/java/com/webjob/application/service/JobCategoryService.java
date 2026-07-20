package com.webjob.application.service;

import com.webjob.application.dto.Request.JobCategoryRequest;
import com.webjob.application.dto.Request.JobCategorySearchRequest;
import com.webjob.application.dto.Response.*;
import com.webjob.application.exception.Customs.BadRequestException;
import com.webjob.application.exception.Customs.ResourceNotFoundException;
import com.webjob.application.mapper.JobCategoryMapper;
import com.webjob.application.models.Entity.JobCategory;
import com.webjob.application.models.Entity.JobCategorySkill;
import com.webjob.application.models.Entity.Skill;
import com.webjob.application.repository.JobAlertRepository;
import com.webjob.application.repository.JobCategoryRepository;
import com.webjob.application.repository.JobRepository;
import com.webjob.application.repository.SkillRepository;
import com.webjob.application.service.Specification.JobCategorySpecification;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Request;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobCategoryService {
    private final JobCategoryRepository jobCategoryRepository;
    private final ModelMapper modelMapper;
    private final SkillRepository skillRepository;

    private final JobCategoryMapper jobCategoryMapper;

    private final JobRepository jobRepository;

    private final JobAlertRepository jobAlertRepository;


    @Transactional
    public JobCategoryResponse create(JobCategoryRequest request) {
        if (jobCategoryRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Category name already exists");
        }
        JobCategory category = new JobCategory();
        modelMapper.map(request, category);

        if (request.getParentId() != null) {
            JobCategory parent = findById(request.getParentId());
            category.setParent(parent);
            category.setLevel(parent.getLevel() + 1);
        } else {
            category.setParent(null);
            category.setLevel(0);
        }

        if (request.getSkills() != null) {
            Map<Long, Skill> skillMaps = getSkillMapFromRequest(request);
            for (JobCategoryRequest.JobCategorySkillRequest dto : request.getSkills()) {
                Skill skill = skillMaps.get(dto.getSkillId());
                if (skill == null) {
                    throw new BadRequestException("Skill not found with id: " + dto.getSkillId());
                }
                if (dto.getWeight() < 0 || dto.getWeight() > 100) {
                    throw new BadRequestException("Weight must be between 0 and 100");
                }
                JobCategorySkill relation = new JobCategorySkill();
                modelMapper.map(dto, relation);
                relation.setJobCategory(category);
                relation.setSkill(skill);
                category.getJobCategorySkills()
                        .add(relation);
            }

        }
        jobCategoryRepository.save(category);
        return jobCategoryMapper.toResponse(category);

    }

    @Transactional
    public JobCategoryResponse update(Long id, JobCategoryRequest request) {
        JobCategory category = findById(id);
        if (jobCategoryRepository.existsByNameAndIdNot(request.getName(), id)) {
            throw new IllegalArgumentException("Category name already exists");
        }
        modelMapper.map(request, category);
        if (request.getParentId() != null) {

            JobCategory parent = findById(request.getParentId());
            if (parent.getId().equals(category.getId())) {
                throw new BadRequestException("Parent cannot be itself");
            }
            if (isDescendant(parent, category)) {
                throw new BadRequestException("Circular hierarchy detected");
            }
            category.setParent(parent);
            category.setLevel(parent.getLevel() + 1);
        } else {
            category.setParent(null);
            category.setLevel(0);
        }

        if (request.getSkills() != null) {
            Map<Long, Skill> skillMaps = getSkillMapFromRequest(request);

            for (JobCategoryRequest.JobCategorySkillRequest dto : request.getSkills()) {
                Skill skill = skillMaps.get(dto.getSkillId());
                if (skill == null) {
                    throw new IllegalArgumentException("Skill not found with id: " + dto.getSkillId());
                }
                if (dto.getWeight() < 0 || dto.getWeight() > 100) {
                    throw new IllegalArgumentException("Weight must be between 0 and 100");
                }
                JobCategorySkill relation = new JobCategorySkill();

                modelMapper.map(dto, relation);
                relation.setJobCategory(category);
                relation.setSkill(skill);
                category.getJobCategorySkills()
                        .add(relation);
            }

        }
        jobCategoryRepository.save(category);
        return jobCategoryMapper.toResponse(category);

    }

    public JobCategory findById(Long id) {
        return jobCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("JobCategory not found with id: " + id));
    }

    private boolean isDescendant(JobCategory parent,
                                 JobCategory child) {
        if (parent == null) {
            return false;
        }
        JobCategory current = parent;
        while (current != null) {

            if (current.getId().equals(child.getId())) {
                return true;
            }

            current = current.getParent();
        }

        return false;

    }

    @Transactional(readOnly = true)
    public JobCategoryResponse getById(Long id) {
        JobCategory category = jobCategoryRepository.findByIdWithSkills(id)
                .orElseThrow(() -> new ResourceNotFoundException("JobCategory not found with id: " + id));
        return jobCategoryMapper.toResponse(category);
    }

    @Transactional
    public void delete(Long id) {

        JobCategory category = findById(id);

        if (jobRepository.existsByJobCategoryIdAndDeletedFalse(id)) {
            throw new BadRequestException("Cannot delete category because it is being used by jobs.");
        }

        if (jobCategoryRepository.existsByParentId(id)) {
            throw new BadRequestException("Cannot delete category because it has child categories.");
        }
        if (jobAlertRepository.existsByJobCategoryId(id)) {
            throw new BadRequestException(
                    "Cannot delete category because it is being used by job alerts.");
        }

        jobCategoryRepository.delete(category);
    }

    private Map<Long, Skill> getSkillMapFromRequest(JobCategoryRequest request) {

        if (request.getSkills() == null || request.getSkills().isEmpty()) {
            return Map.of();
        }
        Set<Long> ids = request.getSkills()
                .stream()
                .map(JobCategoryRequest.JobCategorySkillRequest::getSkillId)
                .collect(Collectors.toSet());

        List<Skill> skills = skillRepository.findAllById(ids);

        Map<Long, Skill> skillMap = skills.stream()
                .collect(Collectors.toMap(Skill::getId, Function.identity()));
        return skillMap;
    }

    public ResponseDTO<List<JobCategoryResponse>> getAllPageList(int page, int size, JobCategorySearchRequest request) {

        try {
            if (page <= 0)
                page = 1;
            if (size <= 0) {
                size = 8;
            }
        } catch (NumberFormatException e) {
            // Nếu người dùng nhập sai, mặc định về trang đầu
            page = 1;
            size = 8;
        }
        Page<JobCategory> pagelist = getAllPage(page - 1, size,request);
        List<JobCategoryResponse> responseList = pagelist.getContent().stream()
                .map(jobCategoryMapper::toResponse)
                .toList();

        int currentpage = pagelist.getNumber() + 1;
        int pagesize = pagelist.getSize();
        int totalpage = pagelist.getTotalPages();
        Long totalItem = pagelist.getTotalElements();

        MetaDTO metaDTO = new MetaDTO(currentpage, pagesize, totalpage, totalItem);
        ResponseDTO<List<JobCategoryResponse>> respond = new ResponseDTO<>(metaDTO, responseList);
        return respond;
    }

    public Page<JobCategory> getAllPage(int page, int size,JobCategorySearchRequest request) {
        Specification<JobCategory> specification =
                Specification.where(JobCategorySpecification.hasKeyword(request.getKeyword()))
                        .and(JobCategorySpecification.hasStatus(request.getStatus()));
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));
        return jobCategoryRepository.findAll(specification,pageable);
    }

    public List<JobCategoryTreeResponse> getTree() {
        // Query 1
        List<JobCategory> categories = jobCategoryRepository.findAllTree();
        // Query 2
        Map<Long, Long> jobCountMap = jobRepository.countJobsByCategory()
                .stream()
                .collect(Collectors.toMap(
                        dto -> dto.getCategoryId(),
                        dto -> dto.getJobCount()
                ));

        // id -> DTO
        Map<Long, JobCategoryTreeResponse> dtoMap = new LinkedHashMap<>();

        // Tạo DTO
        for (JobCategory category : categories) {
            dtoMap.put(category.getId(),jobCategoryMapper.toResponseTree(category,jobCountMap));
        }
        List<JobCategoryTreeResponse> roots = new ArrayList<>();

        // Build Tree
        for (JobCategory category : categories) {
            JobCategoryTreeResponse current = dtoMap.get(category.getId());
            if (category.getParent() == null) {
                roots.add(current);
            } else {
                JobCategoryTreeResponse parent = dtoMap.get(category.getParent().getId());
                parent.getChildren().add(current);

            }
        }
        // Tính tổng Job từ dưới lên
        for (JobCategoryTreeResponse root : roots) {
            calculateTotalJobs(root);
        }
        return roots;
    }
    public long calculateTotalJobs(JobCategoryTreeResponse node) {
        long total = node.getTotalJobs();
        for (JobCategoryTreeResponse child : node.getChildren()) {

            total += calculateTotalJobs(child);

        }
        node.setTotalJobs(total);
        return total;
    }

    @Transactional(readOnly = true)
    public JobCategoryResponseChildren getJobCategoriesChildren(Long id){
        JobCategory jobCategory=jobCategoryRepository.findDetailById(id)
                .orElseThrow(() -> new ResourceNotFoundException("JobCategory not found with id: " + id));
        return jobCategoryMapper.toResponseTreeChildren(jobCategory);
    }
}
