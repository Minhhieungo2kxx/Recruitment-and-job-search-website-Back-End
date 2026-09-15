package com.webjob.application.service.JobRecommendation;

import com.webjob.application.enums.JobStatus;
import com.webjob.application.models.Entity.Job;
import com.webjob.application.models.Entity.User;
import com.webjob.application.repository.ApplicationRepository;
import com.webjob.application.repository.JobRepository;
import com.webjob.application.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CandidateJobService {
    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final SkillRepository skillRepository;
    private static final int MAX_CANDIDATES = 25;

    public List<Job> findCandidates(User user, List<String> cvSkillNames) {
        Instant now = Instant.now();
        Pageable pageable = PageRequest.of(0, MAX_CANDIDATES);

        // 1. Skill từ CV -> Skill IDs
        List<Long> matchedSkillIds =
                skillRepository.findIdsByNameIn(cvSkillNames);

        // 2. Những Job user đã apply -> loại ra
        List<Long> excludedJobIds =
                applicationRepository.findJobIdsByUserId(user.getId());

        if (excludedJobIds.isEmpty()) {
            excludedJobIds = List.of(-1L); // tránh lỗi IN () rỗng
        }


        // CASE 1: CV không có skill nào match
        if (matchedSkillIds.isEmpty()) {
            Page<Long> idPage = jobRepository.findJobIds(
                    JobStatus.OPEN,
                    now,
                    excludedJobIds,
                    pageable
            );
            if (idPage.isEmpty()) {
                return Collections.emptyList();
            }
            return loadJobsWithDetailsInOrder(idPage.getContent());
        }

//        // CASE 2: Có skill match
        List<Long> jobIds = jobRepository.findCandidateJobIdsBySkills(
                matchedSkillIds,
                excludedJobIds,
                JobStatus.OPEN,
                now, pageable);
        if (jobIds.isEmpty()) {
            return Collections.emptyList();
        }
        return loadJobsWithDetailsInOrder(jobIds);

    }
    private List<Job> loadJobsWithDetailsInOrder(List<Long> jobIds) {

        if (jobIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Job> jobs = jobRepository.findJobsWithDetails(jobIds);

        Map<Long, Job> jobMap = jobs.stream()
                .collect(Collectors.toMap(
                        Job::getId,
                        Function.identity()
                ));
        // Giữ nguyên thứ tự từ query lấy ID
        return jobIds.stream()
                .map(jobMap::get)
                .filter(Objects::nonNull)
                .toList();
    }





}
