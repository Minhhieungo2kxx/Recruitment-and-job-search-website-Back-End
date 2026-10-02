package com.webjob.application.controller;

import com.webjob.application.annotation.RateLimit;
import com.webjob.application.dto.Request.Search.MatchFilterRequest;
import com.webjob.application.dto.Response.ApiResponse;
import com.webjob.application.dto.Response.MatchResultDto;
import com.webjob.application.dto.Response.ResponseDTO;
import com.webjob.application.mapper.MatchResultMapper;
import com.webjob.application.models.Entity.MatchResult;
import com.webjob.application.service.JobMatchingHR.Matching.CvJobMatchingService;
import com.webjob.application.service.JobMatchingHR.Matching.JobMatchingBatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/hr")
@RequiredArgsConstructor
public class HrMatchingController {
    private final CvJobMatchingService cvJobMatchingService;
    private final JobMatchingBatchService jobMatchingBatchService;
    private final MatchResultMapper matchResultMapper;


//  Chạy matching cho toàn bộ application của Job. Trả 202 ngay, chạy nền.
    @RateLimit(maxRequests = 6, timeWindowSeconds = 60, keyType = "TOKEN")
    @PostMapping("/jobs/{jobId}/matching/run")
    public ResponseEntity<ApiResponse<Void>> runMatching(@PathVariable Long jobId) {
        jobMatchingBatchService.startMatching(jobId);
        ApiResponse<Void> apiResponse = new ApiResponse<>(
                HttpStatus.ACCEPTED.value(),
                null,
                "Đã tiếp nhận yêu cầu chạy matching cho công việc",
                null
        );
        return new ResponseEntity<>(apiResponse, HttpStatus.ACCEPTED);
    }

//    Re-run giống hệt run - tách endpoint riêng cho rõ ý định (vd sau khi đổi JobSkill)
    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "TOKEN")
    @PostMapping("/jobs/{jobId}/matching/rerun")
    public ResponseEntity<ApiResponse<Void>> rerunMatching(@PathVariable Long jobId) {
        // Kích hoạt tiến trình chạy lại matching bất đồng bộ
        jobMatchingBatchService.startMatching(jobId);
        ApiResponse<Void> apiResponse = new ApiResponse<>(
                HttpStatus.ACCEPTED.value(),
                null,
                "Đã tiếp nhận yêu cầu chạy lại matching cho công việc",
                null
        );
        return new ResponseEntity<>(apiResponse, HttpStatus.ACCEPTED);
    }


    @RateLimit(maxRequests = 8, timeWindowSeconds = 60, keyType = "TOKEN")
    @GetMapping("/jobs/{jobId}/matching/results")
    public ResponseEntity<ApiResponse<ResponseDTO<List<MatchResultDto.Summary>>>> getResults(
            @PathVariable Long jobId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @ModelAttribute MatchFilterRequest request) {
        ApiResponse<ResponseDTO<List<MatchResultDto.Summary>>> apiResponse = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Lấy danh sách kết quả matching thành công",
                cvJobMatchingService.getJobMatches(page, size, jobId, request)
        );

        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Xem chi tiết 1 candidate. Match Score/breakdown đã có sẵn (tính lúc /run).
     * Explanation (AI) CHỈ được sinh ở lần xem chi tiết ĐẦU TIÊN - các lần xem sau
     * dùng lại kết quả đã lưu, không gọi AI lại (xem CvJobMatchingService.ensureExplanation).
     */
    @RateLimit(maxRequests = 8, timeWindowSeconds = 60, keyType = "TOKEN")
    @GetMapping("/applications/{applicationId}/matching")
    public ResponseEntity<ApiResponse<MatchResultDto.Detail>> getDetail(@PathVariable Long applicationId) {
        MatchResult matchResult = cvJobMatchingService.ensureExplanation(applicationId);
        ApiResponse<MatchResultDto.Detail> apiResponse = new ApiResponse<>(
                HttpStatus.OK.value(),
                null,
                "Lấy chi tiết kết quả matching thành công",
                matchResultMapper.toDetail(matchResult)
        );
        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Chạy lại (hoặc chạy mới) quá trình matching cho 1 candidate cụ thể.
     * Tính toán lại Match Score, breakdown và chuẩn bị dữ liệu.
     */
    @RateLimit(maxRequests = 5, timeWindowSeconds = 60, keyType = "TOKEN")
    @PostMapping("/applications/{applicationId}/matching/run")
    public ResponseEntity<ApiResponse<Void>> runMatchingForOne(@PathVariable Long applicationId) {

        jobMatchingBatchService.startMatchingForApplication(applicationId);
        ApiResponse<Void> apiResponse = new ApiResponse<>(
                HttpStatus.ACCEPTED.value(),
                null,
                "Yêu cầu chạy matching applications đã được tiếp nhận thành công",
                null
        );
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(apiResponse);
    }


    



}



