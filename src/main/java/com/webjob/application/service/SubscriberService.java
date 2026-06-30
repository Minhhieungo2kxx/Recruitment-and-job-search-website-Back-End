package com.webjob.application.service;


import com.webjob.application.messaging.producer.EmailProducer;
import com.webjob.application.models.Entity.Job;
import com.webjob.application.dto.Request.SubscriberRequest;
import com.webjob.application.models.Entity.Skill;
import com.webjob.application.models.Entity.Subscriber;
import com.webjob.application.models.Entity.User;
import com.webjob.application.repository.JobRepository;
import com.webjob.application.repository.SkillRepository;
import com.webjob.application.repository.SubscriberRepository;


import com.webjob.application.service.SendEmail.ApplicationEmailService;
import com.webjob.application.service.SendEmail.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.NumberFormat;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SubscriberService {

    private final SubscriberRepository subscriberRepository;

    private final SkillRepository skillRepository;

    private final EmailService emailService;

    private final JobRepository jobRepository;

    private final UserService userService;

    private final ApplicationEmailService applicationEmailService;
    private final EmailProducer emailProducer;


    @Transactional
    public Subscriber createSubciber(Subscriber subscriber) {
        checkEmailSubcriber(subscriber.getEmail());
        List<Skill> validSkills = getValidSkills(subscriber.getSkills());
        if (validSkills.isEmpty()) {
            throw new IllegalArgumentException("Không có kỹ năng nào hợp lệ.");
        }
        subscriber.setSkills(validSkills);
        return subscriberRepository.save(subscriber);
    }

    @Transactional
    public Subscriber updateSubciber(SubscriberRequest request) {
        Subscriber canfind = getById(request.getId());
        List<Skill> validSkills = getValidSkills(request.getSkills());
        if (validSkills.isEmpty()) {
            throw new IllegalArgumentException("Không có kỹ năng nào hợp lệ.");
        }
        canfind.setSkills(validSkills);
        return subscriberRepository.save(canfind);
    }

    public boolean checkEmailSubcriber(String email) {
        boolean exist = subscriberRepository.existsByEmail(email);
        if (exist) {
            throw new IllegalArgumentException("Subcriber email " + email + " da ton tai, vui long tao cai khac");
        }
        return false;
    }

    private List<Skill> getValidSkills(List<Skill> skills) {
        List<Long> ids = skills.stream()
                .map(Skill::getId)
                .collect(Collectors.toList());
        return skillRepository.findByIdIn(ids);
    }

    public Subscriber getById(Long id) {
        Subscriber get = subscriberRepository.findById(id).
                orElseThrow(() -> new IllegalArgumentException("Subcriber not found with ID: " + id));
        return get;
    }

    @Scheduled(cron = "0 0 8 1 * *")
//    @Scheduled(cron = "0 */1 * * * *")
    public void sendSubscribersEmailJobs() {

        log.info("Start publishing email jobs...");

        Pageable pageable = PageRequest.of(0, 500);

        Page<Long> page;

        do {

            page = subscriberRepository.findPageIds(pageable);

            if (page.isEmpty()) {
                break;
            }

            page.getContent().forEach(emailProducer::publish);

            pageable = page.nextPageable();

        } while (page.hasNext());

        log.info("Finish publishing.");

    }


    public String formatVietnameseCurrency(double amount) {
        NumberFormat vndFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        return vndFormat.format(amount); // Output: 15.000.000 ₫
    }

    public Subscriber getbySkillSub(Authentication authentication) {
        User user = userService.getById(Long.valueOf(authentication.getName()));
        return subscriberRepository.findByEmail(user.getEmail().trim());

    }
//    feat(email): add RabbitMQ-based asynchronous email processing
//
//- configure RabbitMQ exchange, queue and DLQ
//- add retry and dead-letter handling
//- schedule monthly email publishing
//- publish subscriber email jobs to queue
//- consume email jobs asynchronously
//- send job recommendation emails using template


}
//    Với cải tiến này:
//
// ✔ Không load tất cả subscriber vào RAM
//✔ Chạy batch rất lớn (10k – 1M subscriber) vẫn ổn
//✔ Email đúng người đúng kỹ năng
//✔ Mỗi subscriber chỉ nhận TOP 10 job mới nhất
//✔ Tối ưu hiệu năng, giảm thời gian chạy scheduled
//✔ Code sạch & dễ mở rộng