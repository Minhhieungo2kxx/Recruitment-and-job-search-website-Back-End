package com.webjob.application.elasticsearch.job;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.HealthStatus;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import com.webjob.application.document.JobDocument;
import com.webjob.application.enums.JobStatus;
import com.webjob.application.exception.Customs.ElasticsearchException;
import com.webjob.application.mapper.JobMapper;
import com.webjob.application.models.Entity.Job;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobIndexService {
    private final ElasticsearchClient elasticsearchClient;

    private final JobMapper jobMapper;
    private static final String INDEX_NAME = "jobs";




    @EventListener(ApplicationReadyEvent.class)
    public void createJobIndex() {
        if (!waitForElasticsearch()) {
            log.error(
                    "Skip creating Elasticsearch index '{}' because Elasticsearch is unavailable",
                    INDEX_NAME
            );
            return;
        }

        try {
            boolean exists = elasticsearchClient.indices()
                    .exists(e -> e.index(INDEX_NAME))
                    .value();

            if (exists) {
                log.info("Index " + INDEX_NAME + " already exists");
                return;
            }

            elasticsearchClient.indices().create(c -> c

                    .index(INDEX_NAME)
                    .settings(s -> s
                            .analysis(a -> a

                                    .filter("job_ascii_folding",
                                            f -> f.definition(
                                                    d -> d.asciifolding(
                                                            af -> af
                                                                    .preserveOriginal(false)
                                                    )
                                            )
                                    )
                                    .normalizer("job_keyword_normalizer",
                                            n -> n.custom(
                                                    custom -> custom
                                                            .filter(
                                                                    "lowercase",
                                                                    "job_ascii_folding"
                                                            )
                                            )
                                    )

                                    .analyzer("job_text_analyzer",
                                            an -> an.custom(
                                                    custom -> custom
                                                            .tokenizer("standard")
                                                            .filter(
                                                                    "lowercase",
                                                                    "job_ascii_folding"
                                                            )
                                            )
                                    )
                            )
                    )

                    .mappings(m -> m
                            .properties("id", p -> p
                                    .long_(l -> l)
                            )
                            .properties("name", p -> p
                                    .text(t -> t
                                            .analyzer("job_text_analyzer")
                                            .fields("keyword", f -> f
                                                    .keyword(k -> k
                                                            .normalizer("job_keyword_normalizer")
                                                    )
                                            )
                                    )
                            )

                            .properties("location", p -> p
                                    .text(t -> t
                                            .analyzer("job_text_analyzer")
                                            .fields("keyword", f -> f
                                                    .keyword(k -> k
                                                            .normalizer("job_keyword_normalizer")
                                                    )
                                            )
                                    )
                            )

                            .properties("description", p -> p
                                    .text(t -> t
                                            .analyzer("job_text_analyzer")
                                    )
                            )

                            .properties("benefits", p -> p
                                    .text(t -> t
                                            .analyzer("job_text_analyzer")
                                    )
                            )

                            .properties("requirement", p -> p
                                    .text(t -> t
                                            .analyzer("job_text_analyzer")
                                    )
                            )

                            .properties("responsibility", p -> p
                                    .text(t -> t
                                            .analyzer("job_text_analyzer")
                                    )
                            )


                            .properties("salaryMin", p -> p
                                    .double_(d -> d)
                            )

                            .properties("salaryMax", p -> p
                                    .double_(d -> d)
                            )

                            .properties("quantity", p -> p
                                    .integer(i -> i)
                            )

                            .properties("experienceRequired", p -> p
                                    .integer(i -> i)
                            )

                            .properties("viewCount", p -> p
                                    .long_(l -> l)
                            )

                            .properties("appliedCount", p -> p
                                    .integer(i -> i)
                            )

                            .properties("negotiable", p -> p
                                    .boolean_(b -> b)
                            )

                            .properties("deleted", p -> p
                                    .boolean_(b -> b)
                            )

                            .properties("level", p -> p
                                    .keyword(k -> k)
                            )
                            .properties("workingType", p -> p
                                    .keyword(k -> k)
                            )

                            .properties("workMode", p -> p
                                    .keyword(k -> k)
                            )

                            .properties("competitionLevel", p -> p
                                    .keyword(k -> k)
                            )

                            .properties("status", p -> p
                                    .keyword(k -> k)
                            )

                            .properties("startDate", p -> p
                                    .date(d -> d)
                            )

                            .properties("endDate", p -> p
                                    .date(d -> d)
                            )

                            .properties("createdAt", p -> p
                                    .date(d -> d)
                            )

                            .properties("companyId", p -> p
                                    .long_(l -> l)
                            )
                            .properties("companyName", p -> p
                                    .text(t -> t
                                            .analyzer("job_text_analyzer")
                                            .fields("keyword", f -> f
                                                    .keyword(k -> k.normalizer("job_keyword_normalizer")
                                                    )
                                            )
                                    )
                            )
                            .properties("companyStatus", p -> p
                                    .keyword(k -> k)
                            )
                            .properties("companyDeleted", p -> p
                                    .boolean_(b -> b)
                            )


                            .properties("jobCategoryId", p -> p
                                    .long_(l -> l)
                            )

                            .properties("jobCategoryName", p -> p
                                    .text(t -> t
                                            .analyzer("job_text_analyzer")
                                            .fields("keyword", f -> f.keyword(
                                                            k -> k.normalizer("job_keyword_normalizer")

                                                    )
                                            )
                                    )
                            )
                            .properties("skills", p -> p
                                    .nested(n -> n
                                            .properties("id", sp -> sp
                                                    .long_(l -> l)
                                            )
                                            .properties("skillId", sp -> sp
                                                    .long_(l -> l)
                                            )
                                            .properties("skillName", sp -> sp
                                                    .text(t -> t
                                                            .analyzer(
                                                                    "job_text_analyzer"
                                                            )
                                                            .fields("keyword",
                                                                    f -> f
                                                                            .keyword(
                                                                                    k -> k.normalizer(
                                                                                            "job_keyword_normalizer"
                                                                                    )
                                                                            )
                                                            )
                                                    )
                                            )
                                    )
                            )
                    )
            );
            log.info("Created Elasticsearch index:" + INDEX_NAME);


        } catch (Exception e) {
            log.error(
                    "Cannot create Elasticsearch index: {}",
                    INDEX_NAME,
                    e
            );
        }
    }

    public void bulkIndex(List<Job> jobs) {

        if (jobs == null || jobs.isEmpty()) {
            return;
        }
        BulkRequest.Builder bulkRequest = new BulkRequest.Builder();
        jobs.forEach(job -> {
            JobDocument document = jobMapper.toDocument(job);
            bulkRequest.operations(op -> op
                    .index(idx -> idx
                            .index(INDEX_NAME)
                            .id(String.valueOf(job.getId()))
                            .document(document)
                    )
            );
        });

        try {
            BulkResponse response = elasticsearchClient.bulk(bulkRequest.build());
            if (response.errors()) {
                response.items().forEach(item -> {
                    if (item.error() != null) {
                        log.error(
                                "Failed to index Job. id={}, type={}, reason={}",
                                item.id(),
                                item.error().type(),
                                item.error().reason()
                        );
                    }
                });

                throw new ElasticsearchException("Some jobs failed during bulk indexing");
            }
            log.info("Successfully indexed {} jobs to Elasticsearch", response.items().size()
            );
        } catch (IOException e) {
            log.error("Bulk indexing failed", e);
            throw new ElasticsearchException("Bulk indexing failed", e);
        }
    }


    public void indexJob(JobDocument document) {
        try {
            elasticsearchClient.index(i -> i
                    .index(INDEX_NAME)
                    .id(String.valueOf(document.getId()))
                    .document(document));
        } catch (IOException e) {
            throw new ElasticsearchException("Failed to index job: " + document.getId(), e);
        }
    }

    public void deleteIndexJob(JobDocument document) {
        try {
            Map<String, Object> update = new HashMap<>();
            update.put("deleted", document.getDeleted());
            update.put("status", document.getStatus());

            elasticsearchClient.update(u -> u
                    .index(INDEX_NAME)
                    .id(String.valueOf(document.getId()))
                    .doc(update),
                    JobDocument.class
            );
        } catch (IOException e) {
            throw new ElasticsearchException("Failed to soft delete job: " + document.getId(),e);
        }
    }

    public void restoreIndexJob(JobDocument job) {
        try {
            Map<String, Object> update = new HashMap<>();
            update.put("deleted", job.getDeleted());
            update.put("status", job.getStatus());
            elasticsearchClient.update(u -> u
                            .index(INDEX_NAME)
                            .id(String.valueOf(job.getId()))
                            .doc(update),
                    JobDocument.class
            );

        } catch (IOException e) {
            throw new ElasticsearchException("Failed to restore job: " + job.getId(), e);
        }
    }






    private boolean waitForElasticsearch() {

        int maxRetries = 10;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {

            try {

                elasticsearchClient.cluster().health(h -> h
                        .waitForStatus(HealthStatus.Yellow)
                        .timeout(t -> t.time("5s"))
                );

                log.info("Elasticsearch is ready. attempt={}/{}", attempt, maxRetries);
                return true;
            } catch (Exception e) {
                log.warn("Elasticsearch is not ready. attempt={}/{}, error={}", attempt, maxRetries, e.getMessage()
                );
                if (attempt == maxRetries) {
                    return false;
                }
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        return false;
    }



    public void incrementViewCount(Long jobId) {
        try {
            elasticsearchClient.update(u -> u
                            .index(INDEX_NAME)
                            .id(String.valueOf(jobId))
                            .script(s -> s.source("""
                            if (ctx._source.viewCount == null) {
                                ctx._source.viewCount = 1;
                            } else {
                                ctx._source.viewCount += 1;
                            }
                        """)
                            ),
                    JobDocument.class
            );

        } catch (IOException e) {
            log.error("Failed to increment viewCount in Elasticsearch, jobId={}", jobId, e);
            throw new ElasticsearchException("Failed to increment viewCount Job "+jobId,e);

        }
    }

    public void incrementAppliedCount(Long jobId) {
        try {
            elasticsearchClient.update(u -> u
                            .index(INDEX_NAME)
                            .id(String.valueOf(jobId))
                            .script(s -> s.source("""
                            if (ctx._source.appliedCount == null) {
                                ctx._source.appliedCount = 1;
                            } else {
                                ctx._source.appliedCount += 1;
                            }
                        """)),
                    JobDocument.class
            );

        } catch (IOException e) {
            log.error("Failed to increment appliedCount in Elasticsearch, jobId={}", jobId, e);
            throw new ElasticsearchException("Failed to increment appliedCount in Elasticsearch", e);
        }
    }

    public void decrementAppliedCount(Long jobId) {
        try {
            elasticsearchClient.update(u -> u
                            .index(INDEX_NAME)
                            .id(String.valueOf(jobId))
                            .script(s -> s.source("""
                            if (ctx._source.appliedCount != null && ctx._source.appliedCount > 0) {
                                ctx._source.appliedCount -= 1;
                            }
                        """)),
                    JobDocument.class
            );

        } catch (IOException e) {
            log.error("Failed to decrement appliedCount in Elasticsearch, jobId={}", jobId, e);
            throw new ElasticsearchException("Failed to decrement appliedCount job: " + jobId,e);
        }
    }




}

