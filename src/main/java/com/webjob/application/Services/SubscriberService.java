package com.webjob.application.Services;


import com.webjob.application.Models.Entity.Job;
import com.webjob.application.Models.Request.SubscriberRequest;
import com.webjob.application.Models.Response.ResponEmailJob;
import com.webjob.application.Models.Entity.Skill;
import com.webjob.application.Models.Entity.Subscriber;
import com.webjob.application.Models.Entity.User;
import com.webjob.application.Repository.JobRepository;
import com.webjob.application.Repository.SkillRepository;
import com.webjob.application.Repository.SubscriberRepository;


import com.webjob.application.Services.SendEmail.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.NumberFormat;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class SubscriberService {
    @Autowired
    private SubscriberRepository subscriberRepository;
    @Autowired
    private SkillRepository skillRepository;
    @Autowired
    private EmailService emailService;
    @Autowired
    private JobRepository jobRepository;
    @Autowired
    private UserService userService;
    private static final Logger log = LoggerFactory.getLogger(SubscriberService.class);

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

    @Scheduled(fixedDelay = 2592000000L)
    @Async("taskExecutor")
    @Transactional
    public void sendSubscribersEmailJobs() {
        System.out.println("Send email");

        List<Subscriber> subscribers = subscriberRepository.findAll();
        if (subscribers == null || subscribers.isEmpty()) return;

        for (Subscriber subscriber : subscribers) {
            List<Skill> skills = subscriber.getSkills();

            if (skills == null || skills.isEmpty()) continue;

            List<Job> matchedJobs = jobRepository.findAllBySkillsIn(skills);

            if (matchedJobs == null || matchedJobs.isEmpty()) continue;

            sendJobEmail(subscriber, matchedJobs);
        }

    }


    private void sendJobEmail(Subscriber subscriber, List<Job> jobs) {
        List<ResponEmailJob> jobSummaries = jobs.stream()
                .map(this::convertJobToSendEmail)
                .collect(Collectors.toList());

        emailService.sendTemplateEmail(
                subscriber.getEmail(),
                "Cơ hội việc làm hot đang chờ đón bạn, khám phá ngay",
                "job",
                subscriber.getName(),
                jobSummaries
        );

    }

    public ResponEmailJob convertJobToSendEmail(Job job) {
        if (job == null) {
            throw new IllegalArgumentException("Job must not be null");
        }

        String jobName = job.getName();
//        double salary = job.getSalary();

        String companyName = job.getCompany() != null ? job.getCompany().getName() : "Unknown";

        List<ResponEmailJob.SkillEmail> skillEmails = Optional.ofNullable(job.getSkills())
                .orElse(Collections.emptyList())
                .stream()
                .map(skill -> new ResponEmailJob.SkillEmail(skill.getName()))
                .collect(Collectors.toList());

        ResponEmailJob response = new ResponEmailJob();
        response.setName(jobName);
        response.setFormattedSalary(formatVietnameseCurrency(job.getSalary()));
        response.setCompany(new ResponEmailJob.CompanyEmail(companyName));
        response.setSkills(skillEmails);

        return response;
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
