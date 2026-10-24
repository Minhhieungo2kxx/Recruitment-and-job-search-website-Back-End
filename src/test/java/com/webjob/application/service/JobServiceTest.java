package com.webjob.application.service;

import com.webjob.application.component.SecurityUtils;
import com.webjob.application.dto.Request.JobFilterClient;
import com.webjob.application.dto.Request.JobFilterHrRequest;
import com.webjob.application.dto.Request.JobRequest;
import com.webjob.application.dto.Response.ElasticsearchSearchResult;
import com.webjob.application.dto.Response.JobApplicantInfoResponse;
import com.webjob.application.dto.Response.JobResponse;
import com.webjob.application.dto.Response.ResponseDTO;
import com.webjob.application.elasticsearch.document.JobDocument;
import com.webjob.application.elasticsearch.job.JobElasticsearchSearchService;
import com.webjob.application.enums.*;
import com.webjob.application.event.dto.JobCreatedEvent;
import com.webjob.application.exception.Customs.BadRequestException;
import com.webjob.application.exception.Customs.ForbiddenException;
import com.webjob.application.mapper.JobMapper;
import com.webjob.application.models.Entity.*;
import com.webjob.application.models.Entity.JobCategory;
import com.webjob.application.repository.CompanyRepository;
import com.webjob.application.repository.JobRepository;
import com.webjob.application.repository.SkillRepository;
import com.webjob.application.service.OutBox.OutboxService;
import com.webjob.application.utils.common.UtilFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.DisplayName;
import org.modelmapper.ModelMapper;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

//JUnit 5 + Mockito + AssertJ
@ExtendWith(MockitoExtension.class)
@DisplayName("JobService Unit Tests")
public class JobServiceTest {
    @Mock private SkillRepository skillRepository;
    @Mock private JobRepository jobRepository;
    @Mock private ModelMapper modelMapper;
    @Mock private CompanyRepository companyRepository;
    @Mock private CompanyService companyService;
    @Mock private PaymentService paymentService;
    @Mock private SecurityUtils securityUtils;
    @Mock private JobCategoryService jobCategoryService;
    @Mock private JobMapper jobMapper;
    @Mock private ApplicationEventPublisher publisher;
    @Mock private JobElasticsearchSearchService elasticsearchSearchService;
    @Mock private RedissonClient redissonClient;
    @Mock private OutboxService outboxService;

    @InjectMocks
    private JobService jobService;

    @Mock private RBucket<String> viewBucket;

    private User user;
    private Company company;
    private Job job;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(10L);
        company.setName("Example Company");
        company.setDeleted(false);
        company.setStatus(CompanyStatus.ACTIVE);

        user = new User();
        user.setId(100L);
        user.setEmail("hr@example.com");
        user.setCompany(company);

        job = new Job();
        job.setId(1L);
        job.setName("Java Developer");
        job.setCompany(company);
        job.setDeleted(false);
        job.setStatus(JobStatus.OPEN);
        job.setViewCount(0L);
        job.setAppliedCount(0);
        job.setJobSkills(new ArrayList<>());
        job.setEndDate(Instant.now().plusSeconds(86_400));


    }
    private JobRequest validJobRequest() {
        JobRequest request = new JobRequest();
        request.setName("Java Developer");
        request.setSalaryMin(1_000.0);
        request.setSalaryMax(2_000.0);
        request.setJobCategoryId(5L);
        request.setSkills(new ArrayList<>());
        return request;
    }

    private void mockHrWithActiveCompany() {
        // Điều chỉnh Role/Role entity theo model thực tế của dự án.
        Role role = new Role();
        role.setCode("HR");
        user.setRole(role);
        when(securityUtils.getCurrentUser()).thenReturn(user);
    }

//    Nhóm A — validation và kỹ năng
    @Test
    void validateSalary_shouldAcceptNullBounds() {
        jobService.validateSalary(null, 2_000.0);
        jobService.validateSalary(1_000.0, null);
        jobService.validateSalary(null, null);
    }

    @Test
    void validateSalary_shouldAcceptEqualBounds() {
        jobService.validateSalary(1_000.0, 1_000.0);
    }

    @Test
    void validateSalary_shouldRejectMinimumGreaterThanMaximum() {
        assertThatThrownBy(() -> jobService.validateSalary(2_000.0, 1_000.0))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Salary minimum cannot be greater than salary maximum.");
    }

    @Test
    void checkNameJobCreate_shouldRejectExistingName() {
        when(jobRepository.existsByNameIgnoreCaseAndDeletedFalse("Java Developer"))
                .thenReturn(true);

        assertThatThrownBy(() -> jobService.checkNameJobCreate("Java Developer"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Java Developer");

        verify(jobRepository).existsByNameIgnoreCaseAndDeletedFalse("Java Developer");
    }

    @Test
    void checkNameJobUpdate_shouldExcludeCurrentJobFromDuplicateCheck() {
        when(jobRepository.existsByNameIgnoreCaseAndDeletedFalseAndIdNot(
                "Java Developer", 1L))
                .thenReturn(false);

        jobService.checkNameJobUpdate(1L, "Java Developer");

        verify(jobRepository)
                .existsByNameIgnoreCaseAndDeletedFalseAndIdNot("Java Developer", 1L);
    }

    @Test
    void getById_shouldThrowWhenJobDoesNotExistOrIsDeleted() {
        when(jobRepository.findByIdAndDeletedFalse(999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobService.getById(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("999");
    }

    @Test
    void buildJobSkills_shouldReturnEmptyListWhenRequestHasNoSkills() {
        JobRequest request = validJobRequest();
        request.setSkills(null);

        List<JobSkill> result = jobService.buildJobSkills(job, request);

        assertThat(result).isEmpty();
        verifyNoInteractions(skillRepository);
    }

    @Test
    void buildJobSkills_shouldMapSkillAttributesCorrectly() {
        Skill skill = new Skill();
        skill.setId(7L);

        JobRequest request = validJobRequest();

        JobRequest.JobSkillRequest item = new JobRequest.JobSkillRequest();
        item.setSkillId(7L);
        item.setRequired(true);
        item.setPriority(1);
        item.setExperienceYear(2);
        item.setLevel(SkillLevel.ADVANCED);

        request.setSkills(List.of(item));
        when(skillRepository.findAllById(Set.of(7L))).thenReturn(List.of(skill));

        List<JobSkill> result = jobService.buildJobSkills(job, request);

        assertThat(result).hasSize(1);

        JobSkill actual = result.get(0);
        assertThat(actual.getJob()).isSameAs(job);
        assertThat(actual.getSkill()).isSameAs(skill);
        assertThat(actual.getRequired()).isTrue();
        assertThat(actual.getPriority()).isEqualTo(1);
        assertThat(actual.getExperienceYear()).isEqualTo(2);
        assertThat(actual.getLevel()).isEqualTo(SkillLevel.ADVANCED);
    }

    @Test
    void buildJobSkills_shouldRejectSkillIdThatWasNotFound() {
        JobRequest request = validJobRequest();

        JobRequest.JobSkillRequest item = new JobRequest.JobSkillRequest();
        item.setSkillId(999L);
        request.setSkills(List.of(item));

        when(skillRepository.findAllById(Set.of(999L))).thenReturn(List.of());

        assertThatThrownBy(() -> jobService.buildJobSkills(job, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("999");
    }

//    Nhóm B — tạo và cập nhật công việc
    @Test
    void createJob_shouldRejectDuplicateNameBeforeSaving() {
        JobRequest request = validJobRequest();

        when(jobRepository.existsByNameIgnoreCaseAndDeletedFalse(request.getName()))
                .thenReturn(true);

        assertThatThrownBy(() -> jobService.createJob(request))
                .isInstanceOf(IllegalArgumentException.class);

        verify(jobRepository, never()).save(any(Job.class));
        verifyNoInteractions(publisher, outboxService);
    }

    @Test
    void createJob_shouldRejectInvalidSalaryBeforeSaving() {
        JobRequest request = validJobRequest();
        request.setSalaryMin(3_000.0);
        request.setSalaryMax(2_000.0);

        when(jobRepository.existsByNameIgnoreCaseAndDeletedFalse(request.getName()))
                .thenReturn(false);

        assertThatThrownBy(() -> jobService.createJob(request))
                .isInstanceOf(BadRequestException.class);

        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    void createJob_shouldSaveJobAndPublishIndexOutbox() {
        JobRequest request = validJobRequest();
        JobCategory category = new JobCategory();

        mockHrWithActiveCompany();

        when(jobRepository.existsByNameIgnoreCaseAndDeletedFalse(request.getName()))
                .thenReturn(false);
        when(jobCategoryService.findById(5L)).thenReturn(category);
        when(modelMapper.map(request, Job.class)).thenReturn(job);
        when(jobRepository.save(any(Job.class))).thenReturn(job);
        when(jobMapper.toDocument(job)).thenReturn(new JobDocument());
        when(jobMapper.toResponse(job)).thenReturn(new JobResponse());

        JobResponse response = jobService.createJob(request);

        assertThat(response).isNotNull();
        assertThat(job.getCompany()).isSameAs(company);
        assertThat(job.getJobCategory()).isSameAs(category);
        assertThat(job.getViewCount()).isZero();
        assertThat(job.getAppliedCount()).isZero();

        verify(jobRepository).save(job);
        verify(publisher).publishEvent(any(JobCreatedEvent.class));
        verify(outboxService).save(argThat(dto ->
                dto.getAggregateType().equals("JOB")
                        && dto.getAggregateId().equals("1")
                        && dto.getEventType() == OutboxEventType.JOB_INDEX_CREATED
        ));
    }

    @Test
    void updateJob_shouldRejectWhenJobBelongsToAnotherCompany() {
        Company anotherCompany = new Company();
        anotherCompany.setId(20L);
        job.setCompany(anotherCompany);

        mockHrWithActiveCompany();

        when(jobRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(job));
        when(jobRepository.existsByNameIgnoreCaseAndDeletedFalseAndIdNot(
                "Java Developer", 1L)).thenReturn(false);

        assertThatThrownBy(() -> jobService.updateJob(1L, validJobRequest()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("not allowed");

        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    void updateJob_shouldReplaceSkillsWhenSkillsAreProvided() {
        JobSkill oldSkill = new JobSkill();
        job.setJobSkills(new ArrayList<>(List.of(oldSkill)));

        JobRequest request = validJobRequest();
        request.setSkills(new ArrayList<>());

        mockHrWithActiveCompany();

        when(jobRepository.existsByNameIgnoreCaseAndDeletedFalseAndIdNot(
                request.getName(), 1L)).thenReturn(false);
        when(jobRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(job));
        when(jobRepository.save(job)).thenReturn(job);
        when(jobMapper.toResponse(job)).thenReturn(new JobResponse());
        when(jobMapper.toDocument(job)).thenReturn(new JobDocument());

        JobResponse response = jobService.updateJob(1L, request);

        assertThat(response).isNotNull();
        assertThat(job.getJobSkills()).isEmpty();

        verify(jobRepository).flush();
        verify(outboxService).save(argThat(dto ->
                dto.getEventType() == OutboxEventType.JOB_INDEX_UPDATED
        ));
    }

    @Test
    void updateJob_shouldKeepExistingSkillsWhenSkillsAreNull() {
        JobSkill oldSkill = new JobSkill();
        job.setJobSkills(new ArrayList<>(List.of(oldSkill)));

        JobRequest request = validJobRequest();
        request.setSkills(null);

        mockHrWithActiveCompany();

        when(jobRepository.existsByNameIgnoreCaseAndDeletedFalseAndIdNot(
                request.getName(), 1L)).thenReturn(false);
        when(jobRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(job));
        when(jobRepository.save(job)).thenReturn(job);
        when(jobMapper.toResponse(job)).thenReturn(new JobResponse());
        when(jobMapper.toDocument(job)).thenReturn(new JobDocument());

        jobService.updateJob(1L, request);

        assertThat(job.getJobSkills()).containsExactly(oldSkill);
        verify(jobRepository, never()).flush();
    }

//    Nhóm C — xóa mềm và khôi phục công việc
    @Test
    void deleteJob_shouldSoftDeleteAndCreateDeletedIndexOutbox() {
        mockHrWithActiveCompany();

        when(jobRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(job));
        when(jobRepository.save(job)).thenReturn(job);
//        when(jobMapper.toDocument(job)).thenReturn(mock(JobDocument.class));

        jobService.deleteJob(1L);

        assertThat(job.isDeleted()).isTrue();
        assertThat(job.getDeletedAt()).isNotNull();
        assertThat(job.getDeletedBy()).isEqualTo(user.getEmail());
        assertThat(job.getStatus()).isEqualTo(JobStatus.CLOSED);

        verify(jobRepository).save(job);
        verify(outboxService).save(argThat(dto ->
                dto.getEventType() == OutboxEventType.JOB_INDEX_DELETED
        ));
    }

    @Test
    void deleteJob_shouldRejectJobOwnedByAnotherCompany() {
        Company anotherCompany = new Company();
        anotherCompany.setId(20L);
        job.setCompany(anotherCompany);

        mockHrWithActiveCompany();
        when(jobRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> jobService.deleteJob(1L))
                .isInstanceOf(ForbiddenException.class);

        verify(jobRepository, never()).save(any(Job.class));
        verifyNoInteractions(outboxService);
    }

    @Test
    void restoreJob_shouldRejectJobWithoutCompany() {
        mockHrWithActiveCompany();

        job.setDeleted(true);
        job.setCompany(null);

        when(jobRepository.findByIdAndDeletedTrue(1L)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> jobService.restoreJob(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("công ty");

        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    void restoreJob_shouldRejectJobWithoutCategory() {
        mockHrWithActiveCompany();

        job.setDeleted(true);
        job.setJobCategory(null);

        when(jobRepository.findByIdAndDeletedTrue(1L)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> jobService.restoreJob(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("danh mục");

        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    void restoreJob_shouldSetExpiredStatusWhenEndDateIsInPast() {
        mockHrWithActiveCompany();

        job.setDeleted(true);
        job.setEndDate(Instant.now().minusSeconds(60));

        JobCategory category = new JobCategory();
        job.setJobCategory(category);

        when(jobRepository.findByIdAndDeletedTrue(1L)).thenReturn(Optional.of(job));
        when(jobRepository.save(job)).thenReturn(job);
//        when(jobMapper.toDocument(job)).thenReturn(mock(JobDocument.class));

        jobService.restoreJob(1L);

        assertThat(job.isDeleted()).isFalse();
        assertThat(job.getDeletedAt()).isNull();
        assertThat(job.getDeletedBy()).isNull();
        assertThat(job.getStatus()).isEqualTo(JobStatus.EXPIRED);

        verify(outboxService).save(argThat(dto ->
                dto.getEventType() == OutboxEventType.JOB_INDEX_RESTORED
        ));
    }

    @Test
    void restoreJob_shouldSetOpenStatusWhenEndDateIsInFuture() {
        mockHrWithActiveCompany();

        job.setDeleted(true);
        job.setEndDate(Instant.now().plusSeconds(3_600));
        job.setJobCategory(new JobCategory());

        when(jobRepository.findByIdAndDeletedTrue(1L)).thenReturn(Optional.of(job));
        when(jobRepository.save(job)).thenReturn(job);
//        when(jobMapper.toDocument(job)).thenReturn(mock(JobDocument.class));

        jobService.restoreJob(1L);

        assertThat(job.isDeleted()).isFalse();
        assertThat(job.getStatus()).isEqualTo(JobStatus.OPEN);
    }


//    Nhóm D — Elasticsearch và fallback về database
@Test
void searchJobClient_shouldReturnEmptyPageWhenElasticsearchHasNoIds() {
    JobFilterClient filter = new JobFilterClient();

    ElasticsearchSearchResult searchResult = mock(ElasticsearchSearchResult.class);
    when(searchResult.getTotal()).thenReturn(0L);
    when(searchResult.getIds()).thenReturn(List.of());

    when(elasticsearchSearchService.searchClient(0, 10, filter))
            .thenReturn(searchResult);

    Page<Job> result = jobService.searchJobClient(0, 10, filter);

    assertThat(result.getContent()).isEmpty();
    assertThat(result.getTotalElements()).isZero();

    verifyNoInteractions(jobRepository);
}

    @Test
    void searchJobClient_shouldPreserveElasticsearchIdOrder() {
        JobFilterClient filter = new JobFilterClient();

        Job first = new Job();
        first.setId(3L);

        Job second = new Job();
        second.setId(1L);

        ElasticsearchSearchResult searchResult = mock(ElasticsearchSearchResult.class);
        when(searchResult.getTotal()).thenReturn(2L);
        when(searchResult.getIds()).thenReturn(List.of(3L, 1L));

        when(elasticsearchSearchService.searchClient(0, 10, filter))
                .thenReturn(searchResult);
        when(jobRepository.findByIdIn(List.of(3L, 1L)))
                .thenReturn(List.of(second, first));

        Page<Job> result = jobService.searchJobClient(0, 10, filter);

        assertThat(result.getContent())
                .extracting(Job::getId)
                .containsExactly(3L, 1L);
        assertThat(result.getTotalElements()).isEqualTo(2L);
    }

    @Test
    void searchJobClient_shouldFallbackToDatabaseWhenElasticsearchThrows() {
        JobFilterClient filter = new JobFilterClient();
        filter.setKeyword("Java");
        filter.setSort(JobSort.NEWEST);

        when(elasticsearchSearchService.searchClient(0, 10, filter))
                .thenThrow(new RuntimeException("Elasticsearch unavailable"));

        Page<Job> expected = new PageImpl<>(
                List.of(job),
                PageRequest.of(0, 10),
                1
        );

        when(jobRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(expected);
        when(jobMapper.toSort(JobSort.NEWEST)).thenReturn(Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Job> result = jobService.searchJobClient(0, 10, filter);

        assertThat(result.getContent()).containsExactly(job);
        assertThat(result.getTotalElements()).isEqualTo(1L);

        verify(jobRepository).findAll(
                any(Specification.class),
                any(Pageable.class)
        );
    }


//    Nhóm E — lượt xem và chống tăng lượt xem trùng lặp
@Test
void increaseViewIfNeeded_shouldIncrementOnlyOnFirstView() {
    when(redissonClient.<String>getBucket("job:view:1:user:100"))
            .thenReturn(viewBucket);
    when(viewBucket.trySet("1", 24L, TimeUnit.HOURS))
            .thenReturn(true);

    // Method repository là void, vì vậy chỉ cần xác minh lời gọi.
    when(jobRepository.findByIdAndDeletedFalse(1L))
            .thenReturn(Optional.of(job));
    when(securityUtils.getCurrentUser()).thenReturn(user);
//    when(jobMapper.toDocument(any(Job.class)))
//            .thenReturn(mock(JobDocument.class));

    // detailJobId kích hoạt luồng tăng lượt xem.
    when(jobMapper.toResponse(job)).thenReturn(new JobResponse());

    jobService.detailJobId(1L);

    verify(jobRepository).increaseViewCount(1L);
    verify(outboxService).save(argThat(dto ->
            dto.getEventType()
                    == OutboxEventType.JOB_INDEX_VIEW_COUNT_INCREMENTED
    ));
}

    @Test
    void increaseViewIfNeeded_shouldNotIncrementWhenUserAlreadyViewed() {
        when(redissonClient.<String>getBucket("job:view:1:user:100"))
                .thenReturn(viewBucket);
        when(viewBucket.trySet("1", 24L, TimeUnit.HOURS))
                .thenReturn(false);

        when(jobRepository.findByIdAndDeletedFalse(1L))
                .thenReturn(Optional.of(job));
        when(securityUtils.getCurrentUser()).thenReturn(user);
        when(jobMapper.toResponse(job)).thenReturn(new JobResponse());

        jobService.detailJobId(1L);

        verify(jobRepository, never()).increaseViewCount(anyLong());
        verifyNoInteractions(outboxService);
    }

    @Test
    void detailJobId_shouldNotIncrementViewForAnonymousUser() {
        when(securityUtils.getCurrentUser()).thenReturn(null);
        when(jobRepository.findByIdAndDeletedFalse(1L))
                .thenReturn(Optional.of(job));
        when(jobMapper.toResponse(job)).thenReturn(new JobResponse());

        jobService.detailJobId(1L);

        verifyNoInteractions(redissonClient);
        verify(jobRepository, never()).increaseViewCount(anyLong());
    }

//    Nhóm F — phân quyền và dữ liệu phân trang

    @Test
    void validateCompany_shouldRejectHrWithoutCompany() {
        mockHrWithActiveCompany();
        user.setCompany(null);

        assertThatThrownBy(() -> jobService.getJobsCompany(
                0, 10, new JobFilterHrRequest()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("No company");
    }

    @Test
    void validateCompany_shouldRejectDeletedCompany() {
        mockHrWithActiveCompany();
        company.setDeleted(true);

        assertThatThrownBy(() -> jobService.getJobsCompany(
                0, 10, new JobFilterHrRequest()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("deleted");
    }

    @Test
    void validateCompany_shouldRejectInactiveCompany() {
        mockHrWithActiveCompany();
        company.setStatus(CompanyStatus.INACTIVE);

        assertThatThrownBy(() -> jobService.getJobsCompany(
                0, 10, new JobFilterHrRequest()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("inactive");
    }

    @Test
    void searchJob_shouldNormalizeNonPositivePageAndSize() {
        JobFilterClient filter = new JobFilterClient();

        ElasticsearchSearchResult searchResult = mock(ElasticsearchSearchResult.class);

        when(searchResult.getTotal()).thenReturn(0L);
        when(searchResult.getIds()).thenReturn(List.of());

        when(elasticsearchSearchService.searchClient(0, 10, filter))
                .thenReturn(searchResult);

        ResponseDTO<List<JobResponse>> response =
                jobService.searchJob(0, 0, filter);

        assertThat(response).isNotNull();

        verify(elasticsearchSearchService)
                .searchClient(0, 10, filter);
    }

    @Test
    void searchJob_shouldHandleNullFilter() {
        ElasticsearchSearchResult searchResult =
                mock(ElasticsearchSearchResult.class);

        when(searchResult.getTotal()).thenReturn(0L);
        when(searchResult.getIds()).thenReturn(List.of());

        when(elasticsearchSearchService.searchClient(
                eq(0), eq(10), any(JobFilterClient.class)))
                .thenReturn(searchResult);

        ResponseDTO<List<JobResponse>> response =
                jobService.searchJob(1, 10, null);

        assertThat(response).isNotNull();

        verify(elasticsearchSearchService)
                .searchClient(eq(0), eq(10), any(JobFilterClient.class));
    }

//    Nhóm G — ủy quyền nghiệp vụ và chuyển đổi kết quả

    @Test
    void getJobApplicantInfo_shouldDelegateToPaymentService() {
        mockHrWithActiveCompany();

        JobApplicantInfoResponse expected = mock(JobApplicantInfoResponse.class);
        when(paymentService.getJobApplicantInfo(100L, 1L)).thenReturn(expected);

        JobApplicantInfoResponse actual = jobService.getJobApplicantInfo(1L);

        assertThat(actual).isSameAs(expected);
        verify(paymentService).getJobApplicantInfo(100L, 1L);
    }

    @Test
    void getJobApplicantInfo_shouldRejectHrWithoutCompany() {
        mockHrWithActiveCompany();
        user.setCompany(null);

        assertThatThrownBy(() -> jobService.getJobApplicantInfo(1L))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(paymentService);
    }

    @Test
    void convertToJobResponseDTO_shouldMapItemsAndPaginationMetadata() {
        Job anotherJob = new Job();
        anotherJob.setId(2L);

        JobResponse firstResponse = mock(JobResponse.class);
        JobResponse secondResponse = mock(JobResponse.class);

        when(jobMapper.toResponse(job)).thenReturn(firstResponse);
        when(jobMapper.toResponse(anotherJob)).thenReturn(secondResponse);

        Page<Job> page = new PageImpl<>(
                List.of(job, anotherJob),
                PageRequest.of(1, 2),
                6
        );

        ResponseDTO<List<JobResponse>> result =
                jobService.convertToJobResponseDTO(page);

        assertThat(result).isNotNull();

        verify(jobMapper).toResponse(job);
        verify(jobMapper).toResponse(anotherJob);
    }
}
