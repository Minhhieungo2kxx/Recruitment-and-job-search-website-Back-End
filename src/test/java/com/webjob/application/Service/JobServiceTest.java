package com.webjob.application.service;

import com.webjob.application.Model.Entity.Company;
import com.webjob.application.Model.Entity.Job;
import com.webjob.application.Model.Entity.Skill;
import com.webjob.application.Model.Enums.CompetitionLevel;
import com.webjob.application.Model.Enums.JobCategory;
import com.webjob.application.Dto.Request.JobRequest;
import com.webjob.application.Repository.CompanyRepository;
import com.webjob.application.Repository.JobRepository;
import com.webjob.application.Repository.SkillRepository;
import com.webjob.application.Service.JobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JobServiceTest {
    @Mock private SkillRepository skillRepository;
    @Mock
    private JobRepository jobRepository;
    @Mock private ModelMapper modelMapper;
    @Mock private CompanyRepository companyRepository;
    @InjectMocks
    private JobService jobService;

    private JobRequest request;
    private Company company;
    private Job job;
    private List<Skill> validSkills;

    @BeforeEach
    void setUp() {
        Skill skill = new Skill();
        skill.setId(1L);
        validSkills = List.of(skill);

        JobRequest.SkillIdDTO skillDTO = new JobRequest.SkillIdDTO();
        skillDTO.setId(1L);

        request = new JobRequest();
        request.setName("Backend Dev");
        request.setSkills(List.of(skillDTO));
        request.setCompanyId(1L);
        request.setSalary(1000.0);
        request.setQuantity(3);
        request.setLevel("JUNIOR");
        request.setCompetitionLevel(CompetitionLevel.MEDIUM);
        request.setJobCategory(JobCategory.IT);
        request.setDescription("Java backend developer");
        request.setStartDate(Instant.now());
        request.setEndDate(Instant.now().plusSeconds(3600));

        company = new Company();
        company.setId(1L);

        job = new Job();
        job.setId(1L);
        job.setName(request.getName());
        job.setSkills(validSkills);
        job.setCompany(company);
        job.setAppliedCount(0);
    }
    @Test
    void createJob_ShouldCreate_WhenValidRequest() {
        when(skillRepository.findByIdIn(anyList())).thenReturn(validSkills);
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(modelMapper.map(eq(request), eq(Job.class))).thenReturn(job);
        when(jobRepository.save(any(Job.class))).thenReturn(job);

        Job result = jobService.createJob(request);

        assertThat(result).isNotNull();
        assertThat(result.getSkills()).hasSize(1);
        assertThat(result.getName()).isEqualTo("Backend Dev");
        verify(jobRepository).save(any(Job.class));
    }

    @Test
    void createJob_ShouldThrowException_WhenNoValidSkills() {
        when(skillRepository.findByIdIn(anyList())).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> jobService.createJob(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Không có kỹ năng nào hợp lệ");
    }

    @Test
    void createJob_ShouldThrowException_WhenCompanyNotFound() {
        when(skillRepository.findByIdIn(anyList())).thenReturn(validSkills);
        when(companyRepository.findById(1L)).thenReturn(Optional.empty());
        when(modelMapper.map(eq(request), eq(Job.class))).thenReturn(job);

        assertThatThrownBy(() -> jobService.createJob(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Company không tồn tại");
    }
    @Test
    void updateJob_ShouldUpdate_WhenValid() {
        Instant originalCreatedAt = Instant.now();
        job.setCreatedAt(originalCreatedAt);

        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(skillRepository.findByIdIn(anyList())).thenReturn(validSkills);
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(jobRepository.save(any(Job.class))).thenReturn(job);

        Job result = jobService.updateJob(1L, request);

        assertThat(result).isNotNull();
        assertThat(result.getCreatedAt()).isEqualTo(originalCreatedAt);
        verify(jobRepository).save(any(Job.class));
    }
    @Test
    void updateJob_ShouldThrowException_WhenJobNotFound() {
        when(jobRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobService.updateJob(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Job not found with ID: 1");
    }

    @Test
    void updateJob_ShouldThrowException_WhenNoValidSkills() {
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(skillRepository.findByIdIn(anyList())).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> jobService.updateJob(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Không có kỹ năng nào hợp lệ");
    }
    @Test
    void updateJob_ShouldThrowException_WhenCompanyNotFound() {
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(skillRepository.findByIdIn(anyList())).thenReturn(validSkills);
        when(companyRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobService.updateJob(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Company không tồn tại");
    }
    @Test
    void checkNameJob_ShouldThrow_WhenNameExists() {
        when(jobRepository.existsByName("Backend Dev")).thenReturn(true);

        assertThatThrownBy(() -> jobService.checkNameJob("Backend Dev"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Job name Backend Dev da ton tai");
    }
    @Test
    void checkNameJob_ShouldReturnFalse_WhenNameNotExist() {
        when(jobRepository.existsByName("Full Stack Develpoment")).thenReturn(false);
        boolean result = jobService.checkNameJob("Full Stack Develpoment");
        assertThat(result).isFalse();
    }
    @Test
    void getById_ShouldReturnJob_WhenExists() {
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
        Job found = jobService.getById(1L);
        assertThat(found).isEqualTo(job);
    }
    @Test
    void getById_ShouldThrow_WhenNotFound() {
        when(jobRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobService.getById(2L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Job not found with ID: 2");
    }
    @Test
    void deleteJob_ShouldClearSkillsAndDelete() {
        job.setSkills(new ArrayList<>(validSkills));

        jobService.deleteJob(job);

        assertThat(job.getSkills()).isEmpty();
        verify(jobRepository).delete(job);
    }













}
