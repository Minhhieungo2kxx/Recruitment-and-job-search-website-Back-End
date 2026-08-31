package com.webjob.application.service;


import com.webjob.application.document.CompanyDocument;
import com.webjob.application.document.JobDocument;
import com.webjob.application.dto.Request.*;
import com.webjob.application.dto.Response.*;
import com.webjob.application.dto.record.*;
import com.webjob.application.elasticsearch.company.CompanyElasticsearchSearchService;
import com.webjob.application.enums.CompanyStatus;
import com.webjob.application.enums.OutboxCategory;
import com.webjob.application.enums.OutboxEventType;
import com.webjob.application.exception.Customs.CompanyAlreadyExistsException;
import com.webjob.application.exception.Customs.ResourceNotFoundException;
import com.webjob.application.mapper.CompanyMapper;
import com.webjob.application.messaging.config.RabbitMQConfig;
import com.webjob.application.models.Entity.Company;
import com.webjob.application.dto.Request.Search.SearchCompanyDTO;
import com.webjob.application.models.Entity.Industry;
import com.webjob.application.models.Entity.Job;
import com.webjob.application.models.Entity.User;
import com.webjob.application.repository.*;
import com.webjob.application.service.OutBox.OutboxService;
import com.webjob.application.service.Specification.CompanySpecification;
import com.webjob.application.service.Specification.JobSpecification;
import com.webjob.application.utils.common.SecurityUtils;
import com.webjob.application.utils.common.UtilFormat;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompanyService {
    private final CompanyRepository companyRepository;
    private final ModelMapper modelMapper;

    private final UserRepository userRepository;

    private final SecurityUtils securityUtils;

    private final JobRepository jobRepository;
    private final FollowCompanyRepository followCompanyRepository;
    private final IndustryRepository industryRepository;
    private final CompanyElasticsearchSearchService elasticsearchSearchService;
    private final CompanyMapper companyMapper;
    private final ApplicationEventPublisher publisher;

    private final OutboxService outboxService;


    public Optional<Company> getbyID(Long id) {
        return companyRepository.findByIdAndDeletedFalse(id);

    }


    //    quan he 1-n
    public void delete(Company company) {
        List<User> userList = userRepository.findAllByCompanyAndDeletedFalse(company);
        for (User user : userList) {
            user.setCompany(null); // Bỏ liên kết
        }
        userRepository.saveAll(userList);
        companyRepository.delete(company);
    }


    public ResponseDTO<List<CompanyResponse>> getCompanyClient(int page, int size, CompanySearchRequest request) {
        if (request == null) {
            request = new CompanySearchRequest();
        }
        size = Math.min(Math.max(size, 1), 50);
        page = Math.max(page, 1);
        Page<Company> result=searchCompaniesClient(page-1,size,request);
        return convertToCompanyResponse(result);

    }
    public Page<Company> searchCompaniesClient(int page, int size, CompanySearchRequest request) {
        Pageable pageable = PageRequest.of(page, size);
        ElasticsearchSearchResult result;

        try {
            result = elasticsearchSearchService.searchCompanyClient(page, size, request);
            long total = (result != null) ? result.getTotal() : 0;
            if (result == null || result.getIds() == null || result.getIds().isEmpty()) {
                log.info("Elasticsearch search returned no companies. page={}, size={}, keyword={}",
                        page, size, request.getKeyword());
                return new PageImpl<>(Collections.emptyList(), pageable, total);
            }

            List<Company> companies = companyRepository.findByIdIn(result.getIds());

            List<Company> orderedCompanies = reorderCompanies(result.getIds(), companies);

            log.info("Elasticsearch company search successful. page={}, size={}, totalElements={}",
                    page, size, total);

            return new PageImpl<>(orderedCompanies, pageable, total);

        } catch (Exception e) {
            log.error("Elasticsearch company search failed, falling back to database. page={}, size={}, keyword={}, error={}",
                    page, size, (request != null ? request.getKeyword() : null), e.getMessage(), e);
            Pageable fallbackPageable = PageRequest.of(page, size, Sort.by(
                    Sort.Direction.ASC, "name"));
            Specification<Company> specification = Specification.where(CompanySpecification.visible())
                    .and(CompanySpecification.active())
                    .and(CompanySpecification.hasKeyword(request.getKeyword()))
                    .and(CompanySpecification.hasIndustry(request.getIndustry()))
                    .and(CompanySpecification.hasTaxCode(request.getTaxCode()))
                    .and(CompanySpecification.hasEmail(request.getEmail()))
                    .and(CompanySpecification.hasPhone(request.getPhone()))
                    .and(CompanySpecification.hasWebsite(request.getWebsite()))
                    .and(CompanySpecification.hasAddress(request.getAddress()))
                    .and(CompanySpecification.employeeSizeGreaterThan(request.getMinEmployeeSize()))
                    .and(CompanySpecification.employeeSizeLessThan(request.getMaxEmployeeSize()))
                    .and(CompanySpecification.foundedYearFrom(request.getFoundedFrom()))
                    .and(CompanySpecification.foundedYearTo(request.getFoundedTo()));
           return companyRepository.findAll(specification,fallbackPageable);

        }
    }
    private List<Company> reorderCompanies(List<Long> ids, List<Company> companies) {

        Map<Long, Company> companyMap = companies.stream()
                .collect(Collectors.toMap(
                        Company::getId,
                        Function.identity()
                ));

        return ids.stream()
                .map(companyMap::get)
                .filter(Objects::nonNull)
                .toList();
    }

    public ResponseDTO<List<CompanyResponse>> getCompanyAdmin(int page, int size, CompanyAdminSearchRequest request) {
        if (request == null) {
            request = new CompanyAdminSearchRequest();
        }
        if (page <= 0) {
            page = 1;
        }
        if (size <= 0) {
            size = 10;
        }
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(
                Sort.Direction.ASC, "name"));
        Specification<Company> specification = Specification.where(CompanySpecification.hasKeyword(request.getKeyword()))

                .and(CompanySpecification.hasStatuses(request.getStatuses()))
                .and(CompanySpecification.hasIndustry(request.getIndustry()))
                .and(CompanySpecification.isDeleted(request.getDeleted()))
                .and(CompanySpecification.hasTaxCode(request.getTaxCode()))
                .and(CompanySpecification.hasEmail(request.getEmail()))
                .and(CompanySpecification.hasPhone(request.getPhone()))
                .and(CompanySpecification.hasAddress(request.getAddress()))
                .and(CompanySpecification.employeeSizeGreaterThan(request.getMinEmployeeSize()))
                .and(CompanySpecification.employeeSizeLessThan(request.getMaxEmployeeSize()))
                .and(CompanySpecification.foundedYearFrom(request.getFoundedFrom()))
                .and(CompanySpecification.foundedYearTo(request.getFoundedTo()))
                .and(CompanySpecification.createdFrom(request.getCreatedFrom()))
                .and(CompanySpecification.createdTo(request.getCreatedTo()));
        Page<Company> result = companyRepository.findAll(specification, pageable);
        return convertToCompanyResponse(result);
    }


    public ResponseDTO<List<CompanyResponse>> convertToCompanyResponse(Page<Company> pagelist) {
        int currentpage = pagelist.getNumber() + 1;
        int pagesize = pagelist.getSize();
        int totalpage = pagelist.getTotalPages();
        Long totalItem = pagelist.getTotalElements();

        MetaDTO metaDTO = new MetaDTO(currentpage, pagesize, totalpage, totalItem);
        List<Company> companies = pagelist.getContent();
        List<CompanyResponse> list = companies.stream()
                .map(c -> CompanyResponse.builder()
                        .id(c.getId()).name(c.getName()).description(c.getDescription())
                        .address(c.getAddress()).logo(c.getLogo()).website(c.getWebsite())
                        .email(c.getEmail()).phone(c.getPhone()).employeeSize(c.getEmployeeSize())
                        .industry(c.getIndustry().getName()).foundedYear(c.getFoundedYear())
                        .taxCode(c.getTaxCode()).status(c.getStatus()).createdAt(c.getCreatedAt())
                        .updatedAt(c.getUpdatedAt()).deleted(c.getDeleted()).deletedAt(c.getDeletedAt())
                        .jobCount(jobRepository.countByCompanyIdAndDeletedFalse(c.getId()))
                        .followerCount(followCompanyRepository.countByCompanyId(c.getId()))
                        .build())
                .toList();
        // 4. Trả về kết quả
        return new ResponseDTO<>(metaDTO, list);
    }


    @Transactional
    public CompanyResponse createCompany(CompanyDTO companyDTO) {
        if (companyRepository.existsByName(companyDTO.getName())) {
            throw new CompanyAlreadyExistsException("Tên công ty '" + companyDTO.getName() + "' đã tồn tại trên hệ thống!");
        }
        Company company = modelMapper.map(companyDTO, Company.class);
        Industry industry=industryRepository.findByIdAndDeletedFalse(companyDTO.getIndustryId())
                .orElseThrow(() -> new ResourceNotFoundException("Industry not found " +companyDTO.getIndustryId()));
        company.setIndustry(industry);
        company.setTaxCode(UtilFormat.normalizeTaxCode(companyDTO.getTaxCode()));
        CompanyResponse response = new CompanyResponse();
        Company saved = companyRepository.save(company);
        modelMapper.map(saved, response);
        response.setJobCount(0);
        response.setFollowerCount(0);
        publishCompanyEventIndex(saved);
        return response;
    }



    @Transactional
    public CompanyResponse update(Long id, CompanyDTO companyDTO) {
        Company existingCompany = companyRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy công ty với id: " + id));

        modelMapper.map(companyDTO, existingCompany);
        if(companyDTO.getIndustryId() != null){
            Industry industry=industryRepository.findByIdAndDeletedFalse(companyDTO.getIndustryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Industry not found " +companyDTO.getIndustryId()));
            existingCompany.setIndustry(industry);

        }
        Company savedCompany = companyRepository.save(existingCompany);
        CompanyResponse response = modelMapper.map(savedCompany, CompanyResponse.class);

        Integer jobCount = jobRepository.countByCompanyIdAndDeletedFalse(savedCompany.getId());
        Integer followerCount = followCompanyRepository.countByCompanyId(savedCompany.getId());
        response.setJobCount(jobCount == null ? 0 : jobCount);
        response.setFollowerCount(followerCount == null ? 0 : followerCount);

        publishCompanyUpdatedIndex(savedCompany);
        return response;


    }


    public CompanyResponse getById(Long id) {
        Company company = companyRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy công ty với id: " + id));
        CompanyResponse response = modelMapper.map(company, CompanyResponse.class);
        Integer jobCount = jobRepository.countByCompanyIdAndDeletedFalse(company.getId());
        Integer followerCount = followCompanyRepository.countByCompanyId(company.getId());
        boolean followed = false;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(securityUtils.isAuthenticated()){
            Long userId = securityUtils.getCurrentUserId();
            followed = followCompanyRepository.existsByUserIdAndCompanyId(userId, company.getId());
        }
        response.setJobCount(jobCount == null ? 0 : jobCount);
        response.setFollowerCount(followerCount == null ? 0 : followerCount);
        response.setFollowed(followed);
        return response;

    }

    @Transactional
    public void deleteCompanyById(Long id) {
        Company company = companyRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with id: " + id));
        company.setDeleted(true);
        company.setDeletedAt(Instant.now());
        User user = securityUtils.getCurrentUser();
        company.setDeletedBy(user != null ? user.getEmail() : "SYSTEM");
        company.setStatus(CompanyStatus.INACTIVE);
        companyRepository.save(company);
        publishCompanyDeletedIndex(id);

    }


    @Transactional
    public void restoreCompanyById(Long id) {
        Company company = companyRepository.findByIdAndDeletedTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Deleted company not found with id: " + id));

        company.setDeleted(false);
        company.setDeletedAt(null);
        company.setDeletedBy(null);
        company.setStatus(CompanyStatus.ACTIVE);
        companyRepository.save(company);
        publishCompanyRestoreIndex(id);
    }
    public void publishCompanyEventIndex(Company saved) {
        CompanyDocument document = companyMapper.toDocument(saved);
        OutboxDTO dto = OutboxDTO.builder()
                .aggregateType("COMPANY")
                .aggregateId(saved.getId().toString())
                .category(OutboxCategory.COMPANY_INDEX)
                .eventType(OutboxEventType.COMPANY_INDEX_CREATED)
                .payload(document)
                .exchangeName(RabbitMQConfig.COMPANY_INDEX_EXCHANGE)
                .routingKey(RabbitMQConfig.COMPANY_INDEX_CREATED_ROUTING_KEY)
                .build();
        outboxService.save(dto);

    }

    public void publishCompanyUpdatedIndex(Company edit) {
        CompanyDocument document = companyMapper.toDocument(edit);
        OutboxDTO dto = OutboxDTO.builder()
                .aggregateType("COMPANY")
                .aggregateId(edit.getId().toString())
                .category(OutboxCategory.COMPANY_INDEX)
                .eventType(OutboxEventType.COMPANY_INDEX_UPDATED)
                .payload(document)
                .exchangeName(RabbitMQConfig.COMPANY_INDEX_EXCHANGE)
                .routingKey(RabbitMQConfig.COMPANY_INDEX_UPDATED_ROUTING_KEY)
                .build();
        outboxService.save(dto);


    }
    public void publishCompanyDeletedIndex(Long companyID) {
        CompanyDocument document =CompanyDocument.builder()
                .id(companyID)
                .build();
        OutboxDTO dto = OutboxDTO.builder()
                .aggregateType("COMPANY")
                .aggregateId(companyID.toString())
                .category(OutboxCategory.COMPANY_INDEX)
                .eventType(OutboxEventType.COMPANY_INDEX_DELETED)
                .payload(document)
                .exchangeName(RabbitMQConfig.COMPANY_INDEX_EXCHANGE)
                .routingKey(RabbitMQConfig.COMPANY_INDEX_DELETED_ROUTING_KEY)
                .build();
        outboxService.save(dto);
    }
    public void publishCompanyRestoreIndex(Long companyID) {

        CompanyDocument document =CompanyDocument.builder()
                .id(companyID)
                .build();
        OutboxDTO dto = OutboxDTO.builder()
                .aggregateType("COMPANY")
                .aggregateId(companyID.toString())
                .category(OutboxCategory.COMPANY_INDEX)
                .eventType(OutboxEventType.COMPANY_INDEX_RESTORED)
                .payload(document)
                .exchangeName(RabbitMQConfig.COMPANY_INDEX_EXCHANGE)
                .routingKey(RabbitMQConfig.COMPANY_INDEX_RESTORED_ROUTING_KEY)
                .build();
        outboxService.save(dto);

    }


}


