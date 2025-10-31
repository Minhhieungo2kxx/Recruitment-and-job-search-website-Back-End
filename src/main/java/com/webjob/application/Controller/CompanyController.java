package com.webjob.application.Controller;


import com.webjob.application.Annotation.RateLimit;
import com.webjob.application.Model.Entity.Company;
import com.webjob.application.Dto.Request.CompanyDTO;
import com.webjob.application.Dto.Request.Search.SearchCompanyDTO;
import com.webjob.application.Dto.Response.ApiResponse;
import com.webjob.application.Dto.Response.ResponseDTO;
import com.webjob.application.Service.CompanyService;
import com.webjob.application.Service.UserService;
import jakarta.validation.Valid;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/companies") // base path chuẩn RESTful
public class CompanyController {
    private final CompanyService companyService;
    private final ModelMapper modelMapper;
    private final UserService userService;


    public CompanyController(CompanyService companyService, ModelMapper modelMapper, UserService userService) {
        this.companyService = companyService;
        this.modelMapper = modelMapper;
        this.userService = userService;
    }

    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "TOKEN")
    @PostMapping
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

    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "TOKEN")
    @PutMapping("/{id}")
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

    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "IP")
    @GetMapping("/{id}")
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
    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "TOKEN")
    @DeleteMapping("/{id}")
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
    @RateLimit(maxRequests = 10, timeWindowSeconds = 60, keyType = "IP")
    @GetMapping
    public ResponseEntity<?> getallPageList(@RequestParam(value ="page",defaultValue = "0")String pageparam){
        ResponseDTO<?> responseDTO =companyService.getPaginated(pageparam,"default",null);
        ApiResponse<?> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Fetch Company successful",
                responseDTO

        );

        return ResponseEntity.ok(response);
    }
    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "IP")
    @GetMapping("/search")
    public ResponseEntity<?> searchCompanies(@ModelAttribute SearchCompanyDTO searchCompanyDTO) {
        ResponseDTO<?> responseDTO =companyService.getPaginated(searchCompanyDTO.getPage(),"search",searchCompanyDTO);
        ApiResponse<?> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Fetch Company successful",
                responseDTO
        );

        return ResponseEntity.ok(response);
    }
}
