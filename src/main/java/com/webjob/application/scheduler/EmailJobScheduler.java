package com.webjob.application.scheduler;

import com.webjob.application.messaging.producer.EmailProducer;
import com.webjob.application.repository.SubscriberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class EmailJobScheduler {
    private final SubscriberRepository subscriberRepository;

    private final EmailProducer emailProducer;

    @Scheduled(cron = "0 0 8 1 * *", zone = "Asia/Ho_Chi_Minh")  //08:00:00 ngày 01 hàng tháng
//     @Scheduled(cron = "0 */1 * * * *") // Chạy mỗi phút một lần (dùng để test)
    public void sendSubscribersEmailJobs() {

        log.info("Start publishing email jobs...");

        Pageable pageable = PageRequest.of(0, 500, Sort.by("id"));
        Page<Long> page;

        do {
            page = subscriberRepository.findPageIds(pageable);

            if (page == null || page.isEmpty()) {
                break;
            }

            page.getContent().forEach(emailProducer::publish);

            pageable = page.nextPageable();

        } while (page.hasNext());

        log.info("Finish publishing.");
    }

}
