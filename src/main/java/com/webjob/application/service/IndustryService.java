package com.webjob.application.service;

import com.webjob.application.dto.Request.IndustryFilter;
import com.webjob.application.dto.Request.IndustryRequest;
import com.webjob.application.dto.Request.JobFilterHrRequest;
import com.webjob.application.dto.Response.IndustryResponse;
import com.webjob.application.dto.Response.JobResponse;
import com.webjob.application.dto.Response.MetaDTO;
import com.webjob.application.dto.Response.ResponseDTO;
import com.webjob.application.enums.CompanyStatus;
import com.webjob.application.exception.Customs.BadRequestException;
import com.webjob.application.exception.Customs.ConflictException;
import com.webjob.application.exception.Customs.ResourceNotFoundException;
import com.webjob.application.exception.Customs.UnauthorizedException;
import com.webjob.application.models.Entity.Company;
import com.webjob.application.models.Entity.Industry;
import com.webjob.application.models.Entity.Job;
import com.webjob.application.models.Entity.User;
import com.webjob.application.repository.IndustryRepository;
import com.webjob.application.service.Specification.IndustrySpecification;
import com.webjob.application.service.Specification.JobSpecification;
import com.webjob.application.utils.common.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IndustryService {
    private final IndustryRepository industryRepository;
    private final ModelMapper modelMapper;
    private final SecurityUtils securityUtils;

    @Transactional
    public IndustryResponse create(IndustryRequest request) {
        if (industryRepository.existsByNameIgnoreCaseAndDeletedFalse(request.getName())) {
            throw new BadRequestException("Tên ngành nghề đã tồn tại,vui lòng tạo tên khác !");
        }
        Industry industry = modelMapper.map(request, Industry.class);
        return modelMapper.map(industryRepository.save(industry), IndustryResponse.class);
    }

    @Transactional
    public IndustryResponse update(Long id, IndustryRequest request) {

        Industry industry = industryRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Industry not found"));
        Industry existed = industryRepository
                .findByNameIgnoreCaseAndDeletedFalse(request.getName())
                .orElse(null);

        if (existed != null && !existed.getId().equals(id)) {
            throw new BadRequestException("Tên ngành nghề đã tồn tại");
        }

        industry.setName(request.getName());
        industry.setActive(request.getActive());
        return modelMapper.map(industryRepository.save(industry),IndustryResponse.class);
    }
    public IndustryResponse getById(Long id) {
        Industry industry = industryRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Industry not found"));

        return modelMapper.map(industry,IndustryResponse.class);
    }
    @Transactional
    public void delete(Long id) {

        Industry industry = industryRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Industry not found"));

        industry.setDeleted(true);
        industry.setDeletedAt(Instant.now());
        User user=securityUtils.getCurrentUser();
        industry.setDeletedBy(user.getEmail());
        industryRepository.save(industry);
    }

    public List<IndustryResponse> getAll() {
        return industryRepository.findAllByDeletedFalseOrderByNameAsc()
                .stream()
                .map(industry -> modelMapper.map(industry,IndustryResponse.class))
                .toList();
    }

    public IndustryResponse restore(Long id) {

        Industry industry = industryRepository.findByIdAndDeletedTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Industry not found"));

        industry.setDeleted(false);
        industry.setDeletedAt(null);
        industry.setDeletedBy(null);
        industryRepository.save(industry);
        return modelMapper.map(industryRepository.save(industry),IndustryResponse.class);
    }
    public ResponseDTO< List<IndustryResponse>> getIndustries(int page, int size, IndustryFilter request) {
        request = request == null ? new IndustryFilter() : request;
        // Bỏ hoàn toàn try-catch, chỉ giữ lại logic kiểm tra số âm/bằng 0
        if (page <= 0) {
            page = 1;
        }
        if (size <= 0) {
            size = 10;
        }
        Page<Industry> pagelist = getIndustriesForAdmin(page-1,size,request);
        return convertToIndustryResponseDTO(pagelist);

    }
    public Page<Industry> getIndustriesForAdmin(int page, int size, IndustryFilter request) {
        Pageable pageable = PageRequest.of(page, size,Sort.by(
                Sort.Direction.ASC, "name"));
        Specification<Industry> specification =
                Specification.where(IndustrySpecification.hasKeyword(request.getKeyword()))
                .and(IndustrySpecification.hasStatus(request.getActive()))
                .and(IndustrySpecification.isDeleted(request.getDeleted()))
                .and(IndustrySpecification.createdFrom(request.getCreatedFrom()))
                .and(IndustrySpecification.createdTo(request.getCreatedTo()));
        return industryRepository.findAll(specification, pageable);
    }

    public ResponseDTO<List<IndustryResponse>> convertToIndustryResponseDTO(Page<Industry> pagelist) {

        int currentpage = pagelist.getNumber() + 1;
        int pagesize = pagelist.getSize();
        int totalpage = pagelist.getTotalPages();
        Long totalItem = pagelist.getTotalElements();


        MetaDTO metaDTO = new MetaDTO(currentpage, pagesize, totalpage, totalItem);


        List<Industry> jobsList = pagelist.getContent();
        List<IndustryResponse> list = jobsList.stream()
                .map(industry -> modelMapper.map(industry,IndustryResponse.class))
                .toList();
        // 4. Trả về kết quả
        return new ResponseDTO<>(metaDTO, list);
    }




}
