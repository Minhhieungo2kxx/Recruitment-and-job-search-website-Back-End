package com.webjob.application.Service;


import com.webjob.application.Model.Entity.Job;
import com.webjob.application.Dto.Request.SubscriberRequest;
import com.webjob.application.Dto.Response.RespondEmailJob;
import com.webjob.application.Model.Entity.Skill;
import com.webjob.application.Model.Entity.Subscriber;
import com.webjob.application.Model.Entity.User;
import com.webjob.application.Repository.JobRepository;
import com.webjob.application.Repository.SkillRepository;
import com.webjob.application.Repository.SubscriberRepository;


import com.webjob.application.Service.SendEmail.ApplicationEmailService;
import com.webjob.application.Service.SendEmail.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Slf4j
public class SubscriberService {

    private final SubscriberRepository subscriberRepository;

    private final SkillRepository skillRepository;

    private final EmailService emailService;

    private final JobRepository jobRepository;

    private final UserService userService;

    private final ApplicationEmailService applicationEmailService;

    public SubscriberService(SubscriberRepository subscriberRepository, SkillRepository skillRepository, EmailService emailService, JobRepository jobRepository, UserService userService, ApplicationEmailService applicationEmailService) {
        this.subscriberRepository = subscriberRepository;
        this.skillRepository = skillRepository;
        this.emailService = emailService;
        this.jobRepository = jobRepository;
        this.userService = userService;
        this.applicationEmailService = applicationEmailService;
    }

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

    @Scheduled(cron = "0 0 8 1 * *") // 8h sáng ngày 1 mỗi tháng
//    @Scheduled(cron = "0 * * * * *") // chạy mỗi phút
    public void sendSubscribersEmailJobs() {
        log.info("Start sending job emails to subscribers...");

        int pageSize = 500; // xử lý 500 subscriber mỗi batch
        Pageable pageable = PageRequest.of(0, pageSize);

        Page<Long> idPage;

        do {
            // Query 1: lấy 1 trang ID subscriber
            idPage = subscriberRepository.findPageIds(pageable);

            if (idPage.isEmpty()) break;

            log.info("Processing subscriber ID page {} with {} ids",
                    pageable.getPageNumber(), idPage.getContent().size());

            // Query 2: fetch đầy đủ subscriber + skills cho IDs
            List<Subscriber> subscribers =
                    subscriberRepository.findAllWithSkillsByIds(idPage.getContent());

            log.info("Fetched {} subscribers with skills", subscribers.size());

            for (Subscriber subscriber : subscribers) {

                List<Skill> skills = Optional.ofNullable(subscriber.getSkills())
                        .orElse(Collections.emptyList());

                if (skills.isEmpty()) continue;

                // Lấy TOP 10 job mới nhất theo skills
                List<Job> matchedJobs = jobRepository.findTop10BySkills(skills, PageRequest.of(0, 10));

                if (matchedJobs.isEmpty()) continue;

                applicationEmailService.sendJobEmail(subscriber, matchedJobs);
            }

            // chuyển sang trang tiếp theo
            pageable = idPage.nextPageable();

        } while (idPage.hasNext());

        log.info("Finish sending job emails.");
    }



    public String formatVietnameseCurrency(double amount) {
        NumberFormat vndFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        return vndFormat.format(amount); // Output: 15.000.000 ₫
    }

    public Subscriber getbySkillSub() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userService.getbyEmail(email);
        return subscriberRepository.findByEmail(user.getEmail().trim());

    }


}
//    Với cải tiến này:
//
// ✔ Không load tất cả subscriber vào RAM
//✔ Chạy batch rất lớn (10k – 1M subscriber) vẫn ổn
//✔ Email đúng người đúng kỹ năng
//✔ Mỗi subscriber chỉ nhận TOP 10 job mới nhất
//✔ Tối ưu hiệu năng, giảm thời gian chạy scheduled
//✔ Code sạch & dễ mở rộng