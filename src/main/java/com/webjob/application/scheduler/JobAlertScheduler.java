package com.webjob.application.scheduler;

import com.webjob.application.messaging.producer.EmailProducer;
import com.webjob.application.models.Entity.JobAlert;
import com.webjob.application.repository.JobAlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobAlertScheduler {

    private final JobAlertRepository jobAlertRepository;
    private final EmailProducer emailProducer;


//    @Scheduled(cron = "0 0 8 * * MON", zone = "Asia/Ho_Chi_Minh")  // Chạy vào 08:00:00 AM sáng Thứ Hai mỗi tuần

    @Scheduled(cron = "0 */1 * * * *") // Chạy mỗi phút một lần (dùng để test)
    public void processJobAlert() {
        log.info("Start publishing  jobAlerts...");
        Pageable pageable = PageRequest.of(0, 500);
        Page<Long> page;

        do {
            page = jobAlertRepository.findIdsToProcess(Instant.now(), pageable);

            if (page == null || page.isEmpty()) {
                break;
            }

            page.getContent().forEach(emailProducer::publishJobAlerts);

            pageable = page.nextPageable();

        } while (page.hasNext());

        log.info("Finish publishing JobAlert...");
    }


}




