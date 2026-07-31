package com.webjob.application.controller;

import com.webjob.application.annotation.RateLimit;
import com.webjob.application.dto.Response.ApiResponse;
import com.webjob.application.dto.Response.FollowCompanyResponse;
import com.webjob.application.dto.Response.ResponseDTO;
import com.webjob.application.service.FollowCompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/follow-companies")
@RequiredArgsConstructor
public class FollowCompanyController {
    private final FollowCompanyService followCompanyService;


    @RateLimit(maxRequests = 10, timeWindowSeconds = 60, keyType = "TOKEN")
    @PreAuthorize("hasAnyRole('USER_BASIC','USER')")
    @PostMapping("/{companyId}")
    public ResponseEntity<ApiResponse<Object>> follow(@PathVariable Long companyId) {

        followCompanyService.followCompany(companyId);
        ApiResponse<Object> apiResponse = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Follow Company thành công",
                null
        );
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }


    @RateLimit(maxRequests = 10, timeWindowSeconds = 60, keyType = "TOKEN")
    @PreAuthorize("hasAnyRole('USER_BASIC','USER')")
    @DeleteMapping("/{companyId}")
    public ResponseEntity<ApiResponse<Object>> unfollow(@PathVariable Long companyId) {

        followCompanyService.unfollowCompany(companyId);
        ApiResponse<Object> apiResponse = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Delete FollowCompany thành công",
                null
        );
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }


    @RateLimit(maxRequests = 30, timeWindowSeconds = 60, keyType = "TOKEN")
    @PreAuthorize("hasAnyRole('USER_BASIC','USER')")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<ResponseDTO<List<FollowCompanyResponse>>>> myFollowCompanies(
            @RequestParam(defaultValue = "1") int page
            , @RequestParam(defaultValue = "10") int size) {
        ApiResponse<ResponseDTO<List<FollowCompanyResponse>>> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                null
                , "fetch all FollowCompany Successful"
                , followCompanyService.getMyFollowCompanies(page, size)
        );
        return ResponseEntity.ok(response);

    }

    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "TOKEN")
    @PreAuthorize("hasAnyRole('USER_BASIC','USER')")
    @PatchMapping("/{id}/enable")
    public ResponseEntity<ApiResponse<Object>> enableNotification(@PathVariable Long id) {
        followCompanyService.enableNotification(id);
        ApiResponse<Object> apiResponse = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Enable FollowCompany thành công",
                null
        );

        return ResponseEntity.ok(apiResponse);

    }


    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "TOKEN")
    @PreAuthorize("hasAnyRole('USER_BASIC','USER')")
    @PatchMapping("/{id}/disable")
    public ResponseEntity<ApiResponse<Object>> disableNotification(@PathVariable Long id) {
        followCompanyService.disableNotification(id);
        ApiResponse<Object> apiResponse = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Disable FollowCompany thành công",
                null
        );

        return ResponseEntity.ok(apiResponse);
    }
//

}
//git commit -m "feat(follow-company): implement follow company APIs" -m "
////            - add follow company endpoint
////- add unfollow company endpoint
////- add current user's followed companies endpoint
////            - add enable/disable notification endpoints
////"

