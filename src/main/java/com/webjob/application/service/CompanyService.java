package com.webjob.application.service;


import com.webjob.application.dto.Request.CompanyAdminSearchRequest;
import com.webjob.application.dto.Request.CompanyDTO;
import com.webjob.application.dto.Request.CompanySearchRequest;
import com.webjob.application.dto.Response.*;
import com.webjob.application.enums.CompanyStatus;
import com.webjob.application.exception.Customs.CompanyAlreadyExistsException;
import com.webjob.application.exception.Customs.ResourceNotFoundException;
import com.webjob.application.models.Entity.Company;
import com.webjob.application.dto.Request.Search.SearchCompanyDTO;
import com.webjob.application.models.Entity.Industry;
import com.webjob.application.models.Entity.Job;
import com.webjob.application.models.Entity.User;
import com.webjob.application.repository.*;
import com.webjob.application.service.Specification.CompanySpecification;
import com.webjob.application.service.Specification.JobSpecification;
import com.webjob.application.utils.common.SecurityUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CompanyService {
    private final CompanyRepository companyRepository;
    private final ModelMapper modelMapper;

    private final UserRepository userRepository;

    private final SecurityUtils securityUtils;

    private final JobRepository jobRepository;
    private final FollowCompanyRepository followCompanyRepository;
    private final IndustryRepository industryRepository;


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
        if (page <= 0) {
            page = 1;
        }
        if (size <= 0) {
            size = 10;
        }
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(
                Sort.Direction.ASC, "name"));
        Specification<Company> specification = Specification.where(CompanySpecification.visible())
                .and(CompanySpecification.active())
                .and(CompanySpecification.hasKeyword(request.getKeyword()))
                .and(CompanySpecification.hasIndustry(request.getIndustry()))
                .and(CompanySpecification.employeeSizeGreaterThan(request.getMinEmployeeSize()))
                .and(CompanySpecification.employeeSizeLessThan(request.getMaxEmployeeSize()))
                .and(CompanySpecification.foundedYearFrom(request.getFoundedFrom()))
                .and(CompanySpecification.foundedYearTo(request.getFoundedTo()));
        Page<Company> result = companyRepository.findAll(specification, pageable);
        return convertToCompanyResponse(result);

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
        CompanyResponse response = new CompanyResponse();
        Company save = companyRepository.save(company);
        modelMapper.map(save, response);


        response.setJobCount(0);
        response.setFollowerCount(0);
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
        return response;


    }


    public CompanyResponse getById(Long id) {
        Company company = companyRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy công ty với id: " + id));
        CompanyResponse response = modelMapper.map(company, CompanyResponse.class);
        Integer jobCount = jobRepository.countByCompanyIdAndDeletedFalse(company.getId());
        Integer followerCount = followCompanyRepository.countByCompanyId(company.getId());
        response.setJobCount(jobCount == null ? 0 : jobCount);
        response.setFollowerCount(followerCount == null ? 0 : followerCount);
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
    }


}


