package com.webjob.application.service.JobMatchingHR.Matching;

import com.webjob.application.component.SecurityUtils;
import com.webjob.application.enums.ResumeStatus;
import com.webjob.application.exception.Customs.ForbiddenException;
import com.webjob.application.exception.Customs.ResourceNotFoundException;
import com.webjob.application.messaging.producer.CandidateMatchProducer;
import com.webjob.application.models.Entity.Application;
import com.webjob.application.models.Entity.Job;
import com.webjob.application.models.Entity.User;
import com.webjob.application.repository.ApplicationRepository;
import com.webjob.application.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;


/**
 * Orchestrator toàn bộ flow: Application -> CandidateProfile (parse+cache)
 * -> chuẩn hoá skill -> chấm điểm (rule-based, MIỄN PHÍ - không gọi AI).
 * Explanation (AI giải thích bằng ngôn ngữ tự nhiên) KHÔNG chạy kèm ở đây -
 * // xem {@link # ensureExplanation(Long)}, chỉ gọi AI khi HR thực sự mở xem chi tiết
 * 1 candidate, để tránh tốn N lần gọi AI cho N ứng viên mà HR có thể chỉ xem
 * kỹ 5-10 người điểm cao nhất (Match Score đã đủ để HR sort/lọc trước, miễn phí).
 * <p>
 * Mỗi Application được xử lý trong 1 transaction riêng (REQUIRES_NEW) để:
 * - 1 candidate lỗi (CV hỏng, AI timeout...) không rollback kết quả của các candidate khác trong cùng batch,
 * - Job có nhiều ứng viên vẫn chạy tuần tự an toàn, dễ retry lại đúng candidate bị lỗi.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JobMatchingBatchService {
    private final ApplicationRepository applicationRepository;
    private final CvJobMatchingService cvJobMatchingService;
    private final CandidateMatchProducer candidateMatchProducer;
    private final JobRepository jobRepository;
    private final SecurityUtils securityUtils;

    /**
     * Chỉ chạy matching cho application CÒN ĐANG ĐƯỢC CÂN NHẮC - loại bỏ:
     * - REJECTED: đã bị từ chối, HR không cần xem điểm matching nữa.
     * - HIRED: đã chốt xong, kết quả không còn tác dụng ra quyết định.
     * Điều chỉnh danh sách này nếu nghiệp vụ của bạn khác (vd muốn giữ matching
     * cho HIRED để lưu vết lý do tuyển, thì thêm ResumeStatus.HIRED vào đây).
     */
    private static final List<ResumeStatus> MATCHABLE_STATUSES = List.of(
            ResumeStatus.PENDING,
            ResumeStatus.REVIEWING,
            ResumeStatus.SHORTLISTED,
            ResumeStatus.INTERVIEWING,
            ResumeStatus.OFFERED
    );

    public void startMatching(Long jobId) {
        User user = securityUtils.getCurrentUser();

        if (!jobRepository.existsByIdAndCompanyId(jobId, user.getCompany().getId())) {
            throw new ForbiddenException("You are not allowed to access this job.");
        }
        runMatchingForJobAsync(jobId);
    }


    @Async("matchingTaskExecutor")
    public void runMatchingForJobAsync(Long jobId) {

        long startTime = System.currentTimeMillis();
        int totalProcessed = 0;
        int totalFailed = 0;

        log.info("Start matching for jobId={}", jobId);

        Long lastId = 0L;

        while (true) {
            List<Long> applicationIds =
                    applicationRepository.findNextIdsByJobIdAndStatusIn(
                            jobId,
                            MATCHABLE_STATUSES,
                            lastId,
                            PageRequest.of(0, 500)
                    );

            if (applicationIds.isEmpty()) {
                break;
            }

            for (Long applicationId : applicationIds) {
                totalProcessed++;

                try {
//                    cvJobMatchingService.runMatchingForApplication(applicationId);
                    candidateMatchProducer.publishEvent(applicationId);
                } catch (Exception e) {
                    totalFailed++;
                    log.error(
                            "Matching failed: applicationId={}, jobId={}, error={}",
                            applicationId,
                            jobId,
                            e.getMessage(),
                            e
                    );
                }
            }

            lastId = applicationIds.get(applicationIds.size() - 1);
        }
        log.info(
                "Finished matching for jobId={}, processed={}, failed={}, durationMs={}",
                jobId,
                totalProcessed,
                totalFailed,
                System.currentTimeMillis() - startTime
        );


    }

    public void startMatchingForApplication(Long applicationId) {
        Application application=applicationRepository.findById(applicationId).orElseThrow(
                ()-> new ResourceNotFoundException("Application not found "+applicationId)
        );
        Long jobId=application.getJob().getId();
        User user = securityUtils.getCurrentUser();
        if (!jobRepository.existsByIdAndCompanyId(jobId, user.getCompany().getId())) {
            throw new ForbiddenException("You are not allowed to access this job.");
        }
        runMatchingForApplicationAsync(applicationId);
    }

    @Async("matchingTaskExecutor")
    public void runMatchingForApplicationAsync(Long applicationId) {
        try {
            cvJobMatchingService.runMatchingForApplication(applicationId);
        } catch (Exception e) {
            // runMatchingForApplication đã xử lý FAILED bên trong.
            // Catch này chỉ để đảm bảo exception không làm chết async task.
            log.error("Matching thất bại cho application {}", applicationId, e);
        }
    }


}


