package com.webjob.application.service;


import com.webjob.application.dto.Request.JobCategoryRequest;
import com.webjob.application.dto.Response.JobCategoryResponse;
import com.webjob.application.dto.Response.JobCategoryTreeResponse;
import com.webjob.application.exception.Customs.BadRequestException;
import com.webjob.application.exception.Customs.ResourceNotFoundException;
import com.webjob.application.mapper.JobCategoryMapper;
import com.webjob.application.models.Entity.JobCategory;
import com.webjob.application.models.Entity.Skill;
import com.webjob.application.repository.JobAlertRepository;
import com.webjob.application.repository.JobCategoryRepository;
import com.webjob.application.repository.JobRepository;
import com.webjob.application.repository.SkillRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
@DisplayName("JobCategoryService Unit Tests")
public class JobCategoryServiceTest {
    @Mock
    private JobCategoryRepository jobCategoryRepository;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private SkillRepository skillRepository;

    @Mock
    private JobCategoryMapper jobCategoryMapper;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private JobAlertRepository jobAlertRepository;

    @InjectMocks
    private JobCategoryService jobCategoryService;

    private JobCategoryResponse response;

    @BeforeEach
    void setUp() {
        response = new JobCategoryResponse();
    }

    // --------------------------------------------------
    // findById
    // --------------------------------------------------

    @Test
    void findById_whenCategoryExists_returnsCategory() {
        JobCategory category = new JobCategory();
        category.setId(1L);

        when(jobCategoryRepository.findById(1L))
                .thenReturn(Optional.of(category));

        JobCategory result = jobCategoryService.findById(1L);

        assertThat(result).isSameAs(category);
    }

    @Test
    void findById_whenCategoryDoesNotExist_throwsException() {
        when(jobCategoryRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobCategoryService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // --------------------------------------------------
    // create
    // --------------------------------------------------

    @Test
    void create_whenNameAlreadyExists_throwsException() {
        JobCategoryRequest request = new JobCategoryRequest();
        request.setName("IT");

        when(jobCategoryRepository.existsByName("IT"))
                .thenReturn(true);

        assertThatThrownBy(() -> jobCategoryService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("IT");

        verify(jobCategoryRepository, never()).save(any());
    }

    @Test
    void create_whenParentIsNull_createsRootCategory() {
        JobCategoryRequest request = new JobCategoryRequest();
        request.setName("IT");
        request.setParentId(null);

        when(jobCategoryRepository.existsByName("IT"))
                .thenReturn(false);

        when(jobCategoryMapper.toResponse(any(JobCategory.class)))
                .thenReturn(response);

        JobCategoryResponse result = jobCategoryService.create(request);

        assertThat(result).isSameAs(response);

        org.mockito.ArgumentCaptor<JobCategory> captor = org.mockito.ArgumentCaptor.forClass(JobCategory.class);

        verify(jobCategoryRepository).save(captor.capture());

        JobCategory saved = captor.getValue();
        assertThat(saved.getParent()).isNull();
        assertThat(saved.getLevel()).isZero();
    }

    @Test
    void create_whenParentExists_setsParentAndLevel() {
        JobCategoryRequest request = new JobCategoryRequest();
        request.setName("Backend");
        request.setParentId(1L);

        JobCategory parent = new JobCategory();
        parent.setId(1L);
        parent.setLevel(2);

        when(jobCategoryRepository.existsByName("Backend"))
                .thenReturn(false);
        when(jobCategoryRepository.findById(1L))
                .thenReturn(Optional.of(parent));
        when(jobCategoryMapper.toResponse(any(JobCategory.class)))
                .thenReturn(response);

        jobCategoryService.create(request);

        org.mockito.ArgumentCaptor<JobCategory> captor = org.mockito.ArgumentCaptor.forClass(JobCategory.class);

        verify(jobCategoryRepository).save(captor.capture());

        JobCategory saved = captor.getValue();
        assertThat(saved.getParent()).isSameAs(parent);
        assertThat(saved.getLevel()).isEqualTo(3);
    }

    @Test
    void create_whenParentDoesNotExist_throwsException() {
        JobCategoryRequest request = new JobCategoryRequest();
        request.setName("Backend");
        request.setParentId(99L);

        when(jobCategoryRepository.existsByName("Backend"))
                .thenReturn(false);
        when(jobCategoryRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobCategoryService.create(request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(jobCategoryRepository, never()).save(any());
    }

    // --------------------------------------------------
    // update
    // --------------------------------------------------
    @Test
    void update_whenNameBelongsToAnotherCategory_throwsException() {
        JobCategory category = new JobCategory();
        category.setId(1L);

        JobCategoryRequest request = new JobCategoryRequest();
        request.setName("Backend");

        when(jobCategoryRepository.findById(1L))
                .thenReturn(Optional.of(category));
        when(jobCategoryRepository.existsByNameAndIdNot("Backend", 1L))
                .thenReturn(true);

        assertThatThrownBy(() -> jobCategoryService.update(1L, request))
                .isInstanceOf(IllegalArgumentException.class);

        verify(jobCategoryRepository, never()).save(any());
    }

    @Test
    void update_whenParentIsItself_throwsException() {
        JobCategory category = new JobCategory();
        category.setId(1L);
        category.setJobCategorySkills(new ArrayList<>());

        JobCategoryRequest request = new JobCategoryRequest();
        request.setName("Backend");
        request.setParentId(1L);

        when(jobCategoryRepository.findById(1L))
                .thenReturn(Optional.of(category));
        when(jobCategoryRepository.existsByNameAndIdNot("Backend", 1L))
                .thenReturn(false);

        assertThatThrownBy(() -> jobCategoryService.update(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Parent cannot be itself");

        verify(jobCategoryRepository, never()).save(any());
    }

    @Test
    void update_whenParentIsDescendant_throwsCircularHierarchyException() {
        JobCategory category = new JobCategory();
        category.setId(1L);
        category.setLevel(0);
        category.setJobCategorySkills(new ArrayList<>());

        JobCategory child = new JobCategory();
        child.setId(2L);
        child.setLevel(1);
        child.setParent(category);

        JobCategoryRequest request = new JobCategoryRequest();
        request.setName("Root");
        request.setParentId(2L);

        when(jobCategoryRepository.findById(1L))
                .thenReturn(Optional.of(category));
        when(jobCategoryRepository.existsByNameAndIdNot("Root", 1L))
                .thenReturn(false);
        when(jobCategoryRepository.findById(2L))
                .thenReturn(Optional.of(child));

        assertThatThrownBy(() -> jobCategoryService.update(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Circular hierarchy");

        verify(jobCategoryRepository, never()).save(any());
    }


    // --------------------------------------------------
    // delete
    // --------------------------------------------------
    @Test
    void delete_whenCategoryIsUnused_deletesCategory() {
        JobCategory category = new JobCategory();
        category.setId(1L);

        when(jobCategoryRepository.findById(1L))
                .thenReturn(Optional.of(category));
        when(jobRepository.existsByJobCategoryIdAndDeletedFalse(1L))
                .thenReturn(false);
        when(jobCategoryRepository.existsByParentId(1L))
                .thenReturn(false);
        when(jobAlertRepository.existsByJobCategoryId(1L))
                .thenReturn(false);

        jobCategoryService.delete(1L);

        verify(jobCategoryRepository).delete(category);
    }

    @Test
    void delete_whenCategoryHasActiveJobs_throwsException() {
        JobCategory category = new JobCategory();
        category.setId(1L);

        when(jobCategoryRepository.findById(1L))
                .thenReturn(Optional.of(category));
        when(jobRepository.existsByJobCategoryIdAndDeletedFalse(1L))
                .thenReturn(true);

        assertThatThrownBy(() -> jobCategoryService.delete(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("being used by jobs");

        verify(jobCategoryRepository, never()).delete(any(JobCategory.class));
        verify(jobCategoryRepository, never()).existsByParentId(anyLong());
        verifyNoInteractions(jobAlertRepository);
    }

    @Test
    void delete_whenCategoryHasChildren_throwsException() {
        JobCategory category = new JobCategory();
        category.setId(1L);

        when(jobCategoryRepository.findById(1L))
                .thenReturn(Optional.of(category));
        when(jobRepository.existsByJobCategoryIdAndDeletedFalse(1L))
                .thenReturn(false);
        when(jobCategoryRepository.existsByParentId(1L))
                .thenReturn(true);

        assertThatThrownBy(() -> jobCategoryService.delete(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("child categories");

        verify(jobCategoryRepository, never()).delete(any(JobCategory.class));
        verifyNoInteractions(jobAlertRepository);
    }

    @Test
    void delete_whenCategoryIsUsedByJobAlert_throwsException() {
        JobCategory category = new JobCategory();
        category.setId(1L);

        when(jobCategoryRepository.findById(1L))
                .thenReturn(Optional.of(category));
        when(jobRepository.existsByJobCategoryIdAndDeletedFalse(1L))
                .thenReturn(false);
        when(jobCategoryRepository.existsByParentId(1L))
                .thenReturn(false);
        when(jobAlertRepository.existsByJobCategoryId(1L))
                .thenReturn(true);

        assertThatThrownBy(() -> jobCategoryService.delete(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("job alerts");

        verify(jobCategoryRepository, never()).delete(any(JobCategory.class));
    }


    // --------------------------------------------------
    // getById
    // --------------------------------------------------
    @Test
    void getById_whenCategoryExists_returnsMappedResponse() {
        JobCategory category = new JobCategory();
        category.setId(1L);

        when(jobCategoryRepository.findByIdWithSkills(1L))
                .thenReturn(Optional.of(category));
        when(jobCategoryMapper.toResponse(category))
                .thenReturn(response);

        JobCategoryResponse result = jobCategoryService.getById(1L);

        assertThat(result).isSameAs(response);
        verify(jobCategoryMapper).toResponse(category);
    }

    @Test
    void getById_whenCategoryDoesNotExist_throwsException() {
        when(jobCategoryRepository.findByIdWithSkills(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobCategoryService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");

        verifyNoInteractions(jobCategoryMapper);
    }

    // --------------------------------------------------
    // calculateTotalJobs
    // --------------------------------------------------

    @Test
    void calculateTotalJobs_whenNodeIsLeaf_returnsItsOwnJobCount() {
        JobCategoryTreeResponse node = new JobCategoryTreeResponse();
        node.setTotalJobs(5L);
        node.setChildren(new ArrayList<>());

        long result = jobCategoryService.calculateTotalJobs(node);

        assertThat(result).isEqualTo(5L);
        assertThat(node.getTotalJobs()).isEqualTo(5L);
    }

    @Test
    void calculateTotalJobs_whenNodeHasChildren_sumsAllDescendants() {
        JobCategoryTreeResponse root = new JobCategoryTreeResponse();
        root.setTotalJobs(2L);

        JobCategoryTreeResponse child = new JobCategoryTreeResponse();
        child.setTotalJobs(3L);

        JobCategoryTreeResponse grandchild = new JobCategoryTreeResponse();
        grandchild.setTotalJobs(4L);
        grandchild.setChildren(new ArrayList<>());

        child.setChildren(new ArrayList<>(List.of(grandchild)));
        root.setChildren(new ArrayList<>(List.of(child)));

        long result = jobCategoryService.calculateTotalJobs(root);

        assertThat(result).isEqualTo(9L);
        assertThat(root.getTotalJobs()).isEqualTo(9L);
        assertThat(child.getTotalJobs()).isEqualTo(7L);
        assertThat(grandchild.getTotalJobs()).isEqualTo(4L);
    }


    @Test
    void create_whenSkillDoesNotExist_throwsBadRequestException() {
        JobCategoryRequest.JobCategorySkillRequest skillRequest =
                new JobCategoryRequest.JobCategorySkillRequest();
        skillRequest.setSkillId(99L);
        skillRequest.setWeight(50);

        JobCategoryRequest request = new JobCategoryRequest();
        request.setName("Backend");
        request.setSkills(List.of(skillRequest));

        when(jobCategoryRepository.existsByName("Backend"))
                .thenReturn(false);
        when(skillRepository.findAllById(anySet()))
                .thenReturn(List.of());

        assertThatThrownBy(() -> jobCategoryService.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("99");

        verify(jobCategoryRepository, never()).save(any());
    }

    @Test
    void create_whenSkillWeightIsGreaterThan100_throwsBadRequestException() {
        JobCategoryRequest.JobCategorySkillRequest skillRequest = new JobCategoryRequest.JobCategorySkillRequest();
        skillRequest.setSkillId(10L);
        skillRequest.setWeight(101);

        JobCategoryRequest request = new JobCategoryRequest();
        request.setName("Backend");
        request.setSkills(List.of(skillRequest));

        Skill skill = new Skill();
        skill.setId(10L);

        when(jobCategoryRepository.existsByName("Backend"))
                .thenReturn(false);
        when(skillRepository.findAllById(anySet()))
                .thenReturn(List.of(skill));

        assertThatThrownBy(() -> jobCategoryService.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Weight must be between 0 and 100");

        verify(jobCategoryRepository, never()).save(any());
    }






}
