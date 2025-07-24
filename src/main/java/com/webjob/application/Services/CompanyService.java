package com.webjob.application.Services;


import com.webjob.application.Models.Company;
import com.webjob.application.Models.Request.SearchCompanyDTO;
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
        Pageable pageable= PageRequest.of(page,limit);
        return companyRepository.findAll(pageable);
    }
    public Page<Company> searchCompanies(int page, SearchCompanyDTO companyDTO){
        Specification<Company> specification = Specification.where(CompanySpecification.hasName(companyDTO.getName())
                .and(CompanySpecification.hasAddress(companyDTO.getAddress()))
                .and(CompanySpecification.hasDescription(companyDTO.getDescription()))

        );

        Sort.Direction direction="DESC".equalsIgnoreCase(companyDTO.getSortOrder())?Sort.Direction.DESC:Sort.Direction.ASC;
        Sort sort=Sort.by(direction,companyDTO.getSortBy()!=null ? companyDTO.getSortBy() :"name");
        Pageable pageable= PageRequest.of(page,companyDTO.getLimit(),sort);
        return companyRepository.findAll(specification,pageable);


    }
}
