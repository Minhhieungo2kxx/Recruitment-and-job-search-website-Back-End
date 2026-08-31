package com.webjob.application.elasticsearch;


import com.webjob.application.elasticsearch.company.CompanyIndexService;
import com.webjob.application.elasticsearch.job.JobIndexService;
import com.webjob.application.models.Entity.Company;
import com.webjob.application.models.Entity.Job;
import com.webjob.application.repository.CompanyRepository;
import com.webjob.application.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ElasticsearchService {
    private final JobIndexService jobIndexService;
    private final JobRepository jobRepository;

    private final CompanyRepository companyRepository;

    private final CompanyIndexService companyIndexService;


    @Transactional(readOnly = true)
    public void migrateAllJobs() {

        int page = 0;
        int size = 500;

        Page<Long> jobPage;

        do {
            jobPage = jobRepository.findAllIds(
                    PageRequest.of(
                            page,
                            size,
                            Sort.by(Sort.Direction.ASC, "id")
                    )
            );

            List<Long> jobIds = jobPage.getContent();

            if (!jobIds.isEmpty()) {
                List<Job> jobs = jobRepository.findByIdIn(jobIds);

                jobIndexService.bulkIndex(jobs);
            }
            page++;

        } while (jobPage.hasNext());
    }

    @Transactional(readOnly = true)
    public void migrateAllCompanies() {
        int page = 0;
        int size = 500;
        Page<Company> companyPage;

        do {
            companyPage = companyRepository.findAll(
                    PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"))
            );


            if (!companyPage.getContent().isEmpty()) {
                companyIndexService.bulkIndex(companyPage.getContent());
            }
            page++;

        } while (companyPage.hasNext());
    }
}
