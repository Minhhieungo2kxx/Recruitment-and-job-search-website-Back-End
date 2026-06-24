package com.webjob.application.controller;


import com.webjob.application.annotation.RateLimit;
import com.webjob.application.dto.Request.CompanyDTO;
import com.webjob.application.dto.Request.Search.SearchCompanyDTO;
import com.webjob.application.dto.Response.ApiResponse;
import com.webjob.application.dto.Response.ResponseDTO;
import com.webjob.application.service.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/companies")
public class CompanyController {
    private final CompanyService companyService;



    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "TOKEN")
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CompanyDTO companyDTO) {
       return companyService.createCompany(companyDTO);
    }

    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "TOKEN")
    @PutMapping("/{id}")
    public ResponseEntity<?> EditCompany(@PathVariable Long id, @Valid @RequestBody CompanyDTO companyDTO) {
        return companyService.updateCompany(id,companyDTO);
    }

    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "IP")
    @GetMapping("/{id}")
    public ResponseEntity<?> getCompanyID(@PathVariable Long id) {
        return companyService.getCompanybyID(id);

    }

    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "TOKEN")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCompanyId(@PathVariable Long id) {
            return companyService.deleteCompanybyId(id);

    }

    @RateLimit(maxRequests = 10, timeWindowSeconds = 60, keyType = "IP")
    @GetMapping
    public ResponseEntity<?> callPageList(@RequestParam(value = "page", defaultValue = "0") String pageparam) {
        return companyService.callPageList(pageparam);
    }

    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "IP")
    @GetMapping("/search")
    public ResponseEntity<?> searchCompany(@ModelAttribute SearchCompanyDTO searchCompanyDTO) {
        return companyService.searchCompanies(searchCompanyDTO);
    }
}
