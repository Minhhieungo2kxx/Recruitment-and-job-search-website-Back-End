package com.webjob.application.elasticsearch.company;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.HealthStatus;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import com.webjob.application.document.CompanyDocument;
import com.webjob.application.document.JobDocument;
import com.webjob.application.enums.CompanyStatus;
import com.webjob.application.enums.JobStatus;
import com.webjob.application.exception.Customs.ElasticsearchException;
import com.webjob.application.mapper.CompanyMapper;
import com.webjob.application.models.Entity.Company;
import com.webjob.application.models.Entity.Job;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompanyIndexService {
    private final ElasticsearchClient elasticsearchClient;
    private static final String INDEX_NAME_COMPANY = "companies";
    private final CompanyMapper companyMapper;

    @EventListener(ApplicationReadyEvent.class)
    public void createCompanyIndex() {
        if (!waitForElasticsearch()) {
            log.error(
                    "Skip creating Elasticsearch index '{}' because Elasticsearch is unavailable",
                    INDEX_NAME_COMPANY
            );
            return;
        }
        try {
            boolean exists = elasticsearchClient.indices()
                    .exists(e -> e.index(INDEX_NAME_COMPANY))
                    .value();

            if (exists) {
                log.info("Index {} already exists", INDEX_NAME_COMPANY);
                return;
            }
            elasticsearchClient.indices().create(c -> c
                    .index(INDEX_NAME_COMPANY)

                    .settings(s -> s
                            .analysis(a -> a

                                    // ASCII folding filter
                                    .filter(
                                            "company_ascii_folding",
                                            f -> f.definition(
                                                    d -> d.asciifolding(
                                                            af -> af
                                                                    .preserveOriginal(false)
                                                    )
                                            )
                                    )

                                    // Normalizer for keyword fields
                                    .normalizer(
                                            "company_keyword_normalizer",
                                            n -> n.custom(
                                                    custom -> custom
                                                            .filter(
                                                                    "lowercase",
                                                                    "company_ascii_folding"
                                                            )
                                            )
                                    )

                                    // Analyzer for full-text fields
                                    .analyzer(
                                            "company_text_analyzer",
                                            an -> an.custom(
                                                    custom -> custom
                                                            .tokenizer("standard")
                                                            .filter(
                                                                    "lowercase",
                                                                    "company_ascii_folding"
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
                                    .text(t -> t.analyzer("company_text_analyzer")
                                            .fields("keyword", f -> f.keyword(k -> k
                                                            .normalizer("company_keyword_normalizer")
                                                    )
                                            )
                                    )
                            )

                            .properties("description", p -> p
                                    .text(t -> t
                                            .analyzer("company_text_analyzer")
                                    )
                            )

                            .properties("address", p -> p
                                    .text(t -> t
                                            .analyzer("company_text_analyzer")
                                            .fields("keyword", f -> f
                                                    .keyword(k -> k
                                                            .normalizer(
                                                                    "company_keyword_normalizer"
                                                            )
                                                    )
                                            )
                                    )
                            )



                            .properties("logo", p -> p
                                    .keyword(k -> k)
                            )

                            .properties("website", p -> p
                                    .keyword(k -> k)
                            )

                            .properties("email", p -> p
                                    .keyword(k -> k)
                            )

                            .properties("phone", p -> p
                                    .keyword(k -> k)
                            )


                            .properties("employeeSize", p -> p
                                    .integer(i -> i)
                            )

                            .properties("foundedYear", p -> p
                                    .integer(i -> i)
                            )

                            .properties("status", p -> p
                                    .keyword(k -> k)
                            )

                            .properties("deleted", p -> p
                                    .boolean_(b -> b)
                            )

                            .properties("taxCode", p -> p
                                    .keyword(k -> k)
                            )


                            .properties("industryId", p -> p
                                    .long_(l -> l)
                            )

                            .properties("industryName", p -> p
                                    .text(t -> t
                                            .analyzer("company_text_analyzer")
                                            .fields("keyword", f -> f
                                                    .keyword(k -> k
                                                            .normalizer(
                                                                    "company_keyword_normalizer"
                                                            )
                                                    )
                                            )
                                    )
                            )


                            .properties("createdAt", p -> p
                                    .date(d -> d)
                            )
                    )
            );

            log.info("Created Elasticsearch index: {}", INDEX_NAME_COMPANY);

        } catch (Exception e) {
            log.error(
                    "Cannot create Elasticsearch index: {}",
                    INDEX_NAME_COMPANY,
                    e
            );
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
    public void bulkIndex(List<Company> companies) {
        if (companies == null || companies.isEmpty()) {
            return;
        }
        BulkRequest.Builder bulkRequest = new BulkRequest.Builder();
        companies.forEach(company -> {
            CompanyDocument document = companyMapper.toDocument(company);
            bulkRequest.operations(op -> op
                    .index(idx -> idx
                            .index(INDEX_NAME_COMPANY)
                            .id(String.valueOf(company.getId()))
                            .document(document)
                    )
            );
        });
        try {
            BulkResponse response = elasticsearchClient.bulk(bulkRequest.build());

            if (response.errors()) {

                long failedCount = response.items().stream().filter(item -> item.error() != null).count();
                log.error("Bulk indexing completed with errors. Failed: {}/{}", failedCount, companies.size());
                response.items().forEach(item -> {
                    if (item.error() != null) {
                        log.error("-> id={}, reason={}", item.id(), item.error().reason());
                    }
                });
                throw new RuntimeException("Some Companies failed during bulk indexing");
            }

            log.info("Successfully indexed {} companies to Elasticsearch", companies.size());

        } catch (IOException e) {
            log.error("Bulk indexing network/io error: {}", e.getMessage());
            throw new ElasticsearchException("Failed to add document to Elasticsearch",e);
        }
    }
    public void indexCompany(CompanyDocument document) {
        try {
            elasticsearchClient.index(i -> i
                    .index(INDEX_NAME_COMPANY)
                    .id(String.valueOf(document.getId()))
                    .document(document));
        } catch (Exception e) {
            throw new ElasticsearchException("Unexpected error during company indexing: " + document.getId(), e);
        }
    }
    public void deleteIndexCompany(Long companyId) {
        try {
            Map<String, Object> update = new HashMap<>();
            update.put("deleted", true);
            update.put("status", CompanyStatus.INACTIVE.name());
            elasticsearchClient.update(u -> u
                            .index(INDEX_NAME_COMPANY)
                            .id(String.valueOf(companyId))
                            .doc(update),
                    CompanyDocument.class
            );
        } catch (IOException e) {
            throw new ElasticsearchException("Failed to soft delete company: " + companyId,e);
        }
    }
    public void restoreIndexCompany(Long companyID) {
        try {
            Map<String, Object> update = new HashMap<>();
            update.put("deleted", false);
            update.put("status", CompanyStatus.ACTIVE.name());

            elasticsearchClient.update(u -> u
                            .index(INDEX_NAME_COMPANY)
                            .id(String.valueOf(companyID))
                            .doc(update),
                    CompanyDocument.class
            );

        } catch (IOException e) {
            throw new ElasticsearchException("Failed to restore company index: " + companyID, e);
        }
    }


}

