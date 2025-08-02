package com.webjob.application.Services;


import com.webjob.application.Models.Company;
import com.webjob.application.Models.Request.Search.SearchCompanyDTO;
import com.webjob.application.Models.Response.MetaDTO;
import com.webjob.application.Models.Response.ResponseDTO;
import com.webjob.application.Models.User;
import com.webjob.application.Repository.CompanyRepository;
import com.webjob.application.Repository.UserRepository;
import com.webjob.application.Services.Specification.CompanySpecification;
import jakarta.transaction.Transactional;
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
public class CompanyService {
    private final CompanyRepository companyRepository;
    private final ModelMapper modelMapper;

    private final UserRepository userRepository;







    public CompanyService(CompanyRepository companyRepository, ModelMapper modelMapper, UserRepository userRepository) {
        this.companyRepository = companyRepository;
        this.modelMapper = modelMapper;

        this.userRepository = userRepository;
    }
    @Transactional
    public Company handle(Company company){
        return companyRepository.save(company);
    }
    public List<Company> getAll(){
        return companyRepository.findAll();
    }
    public Optional<Company> getbyID(Long id){
        return companyRepository.findById(id);

    }
    public boolean checkByID(Long id){
        boolean exist=companyRepository.existsById(id);
        if(!exist){
            throw new IllegalArgumentException("Không tồn tại Company với ID: " + id);
        }
        return true;

    }
//    quan he 1-n
    @Transactional
    public void delete(Company company){
        List<User> userList=userRepository.findAllByCompany(company);
        for (User user : userList) {
            user.setCompany(null); // Bỏ liên kết
        }
        userRepository.saveAll(userList);
        companyRepository.delete(company);
    }

    public Page<Company> getallPage(int page,int limit){
        Pageable pageable= PageRequest.of(page-1,limit);
        return companyRepository.findAll(pageable);
    }
    public Page<Company> searchCompanies(int page, SearchCompanyDTO companyDTO){
        Specification<Company> specification = Specification.where(CompanySpecification.hasName(companyDTO.getName())
                .and(CompanySpecification.hasAddress(companyDTO.getAddress()))
                .and(CompanySpecification.hasDescription(companyDTO.getDescription()))

        );

        Sort.Direction direction="DESC".equalsIgnoreCase(companyDTO.getSortOrder())?Sort.Direction.DESC:Sort.Direction.ASC;
        Sort sort=Sort.by(direction,companyDTO.getSortBy()!=null ? companyDTO.getSortBy() :"name");
        Pageable pageable= PageRequest.of(page-1,companyDTO.getLimit(),sort);
        return companyRepository.findAll(specification,pageable);


    }
    public ResponseDTO<?> getPaginated(String pageparam, String type,SearchCompanyDTO searchCompanyDTO) {
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
    }


