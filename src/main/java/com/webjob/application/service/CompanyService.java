package com.webjob.application.service;


import com.webjob.application.dto.Request.CompanyDTO;
import com.webjob.application.dto.Response.ApiResponse;
import com.webjob.application.models.Entity.Company;
import com.webjob.application.dto.Request.Search.SearchCompanyDTO;
import com.webjob.application.dto.Response.MetaDTO;
import com.webjob.application.dto.Response.ResponseDTO;
import com.webjob.application.models.Entity.User;
import com.webjob.application.repository.CompanyRepository;
import com.webjob.application.repository.UserRepository;
import com.webjob.application.service.Specification.CompanySpecification;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CompanyService {
    private final CompanyRepository companyRepository;
    private final ModelMapper modelMapper;

    private final UserRepository userRepository;



    public Company handle(Company company) {
        return companyRepository.save(company);
    }

    public List<Company> getAll() {
        return companyRepository.findAll();
    }

    public Optional<Company> getbyID(Long id) {
        return companyRepository.findById(id);

    }

    public boolean checkByID(Long id) {
        boolean exist = companyRepository.existsById(id);
        if (!exist) {
            throw new IllegalArgumentException("Không tồn tại Company với ID: " + id);
        }
        return true;

    }

    //    quan he 1-n
    public void delete(Company company) {
        List<User> userList = userRepository.findAllByCompany(company);
        for (User user : userList) {
            user.setCompany(null); // Bỏ liên kết
        }
        userRepository.saveAll(userList);
        companyRepository.delete(company);
    }

    public Page<Company> getallPage(int page, int limit) {
        Pageable pageable = PageRequest.of(page - 1, limit);
        return companyRepository.findAll(pageable);
    }

    public Page<Company> searchCompanies(int page, SearchCompanyDTO companyDTO) {
        Specification<Company> specification = Specification.where(CompanySpecification.hasName(companyDTO.getName())
                .and(CompanySpecification.hasAddress(companyDTO.getAddress()))
                .and(CompanySpecification.hasDescription(companyDTO.getDescription()))

        );

        Sort.Direction direction = "DESC".equalsIgnoreCase(companyDTO.getSortOrder()) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sort = Sort.by(direction, companyDTO.getSortBy() != null ? companyDTO.getSortBy() : "name");
        Pageable pageable = PageRequest.of(page - 1, companyDTO.getLimit(), sort);
        return companyRepository.findAll(specification, pageable);


    }

    public ResponseDTO<?> getPaginated(String pageparam, String type, SearchCompanyDTO searchCompanyDTO) {
        int page = 0;
        int size = 5;
        try {
            page = Integer.parseInt(pageparam);
            if (page <= 0)
                page = 1;
        } catch (NumberFormatException e) {
            // Nếu người dùng nhập sai, mặc định về trang đầu
            page = 1;
        }

        // Gọi service để thực hiện tìm kiếm
//        Page<Company> companyPage = companyService.searchCompanies(page-1,searchCompanyDTO);
        Page<Company> companyPage;
        if (type.equals("search")) {
            companyPage = searchCompanies(page, searchCompanyDTO);
        } else {
            companyPage = getallPage(page, size);
        }
        // Tạo meta thông tin cho response
        int currentPage = companyPage.getNumber() + 1;
        int pageSize = companyPage.getSize();
        int totalPages = companyPage.getTotalPages();
        long totalItems = companyPage.getTotalElements();

        MetaDTO metaDTO = new MetaDTO(currentPage, pageSize, totalPages, totalItems);
        ResponseDTO<?> responsePageDTO = new ResponseDTO<>(metaDTO, companyPage.getContent());
        return responsePageDTO;
    }
    @Transactional
    public ResponseEntity<?> createCompany( CompanyDTO companyDTO) {
        Company company = modelMapper.map(companyDTO, Company.class);
        Company respond =handle(company);
        ApiResponse<Company> response = new ApiResponse<>(
                HttpStatus.CREATED.value(),
                null,
                "Create Company successful",
                respond
        );
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    @Transactional
    public ResponseEntity<?> updateCompany(Long id, CompanyDTO companyDTO) {
        checkByID(id);
        Optional<Company> ops =getbyID(id);
        Company edit = ops.get();
        modelMapper.map(companyDTO, edit);
        handle(edit);
        ApiResponse<Company> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Edit Company successful",
                edit
        );
        return ResponseEntity.ok(response);
    }
    public ResponseEntity<?> getCompanybyID(@PathVariable Long id) {
        checkByID(id);
        Optional<Company> findId = getbyID(id);
        Company detail = findId.get();
        ApiResponse<Company> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Edit Company successful",
                detail
        );
        return ResponseEntity.ok(response);

    }

    @Transactional
    public ResponseEntity<?> deleteCompanybyId( Long id) {
        checkByID(id);
        Optional<Company> canfind = getbyID(id);
        Company delete = canfind.get();
        delete(delete);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body("Delete Successful Company with " + id);

    }
    public ResponseEntity<?> callPageList( String pageparam) {
        ResponseDTO<?> responseDTO = getPaginated(pageparam, "default", null);
        ApiResponse<?> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Fetch Company successful",
                responseDTO

        );
        return ResponseEntity.ok(response);
    }
    public ResponseEntity<?> searchCompanies(SearchCompanyDTO searchCompanyDTO) {
        ResponseDTO<?> responseDTO = getPaginated(searchCompanyDTO.getPage(), "search", searchCompanyDTO);
        ApiResponse<?> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Fetch Company successful",
                responseDTO
        );

        return ResponseEntity.ok(response);
    }
}


