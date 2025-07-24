package com.webjob.application.Controller;


import com.webjob.application.Models.Company;
import com.webjob.application.Models.Request.CompanyDTO;
import com.webjob.application.Models.Request.SearchCompanyDTO;
import com.webjob.application.Models.Response.ApiResponse;
import com.webjob.application.Models.Response.MetaDTO;
import com.webjob.application.Models.Response.ResponseDTO;
import com.webjob.application.Models.User;
import com.webjob.application.Services.CompanyService;
import com.webjob.application.Services.UserService;
import jakarta.validation.Valid;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
public class CompanyController {
    private final CompanyService companyService;
    private final ModelMapper modelMapper;
    private final UserService userService;


    public CompanyController(CompanyService companyService, ModelMapper modelMapper, UserService userService) {
        this.companyService = companyService;
        this.modelMapper = modelMapper;
        this.userService = userService;
    }
    @PostMapping("/create/companies")
    public ResponseEntity<?> create(@Valid @RequestBody CompanyDTO companyDTO){
        Company company=modelMapper.map(companyDTO,Company.class);
        Company respond=companyService.handle(company);
        ApiResponse<Company> response = new ApiResponse<>(
                HttpStatus.CREATED.value(),
                null,
                "Create Company successful",
                respond

        );
        return new ResponseEntity<>(response, HttpStatus.CREATED);

    }
    @GetMapping("/all/companies")
    public ResponseEntity<?> GetallCompanies(){
        List<Company> list=companyService.getAll();
        if(list.isEmpty()){

            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }
        ApiResponse<List<Company>> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "GEt All Company successful",
                list

        );
        return ResponseEntity.ok(response);
    }
    @PutMapping("edit/company/{id}")
    public ResponseEntity<?> EditCompany(@PathVariable Long id,@Valid @RequestBody CompanyDTO companyDTO){
        try {
            companyService.checkByID(id);
            Optional<Company> ops=companyService.getbyID(id);
                Company edit=ops.get();
                modelMapper.map(companyDTO,edit);
                companyService.handle(edit);
            ApiResponse<Company> response = new ApiResponse<>(
                    HttpStatus.OK.value(),
                    null,
                    "Edit Company successful",
                    edit

            );
                return ResponseEntity.ok(response);

        }catch (IllegalArgumentException ex){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());

        }
    }
    @GetMapping("/detail/company/{id}")
    public ResponseEntity<?> getCompanybyID(@PathVariable Long id){
        try {
            companyService.checkByID(id);
            Optional<Company> findId=companyService.getbyID(id);
            Company detail=findId.get();

            ApiResponse<Company> response = new ApiResponse<>(
                    HttpStatus.OK.value(),
                    null,
                    "Edit Company successful",
                    detail

            );
            return ResponseEntity.ok(response);

        }catch (IllegalArgumentException ex){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());

        }


    }
    @DeleteMapping("/delete/company/{id}")
    public ResponseEntity<?> deleteCompanybyId(@PathVariable Long id) {
        try {
            companyService.checkByID(id);
            Optional<Company> canfind =companyService.getbyID(id);
            Company delete = canfind.get();
            companyService.delete(delete);
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body("Delete Successful Company with "+id);

        } catch (
                IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        }
    }
    @GetMapping("api/companies")
    public ResponseEntity<?> getallPageList(@RequestParam(value ="page",defaultValue = "0")String pageparam){
        int page=0;
        int size=5;
        try {
            page = Integer.parseInt(pageparam);
            if (page <= 0)
                page = 1;
        } catch (NumberFormatException e) {
            // Nếu người dùng nhập sai, mặc định về trang đầu
            page = 1;
        }
        Page<Company> companypage=companyService.getallPage(page-1,size);
        int currentpage=companypage.getNumber()+1;// Trang hiện tại (0-indexed)
        int pageSize=companypage.getSize();// Kích thước trang
        int totalPages = companypage.getTotalPages(); // Tổng số trang
        long totalItems = companypage.getTotalElements(); // Tổng số phần tử
        MetaDTO metaDTO=new MetaDTO(currentpage,pageSize,totalPages,totalItems);
        ResponseDTO<?> responseDTO =new ResponseDTO<>(metaDTO,companypage.getContent());
        ApiResponse<?> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Fetch Company successful",
                responseDTO

        );

        return ResponseEntity.ok(response);

    }
    @GetMapping("/api/companies/search")
    public ResponseEntity<?> searchCompanies(@Valid SearchCompanyDTO searchCompanyDTO) {
        int page=0;
        try {
            page = Integer.parseInt(searchCompanyDTO.getPage());
            if (page <= 0)
                page = 1;
        } catch (NumberFormatException e) {
            // Nếu người dùng nhập sai, mặc định về trang đầu
            page = 1;
        }


        // Gọi service để thực hiện tìm kiếm
        Page<Company> companyPage = companyService.searchCompanies(page-1,searchCompanyDTO);

        // Tạo meta thông tin cho response
        int currentPage = companyPage.getNumber() + 1;
        int pageSize = companyPage.getSize();
        int totalPages = companyPage.getTotalPages();
        long totalItems = companyPage.getTotalElements();

        MetaDTO metaDTO = new MetaDTO(currentPage, pageSize, totalPages, totalItems);
        ResponseDTO<?> responsePageDTO = new ResponseDTO<>(metaDTO, companyPage.getContent());
        ApiResponse<?> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Fetch Company successful",
                responsePageDTO
        );

        return ResponseEntity.ok(response);
    }
}
